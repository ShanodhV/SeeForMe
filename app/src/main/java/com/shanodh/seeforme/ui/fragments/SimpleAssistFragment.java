package com.shanodh.seeforme.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.ml.SimpleObjectDetectionManager;
import com.shanodh.seeforme.ml.DemoDetectionManager;
import com.shanodh.seeforme.ml.Detection;
import com.shanodh.seeforme.ui.DetectionOverlayView;
import com.shanodh.seeforme.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple Assist Fragment using the new SimpleObjectDetectionManager
 * Focused on better detection and demonstration capabilities
 */
public class SimpleAssistFragment extends Fragment implements 
        SimpleObjectDetectionManager.DetectionCallback,
        DemoDetectionManager.DemoCallback {
    private static final String TAG = "SimpleAssistFragment";
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    
    // UI Components
    private PreviewView viewFinder;
    private DetectionOverlayView detectionOverlay;
    private TextView detectionResults;
    private TextView statusText;
    private MaterialButton toggleAssistButton;
    private MaterialButton demoModeButton;
    
    // Camera and ML
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private SimpleObjectDetectionManager simpleDetectionManager;
    private DemoDetectionManager demoDetectionManager;
    private ExecutorService cameraExecutor;
    
    // State
    private boolean isAssisting = false;
    private boolean isModelLoaded = false;
    private boolean isDemoMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        viewFinder = view.findViewById(R.id.viewFinder);
        detectionOverlay = view.findViewById(R.id.detectionOverlay);
        detectionResults = view.findViewById(R.id.detectionResults);
        statusText = view.findViewById(R.id.statusText);
        toggleAssistButton = view.findViewById(R.id.toggleAssistButton);
        demoModeButton = view.findViewById(R.id.demoModeButton);

        // Initialize executor and detection managers
        cameraExecutor = Executors.newSingleThreadExecutor();
        simpleDetectionManager = new SimpleObjectDetectionManager(requireContext());
        demoDetectionManager = new DemoDetectionManager(requireContext());
        
        // Setup click listeners
        toggleAssistButton.setOnClickListener(v -> toggleAssistance());
        demoModeButton.setOnClickListener(v -> toggleDemoMode());

        // Initialize ML model and camera
        initializeModel();
        
        // Check camera permission and start camera
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void initializeModel() {
        statusText.setText("🤖 Loading AI model...");
        
        // Initialize model in background thread
        cameraExecutor.execute(() -> {
            boolean simpleSuccess = simpleDetectionManager.initializeModel();
            boolean demoSuccess = demoDetectionManager.initializeModel();
            
            getActivity().runOnUiThread(() -> {
                isModelLoaded = simpleSuccess;
                if (simpleSuccess && demoSuccess) {
                    statusText.setText("✅ AI models ready! Choose detection mode below.");
                    Log.i(TAG, "✅ Both detection models loaded successfully");
                } else if (simpleSuccess) {
                    statusText.setText("✅ Main model ready! Demo backup unavailable.");
                    Log.i(TAG, "✅ Simple detection model loaded, demo failed");
                } else {
                    statusText.setText("❌ Failed to load AI models");
                    Log.e(TAG, "❌ Failed to load detection models");
                }
            });
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                statusText.setText("📷 Camera permission required for object detection");
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        statusText.setText("📷 Starting camera...");
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                
                if (isModelLoaded) {
                    statusText.setText("🎯 Ready! Tap 'Start Detection' to scan environment.");
                } else {
                    statusText.setText("📷 Camera ready. Loading AI model...");
                }
                
                Log.d(TAG, "📷 Camera initialized successfully");
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "❌ Camera initialization failed", e);
                statusText.setText("❌ Camera failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e(TAG, "❌ Camera provider is null");
            return;
        }

        Log.d(TAG, "🔧 Binding camera use cases");

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        // Image analysis use case for ML
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if (isAssisting && simpleDetectionManager.isReady()) {
                    // Convert ImageProxy to Bitmap
                    Bitmap bitmap = imageProxyToBitmap(image);
                    if (bitmap != null) {
                        // Run simple object detection
                        simpleDetectionManager.detectObjects(bitmap, SimpleAssistFragment.this);
                    }
                } else if (isDemoMode && demoDetectionManager.isReady()) {
                    // Run demo detection
                    Bitmap bitmap = imageProxyToBitmap(image);
                    if (bitmap != null) {
                        demoDetectionManager.runDemoDetection(bitmap, SimpleAssistFragment.this);
                    }
                }
                image.close();
            }
        });

        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            Log.d(TAG, "✅ Camera use cases bound successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Camera binding failed", e);
            statusText.setText("❌ Camera binding failed: " + e.getMessage());
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        return ImageUtils.fastImageProxyToBitmap(image);
    }

    private void toggleAssistance() {
        if (!isModelLoaded) {
            Toast.makeText(requireContext(), "⏳ Please wait for AI model to load", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isAssisting = !isAssisting;
        if (isAssisting) {
            // Clear previous state and start fresh
            simpleDetectionManager.clearDetectionState();
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            
            toggleAssistButton.setText("Stop Detection");
            statusText.setText("🔍 AI Detection Active - Scanning environment...");
            detectionResults.setText("🎯 Starting detection...");
            Log.d(TAG, "🚀 Simple object detection started - fresh state");
        } else {
            // Stop immediately and clear everything
            toggleAssistButton.setText("Start Detection");
            statusText.setText("⏸️ Detection stopped - Ready to scan");
            detectionResults.setText("Detection stopped");
            
            // Clear overlay and state immediately
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            simpleDetectionManager.clearDetectionState();
            
            Log.d(TAG, "⏸️ Simple object detection stopped - state cleared");
        }
    }

    private void toggleDemoMode() {
        if (!demoDetectionManager.isReady()) {
            Toast.makeText(requireContext(), "⏳ Demo model not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isDemoMode = !isDemoMode;
        
        if (isDemoMode) {
            // Stop regular detection if running
            if (isAssisting) {
                Log.i(TAG, "🎬 Auto-stopping main detection for demo mode");
                toggleAssistance(); // This will turn off main detection
            }
            
            // Start demo mode
            demoDetectionManager.startDemo();
            demoModeButton.setText("🛑 Stop Demo");
            demoModeButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
            statusText.setText("🎬 DEMO MODE ACTIVE - Visual detection for presentation");
            detectionResults.setText("🎬 Demo starting...");
            
            Log.i(TAG, "🎬 Demo mode started");
            
        } else {
            // Stop demo mode
            demoDetectionManager.stopDemo();
            demoModeButton.setText("🎬 Demo Mode");
            demoModeButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_orange_light));
            statusText.setText("🎬 Demo mode stopped - Ready for normal detection");
            detectionResults.setText("Demo stopped");
            
            // Clear overlay
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            
            Log.i(TAG, "🎬 Demo mode stopped");
        }
    }

    // SimpleObjectDetectionManager.DetectionCallback implementation
    @Override
    public void onObjectsDetected(List<Detection> detections) {
        if (!isAdded() || getActivity() == null || !isAssisting) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || !isAssisting) {
                return;
            }
            
            // Update overlay with bounding boxes
            if (detectionOverlay != null) {
                detectionOverlay.updateDetections(detections);
            }
            
            // Update detection results text
            StringBuilder result = new StringBuilder();
            result.append("🎯 Found ").append(detections.size()).append(" objects: ");
            
            for (int i = 0; i < Math.min(3, detections.size()); i++) {
                Detection detection = detections.get(i);
                result.append(detection.getLabel())
                      .append(" (").append(String.format("%.0f%%", detection.getConfidence() * 100)).append(")");
                if (i < Math.min(2, detections.size() - 1)) {
                    result.append(", ");
                }
            }
            
            if (detections.size() > 3) {
                result.append(" and ").append(detections.size() - 3).append(" more");
            }
            
            detectionResults.setText(result.toString());
            statusText.setText("🎯 Active Detection - " + detections.size() + " objects found");
        });
    }

    @Override
    public void onNoObjectsDetected() {
        if (!isAdded() || getActivity() == null || !isAssisting) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || !isAssisting) {
                return;
            }
            
            // Clear overlay
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            
            detectionResults.setText("🔍 No objects detected - Clear path");
            statusText.setText("🔍 Scanning environment...");
        });
    }
    
    // DemoDetectionManager.DemoCallback implementation
    @Override
    public void onDemoDetection(List<Detection> detections, String summaryText) {
        if (!isAdded() || getActivity() == null || !isDemoMode) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || !isDemoMode) {
                return;
            }
            
            // Update overlay with demo detections (colorful bounding boxes)
            if (detectionOverlay != null) {
                detectionOverlay.updateDetections(detections);
            }
            
            // Show comprehensive demo results
            detectionResults.setText("🎬 " + summaryText);
            statusText.setText("🎬 DEMO: Found " + detections.size() + " objects - Speaking results...");
            
            Log.i(TAG, "🎬 Demo detection: " + summaryText);
        });
    }
    
    @Override
    public void onDemoError(String error) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            
            detectionResults.setText("🎬 Demo Error: " + error);
            statusText.setText("🎬 Demo encountered an error");
            Log.e(TAG, "🎬 Demo error: " + error);
        });
    }
    
    public void updateStatus(String status) {
        if (isAdded() && statusText != null) {
            statusText.setText(status);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        Log.d(TAG, "🧹 Cleaning up SimpleAssistFragment");
        
        if (simpleDetectionManager != null) {
            simpleDetectionManager.shutdown();
        }
        
        if (demoDetectionManager != null) {
            demoDetectionManager.shutdown();
        }
        
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }
}

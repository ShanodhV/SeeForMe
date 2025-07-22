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
import com.shanodh.seeforme.ml.ObjectDetectionManager;
import com.shanodh.seeforme.ml.Detection;
import com.shanodh.seeforme.ui.DetectionOverlayView;
import com.shanodh.seeforme.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssistFragment extends Fragment implements ObjectDetectionManager.NavigationCallback {
    private static final String TAG = "AssistFragment";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    // UI Components
    private PreviewView viewFinder;
    private DetectionOverlayView detectionOverlay;
    private TextView detectionResults;
    private TextView statusText;
    private MaterialButton toggleAssistButton;
    
    // Camera and ML
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private ObjectDetectionManager detectionManager;
    private ExecutorService cameraExecutor;
    
    // State
    private boolean isAssisting = false;
    private boolean isModelLoaded = false;

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

        // Initialize executor and detection manager
        cameraExecutor = Executors.newSingleThreadExecutor();
        detectionManager = new ObjectDetectionManager(requireContext());
        
        // Setup click listeners
        toggleAssistButton.setOnClickListener(v -> toggleAssistance());

        // Initialize ML model and camera independently
        initializeModel();
        
        // Check camera permission and start camera immediately
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void initializeModel() {
        statusText.setText("Loading AI model...");
        cameraExecutor.execute(() -> {
            boolean success = detectionManager.initializeModel();
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    isModelLoaded = true;
                    statusText.setText("AI model loaded. Camera ready. Tap 'Start Assistance' to begin.");
                    Log.d(TAG, "Model loaded successfully");
                } else {
                    statusText.setText("Model loading failed - using mock detections for testing");
                    isModelLoaded = true; // Allow camera to work with mock detections
                    Log.w(TAG, "Failed to load model, using mock detections");
                }
            });
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                statusText.setText("Camera permission is required for object detection. Please grant permission in Settings.");
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        statusText.setText("Starting camera...");
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                // Bind the camera to the lifecycle
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                
                // Update status based on model loading state
                if (isModelLoaded) {
                    statusText.setText("Camera ready. Tap 'Start Assistance' to begin.");
                } else {
                    statusText.setText("Camera ready. Loading AI model...");
                }
                
                Log.d(TAG, "Camera initialized successfully");
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                statusText.setText("Camera initialization failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider is null");
            return;
        }

        Log.d(TAG, "Binding camera use cases");

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
                if (isAssisting && isModelLoaded) {
                    // Convert ImageProxy to Bitmap and run detection
                    Bitmap bitmap = imageProxyToBitmap(image);
                    if (bitmap != null) {
                        detectionManager.analyzeEnvironment(bitmap, AssistFragment.this);
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
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);

            Log.d(TAG, "Camera use cases bound successfully");

        } catch (Exception e) {
            Log.e(TAG, "Camera binding failed", e);
            statusText.setText("Camera binding failed: " + e.getMessage());
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        // Use optimized conversion from ImageUtils
        return ImageUtils.fastImageProxyToBitmap(image);
    }

    private void toggleAssistance() {
        if (!isModelLoaded) {
            Toast.makeText(requireContext(), "Please wait for AI model to load", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isAssisting = !isAssisting;
        if (isAssisting) {
            toggleAssistButton.setText("Stop Assistance");
            statusText.setText("🔍 AI Vision Active - Scanning environment...");
            Log.d(TAG, "Object detection started");
        } else {
            toggleAssistButton.setText("Start Assistance");
            statusText.setText("✋ Ready to assist - Tap to start");
            detectionResults.setText("Detection paused");
            
            // Clear overlay when stopping assistance
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            
            Log.d(TAG, "Object detection stopped");
        }
    }

    // ObjectDetectionManager.NavigationCallback implementation
    @Override
    public void onNavigationUpdate(List<Detection> safetyAlerts) {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment not attached, skipping UI update");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            // Double check in UI thread
            if (!isAdded() || getActivity() == null || !isAssisting) {
                return;
            }
            
            // Update overlay with bounding boxes
            if (detectionOverlay != null) {
                detectionOverlay.updateDetections(safetyAlerts);
            }
            
            if (safetyAlerts.isEmpty()) {
                detectionResults.setText("✅ Clear path ahead");
            } else {
                StringBuilder result = new StringBuilder();
                result.append("🎯 Detected: ");
                
                for (int i = 0; i < Math.min(3, safetyAlerts.size()); i++) {
                    Detection detection = safetyAlerts.get(i);
                    if (i > 0) result.append(", ");
                    result.append(detection.getLabel());
                    result.append(String.format(" (%.0f%%)", detection.getConfidence() * 100));
                }
                
                if (safetyAlerts.size() > 3) {
                    result.append(String.format(" + %d more", safetyAlerts.size() - 3));
                }
                
                detectionResults.setText(result.toString());
            }
            
            statusText.setText("🔍 Scanning... " + safetyAlerts.size() + " objects found");
        });
    }

    @Override
    public void onHazardDetected(Detection hazard) {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment not attached, skipping hazard UI update");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || !isAssisting) {
                return;
            }
            
            String hazardText = "⚠️ DANGER: " + hazard.getLabel() + 
                               String.format(" (%.0f%%)", hazard.getConfidence() * 100);
            detectionResults.setText(hazardText);
            statusText.setText("⚠️ Immediate hazard detected!");
        });
    }

    @Override
    public void onPathClear() {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment not attached, skipping clear path UI update");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || !isAssisting) {
                return;
            }
            
            // Clear overlay when no objects detected
            if (detectionOverlay != null) {
                detectionOverlay.clearDetections();
            }
            
            detectionResults.setText("✅ Clear path ahead");
            statusText.setText("🔍 Scanning... path is clear");
        });
    }
    
    public void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isAssisting) {
            toggleAssistance();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (detectionManager != null) {
            detectionManager.shutdown();
        }
        
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
} 
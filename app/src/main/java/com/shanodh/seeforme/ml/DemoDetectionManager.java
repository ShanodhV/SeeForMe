package com.shanodh.seeforme.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Demo Detection Manager - Simple YOLO11 for demonstration purposes
 * This is a backup for presentations that shows visual markings and lists detected objects
 */
public class DemoDetectionManager {
    private static final String TAG = "DemoDetection";
    
    // Model Configuration - Natural YOLO11 settings
    private static final String MODEL_PATH = "yolo11n_float32.tflite";
    private static final float DETECTION_CONFIDENCE = 0.4f; // Lower threshold for demo
    private static final float NMS_THRESHOLD = 0.5f;
    
    // Demo timing
    private static final long DEMO_INTERVAL = 1000; // 1 second - simple timing
    
    // Context and ML Components
    private Context context;
    private Interpreter model;
    private TextToSpeech tts;
    private ExecutorService inferenceThread;
    private Handler mainThread;
    
    // State
    private long lastDetectionTime;
    private boolean isInitialized = false;
    private boolean isDemoActive = false;
    
    // COCO Classes for demo
    private static final String[] COCO_CLASSES = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    };
    
    public interface DemoCallback {
        void onDemoDetection(List<Detection> detections, String summaryText);
        void onDemoError(String error);
    }

    public DemoDetectionManager(Context context) {
        this.context = context;
        this.inferenceThread = Executors.newSingleThreadExecutor();
        this.mainThread = new Handler(Looper.getMainLooper());
        this.lastDetectionTime = 0;
        
        initializeTTS();
    }

    /**
     * Initialize YOLO11 model for demo
     */
    public boolean initializeModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFromAssets();
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Conservative for demo stability
            options.setUseXNNPACK(true);
            options.setAllowFp16PrecisionForFp32(true);
            
            model = new Interpreter(modelBuffer, options);
            model.allocateTensors();
            
            Log.i(TAG, "🎬 Demo YOLO11 model initialized successfully");
            isInitialized = true;
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Demo model initialization failed", e);
            isInitialized = false;
            return false;
        }
    }
    
    private MappedByteBuffer loadModelFromAssets() throws IOException {
        return context.getAssets().openFd(MODEL_PATH).createInputStream().getChannel()
                .map(FileChannel.MapMode.READ_ONLY, 
                     context.getAssets().openFd(MODEL_PATH).getStartOffset(),
                     context.getAssets().openFd(MODEL_PATH).getDeclaredLength());
    }

    /**
     * Run demo detection - shows all detected objects with visual markings
     */
    public void runDemoDetection(Bitmap cameraFrame, DemoCallback callback) {
        if (!isInitialized || model == null || !isDemoActive) {
            Log.e(TAG, "❌ Demo not ready for detection");
            return;
        }
        
        // Rate limiting for demo clarity
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastDetectionTime) < DEMO_INTERVAL) {
            return;
        }
        
        inferenceThread.execute(() -> {
            try {
                Log.i(TAG, "🎬 Running demo detection...");
                
                // Run YOLO11 inference
                List<Detection> detections = runYOLO11Inference(cameraFrame);
                
                // Apply NMS but keep more detections for demo
                List<Detection> filteredDetections = applyDemoNMS(detections);
                
                // Create summary text for demo
                String summaryText = createDemoSummary(filteredDetections);
                
                // Announce the summary
                speakDemoResults(summaryText);
                
                Log.i(TAG, "🎬 Demo detection complete: " + summaryText);
                
                mainThread.post(() -> callback.onDemoDetection(filteredDetections, summaryText));
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Demo detection failed", e);
                mainThread.post(() -> callback.onDemoError("Demo detection failed: " + e.getMessage()));
            }
        });
        
        lastDetectionTime = currentTime;
    }
    
    /**
     * Run YOLO11 inference - same as production but more liberal
     */
    private List<Detection> runYOLO11Inference(Bitmap frame) {
        List<Detection> detections = new ArrayList<>();
        
        try {
            // Resize to YOLO11 input size
            Bitmap resizedFrame = Bitmap.createScaledBitmap(frame, 640, 640, true);
            float[][][][] input = preprocessImage(resizedFrame);
            
            // Run inference
            float[][][] output = new float[1][84][8400];
            model.run(input, output);
            
            // Parse output
            detections = parseYOLO11Output(transposeOutput(output[0]), frame.getWidth(), frame.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Demo inference failed", e);
        }
        
        return detections;
    }
    
    /**
     * Preprocess image - HWC format for TensorFlow Lite
     */
    private float[][][][] preprocessImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float[][][][] input = new float[1][height][width][3];
        
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;         // Blue
            }
        }
        
        return input;
    }
    
    /**
     * Transpose YOLO11 output from [84][8400] to [8400][84]
     */
    private float[][] transposeOutput(float[][] input) {
        int rows = input.length;
        int cols = input[0].length;
        
        float[][] transposed = new float[cols][rows];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = input[i][j];
            }
        }
        
        return transposed;
    }
    
    /**
     * Parse YOLO11 output - liberal settings for demo
     */
    private List<Detection> parseYOLO11Output(float[][] output, int originalWidth, int originalHeight) {
        List<Detection> detections = new ArrayList<>();
        
        for (int i = 0; i < output.length; i++) {
            float[] detection = output[i];
            if (detection.length < 84) continue;
            
            // Extract bounding box
            float centerX = detection[0];
            float centerY = detection[1];
            float width = detection[2];
            float height = detection[3];
            
            // Basic validation
            if (centerX < 0.0f || centerX > 1.0f || centerY < 0.0f || centerY > 1.0f ||
                width < 0.01f || width > 1.0f || height < 0.01f || height > 1.0f) {
                continue;
            }
            
            // Find best class
            int bestClassIdx = 0;
            float bestConfidence = 0;
            
            for (int j = 0; j < 80; j++) {
                float confidence = detection[4 + j];
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestClassIdx = j;
                }
            }
            
            // Liberal confidence threshold for demo
            if (bestConfidence >= DETECTION_CONFIDENCE) {
                // Convert to pixel coordinates
                int pixelX = (int)(centerX * originalWidth);
                int pixelY = (int)(centerY * originalHeight);
                int pixelW = (int)(width * originalWidth);
                int pixelH = (int)(height * originalHeight);
                
                int left = Math.max(0, pixelX - pixelW/2);
                int top = Math.max(0, pixelY - pixelH/2);
                int right = Math.min(originalWidth, pixelX + pixelW/2);
                int bottom = Math.min(originalHeight, pixelY + pixelH/2);
                
                // Size validation
                int detectionWidth = right - left;
                int detectionHeight = bottom - top;
                
                if (detectionWidth > 20 && detectionHeight > 20) {
                    String className = COCO_CLASSES[bestClassIdx];
                    detections.add(new Detection(className, bestConfidence, left, top, right, bottom, 
                                               originalWidth, originalHeight));
                }
            }
        }
        
        return detections;
    }
    
    /**
     * Demo NMS - keep more objects for visualization
     */
    private List<Detection> applyDemoNMS(List<Detection> detections) {
        if (detections.isEmpty()) return detections;
        
        // Sort by confidence
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        List<Detection> result = new ArrayList<>();
        
        for (Detection detection : detections) {
            boolean shouldKeep = true;
            
            // Check against already selected detections
            for (Detection selected : result) {
                if (calculateIoU(detection, selected) > NMS_THRESHOLD) {
                    shouldKeep = false;
                    break;
                }
            }
            
            if (shouldKeep) {
                result.add(detection);
                // Limit to top 10 for demo clarity
                if (result.size() >= 10) break;
            }
        }
        
        return result;
    }
    
    /**
     * Calculate IoU for NMS
     */
    private float calculateIoU(Detection a, Detection b) {
        float x1 = Math.max(a.getLeft(), b.getLeft());
        float y1 = Math.max(a.getTop(), b.getTop());
        float x2 = Math.min(a.getRight(), b.getRight());
        float y2 = Math.min(a.getBottom(), b.getBottom());
        
        if (x1 >= x2 || y1 >= y2) return 0f;
        
        float intersection = (x2 - x1) * (y2 - y1);
        float areaA = (a.getRight() - a.getLeft()) * (a.getBottom() - a.getTop());
        float areaB = (b.getRight() - b.getLeft()) * (b.getBottom() - b.getTop());
        
        return intersection / (areaA + areaB - intersection);
    }
    
    /**
     * Create demo summary text
     */
    private String createDemoSummary(List<Detection> detections) {
        if (detections.isEmpty()) {
            return "No objects detected";
        }
        
        // Simple announcement - just list the objects
        StringBuilder summary = new StringBuilder();
        
        for (int i = 0; i < Math.min(3, detections.size()); i++) {
            Detection detection = detections.get(i);
            String object = detection.getLabel();
            summary.append(object);
            if (i < Math.min(2, detections.size() - 1)) {
                summary.append(", ");
            }
        }
        
        if (detections.size() > 3) {
            summary.append(" and ").append(detections.size() - 3).append(" more objects");
        }
        
        return summary.toString();
    }
    
    /**
     * Initialize TTS for demo
     */
    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.9f); // Slightly slower for demo clarity
                tts.setPitch(1.0f);
                Log.i(TAG, "🎬 Demo TTS initialized");
            } else {
                Log.e(TAG, "❌ Demo TTS initialization failed");
            }
        });
    }
    
    /**
     * Speak demo results
     */
    private void speakDemoResults(String message) {
        if (tts != null && isDemoActive) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "demo_detection");
        }
    }
    
    /**
     * Start demo mode
     */
    public void startDemo() {
        isDemoActive = true;
        Log.i(TAG, "🎬 Demo mode started");
    }
    
    /**
     * Stop demo mode
     */
    public void stopDemo() {
        isDemoActive = false;
        if (tts != null) {
            tts.stop();
        }
        Log.i(TAG, "🎬 Demo mode stopped");
    }
    
    /**
     * Check if demo is ready
     */
    public boolean isReady() {
        return isInitialized && model != null;
    }
    
    /**
     * Check if demo is active
     */
    public boolean isDemoActive() {
        return isDemoActive;
    }
    
    /**
     * Shutdown demo manager
     */
    public void shutdown() {
        Log.i(TAG, "🎬 Shutting down demo manager");
        
        isDemoActive = false;
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        
        if (inferenceThread != null && !inferenceThread.isShutdown()) {
            inferenceThread.shutdown();
        }
        
        if (model != null) {
            model.close();
        }
        
        isInitialized = false;
    }
}

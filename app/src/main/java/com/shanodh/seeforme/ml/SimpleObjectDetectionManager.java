package com.shanodh.seeforme.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;




public class SimpleObjectDetectionManager {
    private static final String TAG = "SimpleDetect";
    
    // Model Configuration - SIMPLE AND FAST
    private static final String MODEL_PATH = "yolo11n_float32.tflite";
    private static final float DETECTION_CONFIDENCE = 0.51f; // High threshold - only announce confident detections
    private static final float NMS_THRESHOLD = 0.5f; // Standard NMS
    private static final float MIN_DETECTION_SIZE = 0.0015f; // Very permissive
    
    // Timing Configuration  
    private static final long DETECTION_INTERVAL = 1000; // 1 second - simple timing
    
    // Context and ML Components
    private Context context;
    private Interpreter model;
    private TextToSpeech tts;
    private Vibrator vibrator;
    private ExecutorService inferenceThread;
    private Handler mainThread;
    
    // State Management  
    private long lastDetectionTime;
    private boolean isInitialized = false;
    
    // COCO Class Names (80 classes)
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
    
    public interface DetectionCallback {
        void onObjectsDetected(List<Detection> detections);
        void onNoObjectsDetected();
    }

    public SimpleObjectDetectionManager(Context context) {
        this.context = context;
        this.inferenceThread = Executors.newSingleThreadExecutor();
        this.mainThread = new Handler(Looper.getMainLooper());
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.lastDetectionTime = 0;
        
        initializeTTS();
    }

    /**
     * Initialize YOLO11 model with optimized settings
     */
    public boolean initializeModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFromAssets();
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // More threads for better performance
            options.setUseXNNPACK(true); // Hardware acceleration
            options.setAllowFp16PrecisionForFp32(true); // Speed optimization
            
            model = new Interpreter(modelBuffer, options);
            
            int[] inputShape = model.getInputTensor(0).shape();
            int[] outputShape = model.getOutputTensor(0).shape();
            
            Log.i(TAG, "🚀 YOLO11 Model Ready: Input " + Arrays.toString(inputShape) + 
                      ", Output " + Arrays.toString(outputShape));
            
            // Allocate tensors explicitly
            model.allocateTensors();
            Log.i(TAG, "✅ Tensors allocated successfully");
            
            isInitialized = true;
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize YOLO11 model", e);
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
     * Detect objects in the environment with full YOLO11 capacity
     */
    public void detectObjects(Bitmap cameraFrame, DetectionCallback callback) {
        if (!isInitialized || model == null) {
            Log.e(TAG, "❌ Model not initialized! Detection skipped.");
            return;
        }
        
        if (isRateLimited()) {
            return; // Maintain smooth frame rate
        }
        
        inferenceThread.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Run YOLO11 inference
                List<Detection> detections = runYOLO11Inference(cameraFrame);
                
                // Apply Non-Maximum Suppression
                List<Detection> filteredDetections = applyNMS(detections);
                
                // Generate announcements for detected objects
                processAnnouncements(filteredDetections);
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Simple logging
                if (!filteredDetections.isEmpty()) {
                    Detection d = filteredDetections.get(0);
                    Log.d(TAG, "🎯 " + d.getLabel() + " (" + String.format("%.2f", d.getConfidence()) + ")");
                }
                
                mainThread.post(() -> {
                    if (!filteredDetections.isEmpty()) {
                        callback.onObjectsDetected(filteredDetections);
                    } else {
                        callback.onNoObjectsDetected();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Detection failed", e);
            }
        });
        
        // Update last detection time
        lastDetectionTime = System.currentTimeMillis();
    }
    
    /**
     * Check if detection should be rate limited
     */
    private boolean isRateLimited() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastDetectionTime) < DETECTION_INTERVAL;
    }
    
    /**
     * Run YOLO11 inference with proper error handling
     */
    private List<Detection> runYOLO11Inference(Bitmap frame) {
        List<Detection> detections = new ArrayList<>();
        
        try {
            // Resize to YOLO11 input size (640x640)
            Bitmap resizedFrame = Bitmap.createScaledBitmap(frame, 640, 640, true);
            float[][][][] input = preprocessImage(resizedFrame);
            
            // Run YOLO11 inference - outputs [1, 84, 8400]
            float[][][] output = new float[1][84][8400];
            
            try {
                model.run(input, output);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "❌ Tensor error: " + e.getMessage());
                return detections;
            }
            
            // Parse YOLO11 output
            detections = parseYOLO11Output(transposeOutput(output[0]), frame.getWidth(), frame.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ YOLO11 inference failed", e);
        }
        
        return detections;
    }
    
    /**
     * Preprocess image for YOLO11 input - Using correct TensorFlow Lite format
     * Most TFLite models expect [1][640][640][3] format (HWC)
     */
    private float[][][][] preprocessImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // TensorFlow Lite expects HWC format: [batch][height][width][channels]
        float[][][][] input = new float[1][height][width][3];
        
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Convert ARGB to normalized HWC format
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                
                // Extract RGB and normalize to [0, 1] - HWC format
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red channel
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green channel  
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;         // Blue channel
            }
        }
        
        return input;
    }
    
    /**
     * Transpose YOLO11 output from [84][8400] to [8400][84] format
     */
    private float[][] transposeOutput(float[][] input) {
        int rows = input.length;    // 84
        int cols = input[0].length; // 8400
        
        float[][] transposed = new float[cols][rows]; // [8400][84]
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = input[i][j];
            }
        }
        
        return transposed;
    }
    
    /**
     * Parse YOLO11 output with simplified logic - based on article best practices
     */
    private List<Detection> parseYOLO11Output(float[][] output, int originalWidth, int originalHeight) {
        List<Detection> detections = new ArrayList<>();
        
        int totalCandidates = 0;
        int validDetections = 0;
        
        // Process all 8400 potential detections
        for (int i = 0; i < output.length; i++) {
            float[] detection = output[i];
            if (detection.length < 84) continue;
            
            totalCandidates++;
            
            // Extract bounding box (normalized coordinates)
            float centerX = detection[0];
            float centerY = detection[1];
            float width = detection[2];
            float height = detection[3];
            
            // Basic coordinate validation
            if (centerX < 0.0f || centerX > 1.0f || centerY < 0.0f || centerY > 1.0f ||
                width < 0.01f || width > 1.0f || height < 0.01f || height > 1.0f) {
                continue;
            }
            
            // Find best class prediction - SIMPLE
            int bestClassIdx = 0;
            float bestConfidence = 0;
            
            for (int j = 0; j < 80; j++) {
                float confidence = detection[4 + j];
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestClassIdx = j;
                }
            }
            
            // Simple confidence threshold - no complex adaptive logic
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
                
                // Basic size validation - very permissive
                int detectionWidth = right - left;
                int detectionHeight = bottom - top;
                
                if (detectionWidth > 10 && detectionHeight > 10) { // Minimum 10 pixels
                    String className = COCO_CLASSES[bestClassIdx];
                    detections.add(new Detection(className, bestConfidence, left, top, right, bottom, 
                                               originalWidth, originalHeight));
                    validDetections++;
                }
            }
        }
        
        return detections;
    }

       private void processAnnouncements(List<Detection> detections) {
        if (detections.isEmpty()) {
            // Nothing detected - stay silent
            Log.d(TAG, "🔇 No objects detected");
            return;
        }
        
        // Get the best detection and announce it
        Detection bestDetection = detections.get(0);
        String objectName = bestDetection.getLabel();
        String direction = getDirection(bestDetection);
        String message = objectName + " to the " + direction;
        
        speakMessage(message);
        Log.i(TAG, "🔊 " + message);
    } 

    /**
     * SIMPLE NMS - Just take the best detection and announce it
     */
    private List<Detection> applyNMS(List<Detection> detections) {
        if (detections.isEmpty()) return detections;
        
        // Sort by confidence and just take the top one
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        List<Detection> result = new ArrayList<>();
        if (!detections.isEmpty()) {
            result.add(detections.get(0)); // Just take the most confident detection
        }
        
        return result;
    }
    
    /**
     * Calculate Intersection over Union (IoU) for two detections
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
     * Get direction description based on object position
     */
    private String getDirection(Detection detection) {
        float centerX = detection.getCenterX();
        float imageWidth = detection.getImageWidth();
        float position = centerX / imageWidth;
        
        if (position < 0.3f) return "left";
        else if (position < 0.7f) return "center";
        else return "right";
    }

    /**
     * Initialize Text-to-Speech
     */
    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(1.0f); // Normal speed for clarity
                tts.setPitch(1.0f); // Normal pitch
                Log.i(TAG, "🔊 TTS initialized successfully");
            } else {
                Log.e(TAG, "❌ TTS initialization failed");
            }
        });
    }
    
    /**
     * Speak announcement message
     */
    private void speakMessage(String message) {
        if (tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "object_detection");
        }
    }

    /**
     * Trigger simple haptic feedback
     */
    private void triggerHapticFeedback(int objectCount) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(100); // Simple vibration for any detection
        }
    }

    /**
     * Check if manager is ready for detection
     */
    public boolean isReady() {
        return isInitialized && model != null;
    }

    /**
     * Clear detection state - use when pausing/starting fresh
     */
    public void clearDetectionState() {
        lastDetectionTime = 0;
        if (tts != null) {
            tts.stop(); // Stop any ongoing speech
        }
        Log.d(TAG, "🧹 Detection state cleared");
    }

    /**
     * Get current detection statistics
     */
    public String getDetectionStats() {
        return "Optimized YOLO11 Manager - Confidence: " + DETECTION_CONFIDENCE + 
               ", Simple immediate announcements enabled";
    }

    /**
     * Shutdown and cleanup resources
     */
    public void shutdown() {
        Log.i(TAG, "Shutting down Simple Object Detection Manager");
        
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

package com.shanodh.seeforme.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Production-ready Object Detection Manager for Blind Navigation
 * Optimized for real-time YOLO11 inference with haptic feedback
 * 
 * NOTE: COCO dataset limitations for architectural elements:
 * - Doors, windows, stairs are NOT in the 80 COCO classes
 * - For complete navigation, consider:
 *   1. Custom model training for architectural elements
 *   2. Depth sensing for stairs/elevation changes
 *   3. Edge detection for doorways and openings
 *   4. Integration with building layout APIs
 */
public class ObjectDetectionManager {
    private static final String TAG = "BlindNavigation";
    
    // YOLO11 Configuration - Optimized for mobile deployment
    private static final String MODEL_PATH = "yolo11n_float32.tflite";
    private static final float CONFIDENCE_THRESHOLD = 0.45f; // Higher for production reliability
    private static final float HOUSEHOLD_CONFIDENCE = 0.35f; // Furniture and common objects
    private static final float HAZARD_CONFIDENCE = 0.60f; // Critical safety items
    
    // Performance tuning for real-time navigation
    private static final long DETECTION_INTERVAL = 200; // Balanced performance/accuracy
    private static final long NAVIGATION_UPDATE = 800; // Navigation announcements
    private static final long HAZARD_ALERT_INTERVAL = 100; // Immediate danger alerts
    
    // Navigation assistance parameters
    private static final float IMMEDIATE_DANGER_ZONE = 0.7f; // Objects taking 70%+ of view
    private static final float CLOSE_PROXIMITY_ZONE = 0.4f; // Objects requiring attention
    private static final float DISTANT_AWARENESS_ZONE = 0.15f; // Background awareness
    
    private Context context;
    private Interpreter model;
    private TextToSpeech tts;
    private Vibrator hapticFeedback;
    private ExecutorService inferenceThread;
    private Handler mainThread;
    
    // Navigation state tracking
    private Map<String, Long> lastAlertTime;
    private Map<String, NavigationObject> trackedObjects;
    private long lastGlobalAlert;
    
    // Safety categories for blind navigation
    private Set<String> immediateDangers;
    private Set<String> navigationObstacles;
    private Set<String> householdItems;
    
    public interface NavigationCallback {
        void onNavigationUpdate(List<Detection> safetyAlerts);
        void onHazardDetected(Detection hazard);
        void onPathClear();
    }

    public ObjectDetectionManager(Context context) {
        this.context = context;
        this.lastAlertTime = new HashMap<>();
        this.trackedObjects = new HashMap<>();
        this.inferenceThread = Executors.newSingleThreadExecutor();
        this.mainThread = new Handler(Looper.getMainLooper());
        this.hapticFeedback = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.lastGlobalAlert = 0;
        
        initializeSafetyCategories();
        initializeTTS();
    }

    /**
     * Initialize YOLO11 model with production-optimized settings
     */
    public boolean initializeModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFromAssets();
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Optimized for mobile CPUs
            options.setUseXNNPACK(true); // Enable XNNPACK optimization
            
            model = new Interpreter(modelBuffer, options);
            
            int[] inputShape = model.getInputTensor(0).shape();
            int[] outputShape = model.getOutputTensor(0).shape();
            
            Log.i(TAG, "YOLO11 Model initialized: Input " + Arrays.toString(inputShape) + 
                      ", Output " + Arrays.toString(outputShape));
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize YOLO11 model", e);
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
     * Main navigation assistance method - processes camera frames for blind users
     * 
     * ARCHITECTURAL ELEMENTS DETECTION:
     * Since doors, windows, stairs are not in COCO dataset, consider these approaches:
     * 1. Depth sensing (ARCore/LiDAR) for stairs and elevation changes  
     * 2. Edge detection algorithms for door frames and openings
     * 3. Integration with indoor positioning systems
     * 4. Custom model training on architectural dataset
     */
    public void analyzeEnvironment(Bitmap cameraFrame, NavigationCallback callback) {
        if (model == null || isRateLimited()) {
            return;
        }
        
        inferenceThread.execute(() -> {
            try {
                List<Detection> detections = runYOLO11Inference(cameraFrame);
                List<Detection> safetyAlerts = prioritizeForNavigation(detections);
                
                processNavigationFeedback(safetyAlerts);
                
                mainThread.post(() -> {
                    if (hasCriticalHazards(safetyAlerts)) {
                        callback.onHazardDetected(getMostCritical(safetyAlerts));
                    } else if (!safetyAlerts.isEmpty()) {
                        callback.onNavigationUpdate(safetyAlerts);
                    } else {
                        callback.onPathClear();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Environment analysis failed", e);
            }
        });
    }
    
    private boolean isRateLimited() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastGlobalAlert) < DETECTION_INTERVAL;
    }

    /**
     * Optimized YOLO11 inference for mobile devices
     */
    private List<Detection> runYOLO11Inference(Bitmap frame) {
        List<Detection> detections = new ArrayList<>();
        
        try {
            // Resize to YOLO11 input size (640x640)
            Bitmap resizedFrame = Bitmap.createScaledBitmap(frame, 640, 640, true);
            float[][][][] input = preprocessImage(resizedFrame);
            
            // Run inference
            float[][][] output = new float[1][8400][84];
            model.run(input, output);
            
            // Parse YOLO11 output format
            detections = parseYOLO11Output(output[0], frame.getWidth(), frame.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "YOLO11 inference failed", e);
        }
        
        return detections;
    }
    
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
     * Parse YOLO11 output with production-grade filtering
     */
    private List<Detection> parseYOLO11Output(float[][] output, int originalWidth, int originalHeight) {
        List<Detection> detections = new ArrayList<>();
        
        String[] cocoClasses = {
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
        
        for (int i = 0; i < output.length && detections.size() < 5; i++) { // Limit for performance
            float[] detection = output[i];
            if (detection.length < 84) continue;
            
            // Extract bounding box (normalized coordinates)
            float centerX = detection[0];
            float centerY = detection[1];
            float width = detection[2];
            float height = detection[3];
            
            // Validate coordinates
            if (centerX < 0 || centerX > 1 || centerY < 0 || centerY > 1 ||
                width < 0.01f || width > 1 || height < 0.01f || height > 1) {
                continue;
            }
            
            // Find best class prediction
            int bestClass = 0;
            float bestScore = 0;
            for (int j = 0; j < 80; j++) {
                float score = detection[4 + j];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = j;
                }
            }
            
            // Apply production confidence thresholds
            String className = cocoClasses[bestClass];
            float requiredConfidence = getRequiredConfidence(className);
            
            if (bestScore >= requiredConfidence) {
                // Convert to pixel coordinates
                int pixelX = (int)(centerX * originalWidth);
                int pixelY = (int)(centerY * originalHeight);
                int pixelW = (int)(width * originalWidth);
                int pixelH = (int)(height * originalHeight);
                
                int left = Math.max(0, pixelX - pixelW/2);
                int top = Math.max(0, pixelY - pixelH/2);
                int right = Math.min(originalWidth, pixelX + pixelW/2);
                int bottom = Math.min(originalHeight, pixelY + pixelH/2);
                
                // Validate detection size
                if ((right - left) > 15 && (bottom - top) > 15) {
                    detections.add(new Detection(className, bestScore, left, top, right, bottom, 
                                               originalWidth, originalHeight));
                }
            }
        }
        
        return applyNonMaxSuppression(detections);
    }
    
    private float getRequiredConfidence(String className) {
        if (immediateDangers.contains(className)) {
            return HAZARD_CONFIDENCE;
        } else if (householdItems.contains(className)) {
            return HOUSEHOLD_CONFIDENCE;
        } else {
            return CONFIDENCE_THRESHOLD;
        }
    }

    /**
     * Non-Maximum Suppression optimized for navigation safety
     */
    private List<Detection> applyNonMaxSuppression(List<Detection> detections) {
        if (detections.isEmpty()) return detections;
        
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        List<Detection> filtered = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];
        
        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;
            
            Detection current = detections.get(i);
            filtered.add(current);
            
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                
                Detection other = detections.get(j);
                if (current.getLabel().equals(other.getLabel()) && 
                    calculateIoU(current, other) > 0.3f) {
                    suppressed[j] = true;
                }
            }
        }
        
        return filtered;
    }
    
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
     * Prioritize detections for blind navigation safety
     */
    private List<Detection> prioritizeForNavigation(List<Detection> detections) {
        List<Detection> navigationAlerts = new ArrayList<>();
        
        for (Detection detection : detections) {
            String object = detection.getLabel();
            float relativeSize = detection.getRelativeSize();
            
            // Immediate dangers - vehicles, moving objects, hazards
            if (immediateDangers.contains(object) && relativeSize > DISTANT_AWARENESS_ZONE) {
                navigationAlerts.add(detection);
            }
            // Navigation obstacles - furniture, barriers
            else if (navigationObstacles.contains(object) && relativeSize > CLOSE_PROXIMITY_ZONE) {
                navigationAlerts.add(detection);
            }
            // People - always important for navigation
            else if ("person".equals(object) && relativeSize > DISTANT_AWARENESS_ZONE) {
                navigationAlerts.add(detection);
            }
        }
        
        // Sort by safety priority: immediate dangers first, then by proximity
        navigationAlerts.sort((a, b) -> {
            int priorityA = getSafetyPriority(a.getLabel());
            int priorityB = getSafetyPriority(b.getLabel());
            
            if (priorityA != priorityB) {
                return Integer.compare(priorityA, priorityB); // Lower = higher priority
            }
            
            return Float.compare(b.getRelativeSize(), a.getRelativeSize()); // Closer objects first
        });
        
        return navigationAlerts.subList(0, Math.min(3, navigationAlerts.size()));
    }
    
    private int getSafetyPriority(String object) {
        if (immediateDangers.contains(object)) return 1; // Highest priority
        if ("person".equals(object)) return 2;
        if (navigationObstacles.contains(object)) return 3;
        return 4; // Lowest priority
    }

    /**
     * Generate navigation feedback with haptic and audio alerts
     */
    private void processNavigationFeedback(List<Detection> safetyAlerts) {
        long currentTime = System.currentTimeMillis();
        
        for (Detection detection : safetyAlerts) {
            String object = detection.getLabel();
            float proximity = detection.getRelativeSize();
            
            // Check if we should alert about this object
            if (shouldAlert(object, proximity, currentTime)) {
                generateNavigationAlert(detection);
                lastAlertTime.put(object, currentTime);
            }
        }
        
        lastGlobalAlert = currentTime;
    }
    
    private boolean shouldAlert(String object, float proximity, long currentTime) {
        Long lastAlert = lastAlertTime.get(object);
        
        if (lastAlert == null) return true; // First detection
        
        long interval = immediateDangers.contains(object) ? 
                       HAZARD_ALERT_INTERVAL : NAVIGATION_UPDATE;
        
        return (currentTime - lastAlert) > interval;
    }
    
    private void generateNavigationAlert(Detection detection) {
        String object = detection.getLabel();
        float proximity = detection.getRelativeSize();
        String direction = getDirection(detection);
        
        // Generate haptic feedback based on danger level
        if (proximity > IMMEDIATE_DANGER_ZONE) {
            triggerUrgentHaptic();
            speakAlert("DANGER! " + object + " directly " + direction);
        } else if (proximity > CLOSE_PROXIMITY_ZONE) {
            triggerWarningHaptic();
            speakAlert(object + " " + direction);
        } else {
            triggerInfoHaptic();
            speakAlert(object + " in the distance " + direction);
        }
    }
    
    private String getDirection(Detection detection) {
        float centerX = detection.getCenterX();
        float imageWidth = detection.getImageWidth();
        float position = centerX / imageWidth;
        
        if (position < 0.3f) return "to your left";
        else if (position > 0.7f) return "to your right";
        else return "ahead";
    }

    /**
     * Haptic feedback patterns for different alert levels
     */
    private void triggerUrgentHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Rapid pulses for immediate danger
            VibrationEffect pattern = VibrationEffect.createWaveform(
                new long[]{0, 100, 50, 100, 50, 100}, -1);
            hapticFeedback.vibrate(pattern);
        }
    }
    
    private void triggerWarningHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Double pulse for warnings
            VibrationEffect pattern = VibrationEffect.createWaveform(
                new long[]{0, 200, 100, 200}, -1);
            hapticFeedback.vibrate(pattern);
        }
    }
    
    private void triggerInfoHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Single pulse for information
            VibrationEffect effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE);
            hapticFeedback.vibrate(effect);
        }
    }

    /**
     * Optimized Text-to-Speech for navigation
     */
    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(1.2f); // Slightly faster for navigation
                tts.setPitch(1.0f);
                Log.i(TAG, "Navigation TTS initialized");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }
    
    private void speakAlert(String message) {
        if (tts != null) {
            tts.stop(); // Interrupt previous announcements for urgent alerts
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "navigation_alert");
        }
    }

    /**
     * Initialize safety categories for blind navigation - ALL 80 COCO classes categorized
     */
    private void initializeSafetyCategories() {
        // IMMEDIATE DANGERS - Vehicles, sharp objects, hot appliances (Priority 1)
        immediateDangers = new HashSet<>(Arrays.asList(
            // Vehicles - immediate collision risk
            "car", "truck", "bus", "motorcycle", "bicycle", "train", "airplane", "boat",
            // Sharp/dangerous objects
            "knife", "scissors",
            // Hot appliances
            "oven", "toaster", "microwave",
            // Large dangerous animals
            "horse", "cow", "elephant", "bear",
            // Traffic infrastructure that indicates vehicle zones
            "fire hydrant", "parking meter"
        ));
        
        // NAVIGATION OBSTACLES - Furniture, barriers, equipment (Priority 2)
        navigationObstacles = new HashSet<>(Arrays.asList(
            // Large furniture - major navigation obstacles
            "chair", "couch", "dining table", "bed", "bench", "toilet",
            // Large appliances and equipment
            "refrigerator", "tv", "sink",
            // Large portable items that block paths
            "suitcase", "backpack", "potted plant",
            // Sports equipment that can block paths
            "surfboard", "skateboard", "skis", "snowboard",
            // Traffic signs - important for navigation context
            "traffic light", "stop sign"
        ));
        
        // HOUSEHOLD ITEMS - Small objects, electronics, food (Priority 3)
        householdItems = new HashSet<>(Arrays.asList(
            // Small electronics
            "laptop", "mouse", "remote", "keyboard", "cell phone", "clock",
            // Kitchen items
            "bottle", "wine glass", "cup", "fork", "spoon", "bowl", "vase",
            // Food items
            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", 
            "hot dog", "pizza", "donut", "cake",
            // Books and small items
            "book", "umbrella", "handbag", "tie",
            // Personal care
            "hair drier", "toothbrush", "teddy bear"
        ));
        
        // ANIMALS AND PEOPLE - Living beings (Priority varies)
        Set<String> livingBeings = new HashSet<>(Arrays.asList(
            // People - high priority for navigation
            "person",
            // Small safe animals
            "cat", "dog", "bird", "sheep", "zebra", "giraffe",
            // Note: Large/dangerous animals are in immediateDangers
        ));
        
        // SPORTS AND RECREATION - Lower priority unless blocking path
        Set<String> sportsItems = new HashSet<>(Arrays.asList(
            "frisbee", "sports ball", "kite", "baseball bat", "baseball glove", 
            "tennis racket"
        ));
        
        // Add living beings to navigation obstacles for awareness
        navigationObstacles.addAll(livingBeings);
        
        // Add small sports items to household items
        householdItems.addAll(sportsItems);
        
        Log.i(TAG, "Safety categories initialized: " + 
              immediateDangers.size() + " dangers, " + 
              navigationObstacles.size() + " obstacles, " + 
              householdItems.size() + " household items");
        
        // Verify all 80 COCO classes are categorized
        verifyCategoryCoverage();
    }
    
    /**
     * Verify that all 80 COCO classes are properly categorized
     */
    private void verifyCategoryCoverage() {
        String[] allCocoClasses = {
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
        
        Set<String> uncategorized = new HashSet<>();
        for (String className : allCocoClasses) {
            if (!immediateDangers.contains(className) && 
                !navigationObstacles.contains(className) && 
                !householdItems.contains(className)) {
                uncategorized.add(className);
            }
        }
        
        if (uncategorized.isEmpty()) {
            Log.i(TAG, "✓ All 80 COCO classes properly categorized for blind navigation");
        } else {
            Log.w(TAG, "⚠ Uncategorized classes: " + uncategorized);
        }
        
        Log.i(TAG, "Coverage: Dangers=" + immediateDangers.size() + 
                   ", Obstacles=" + navigationObstacles.size() + 
                   ", Household=" + householdItems.size() + 
                   ", Total=" + (immediateDangers.size() + navigationObstacles.size() + householdItems.size()) + "/80");
    }

    /**
     * Helper methods for navigation callbacks
     */
    private boolean hasCriticalHazards(List<Detection> alerts) {
        return alerts.stream().anyMatch(d -> 
            immediateDangers.contains(d.getLabel()) && d.getRelativeSize() > IMMEDIATE_DANGER_ZONE);
    }
    
    private Detection getMostCritical(List<Detection> alerts) {
        return alerts.stream()
            .filter(d -> immediateDangers.contains(d.getLabel()))
            .max(Comparator.comparing(Detection::getRelativeSize))
            .orElse(alerts.get(0));
    }

    /**
     * Clean resource management
     */
    public void shutdown() {
        Log.i(TAG, "Shutting down navigation system");
        
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
        
        lastAlertTime.clear();
        trackedObjects.clear();
    }

    /**
     * Simple data class for navigation objects
     */
    private static class NavigationObject {
        final String type;
        final float proximity;
        final long lastSeen;
        
        NavigationObject(String type, float proximity) {
            this.type = type;
            this.proximity = proximity;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}

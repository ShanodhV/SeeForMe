package com.shanodh.seeforme.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
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
 * Advanced Real-Time Blind Assistance System
 * Powered by YOLO11 - Full 80 COCO object detection with intelligent prioritization
 * Optimized for moving blind individuals with contextual navigation assistance
 */
public class ObjectDetectionManager {
    private static final String TAG = "BlindAssist";
    
    // YOLO11 Configuration - Enhanced accuracy to reduce false positives
    private static final String MODEL_PATH = "yolo11n_float32.tflite";
    private static final float HIGH_PRIORITY_CONFIDENCE = 0.45f; // Hazards, people, vehicles - increased for accuracy
    private static final float MEDIUM_PRIORITY_CONFIDENCE = 0.40f; // Navigation obstacles - increased for accuracy
    private static final float LOW_PRIORITY_CONFIDENCE = 0.35f; // Household items - increased for accuracy
    
    // Smart timing for moving users
    private static final long DETECTION_INTERVAL = 300; // 3-4 FPS for smooth real-time
    private static final long CRITICAL_ANNOUNCEMENT_INTERVAL = 800; // Hazards every 0.8s
    private static final long NORMAL_ANNOUNCEMENT_INTERVAL = 2500; // Others every 2.5s
    private static final long MINOR_ANNOUNCEMENT_INTERVAL = 4000; // Background items every 4s
    
    // Distance zones for moving navigation
    private static final float IMMEDIATE_ZONE = 0.5f; // Very close - requires immediate attention
    private static final float CLOSE_ZONE = 0.25f; // Close - navigate around
    private static final float AWARENESS_ZONE = 0.10f; // Background awareness
    
    private Context context;
    private Interpreter model;
    private TextToSpeech tts;
    private Vibrator hapticFeedback;
    private ExecutorService inferenceThread;
    private Handler mainThread;
    
    // Navigation state tracking for moving users
    private Map<String, Long> lastAlertTime;
    private Map<String, NavigationObject> trackedObjects;
    private long lastGlobalAlert;
    
    // Smart object categorization for all 80 COCO classes
    private Set<String> criticalHazards;    // Immediate safety concerns
    private Set<String> navigationBarriers; // Physical obstacles to navigate around  
    private Set<String> peoplePets;         // Living beings
    private Set<String> vehicles;           // All vehicles and transport
    private Set<String> furnitureFixtures;  // Indoor navigation landmarks
    private Set<String> handTools;          // Objects that can be picked up/used
    private Set<String> foodDrinks;         // Kitchen and consumables
    private Set<String> electronics;        // Technology items
    
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
        
        initializeIntelligentCategories();
        initializeAdvancedTTS();
    }

    /**
     * Initialize YOLO11 with optimal settings for blind assistance
     */
    public boolean initializeModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFromAssets();
            
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(3); // Boost for better accuracy
            options.setUseXNNPACK(true); // Hardware acceleration
            options.setAllowFp16PrecisionForFp32(true); // Speed boost
            
            model = new Interpreter(modelBuffer, options);
            
            int[] inputShape = model.getInputTensor(0).shape();
            int[] outputShape = model.getOutputTensor(0).shape();
            
            Log.i(TAG, "🚀 YOLO11 Blind Assist Model Ready: Input " + Arrays.toString(inputShape) + 
                      ", Output " + Arrays.toString(outputShape));
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize YOLO11 model", e);
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
     * Advanced real-time analysis for moving blind users
     * Full 80-object detection with intelligent prioritization
     */
    public void analyzeEnvironment(Bitmap cameraFrame, NavigationCallback callback) {
        if (model == null) {
            Log.e(TAG, "❌ Model not initialized! Detection skipped.");
            return;
        }
        
        if (isRateLimited()) {
            return; // Smooth 3-4 FPS processing
        }
        
        inferenceThread.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Run YOLO11 inference
                List<Detection> rawDetections = runOptimizedYOLO11Inference(cameraFrame);
                
                // Intelligent prioritization for all 80 objects
                List<Detection> prioritizedDetections = intelligentPrioritization(rawDetections);
                
                // Generate contextual announcements - only for currently detected objects
                processIntelligentAnnouncements(prioritizedDetections);
                
                long processingTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "🔍 Processed " + rawDetections.size() + " raw → " + prioritizedDetections.size() + " final detections in " + processingTime + "ms");
                
                // Log current detections for debugging
                if (!prioritizedDetections.isEmpty()) {
                    StringBuilder detectedObjects = new StringBuilder("📍 Currently detected: ");
                    for (Detection d : prioritizedDetections) {
                        detectedObjects.append(d.getLabel()).append("(").append(String.format("%.2f", d.getConfidence())).append(") ");
                    }
                    Log.d(TAG, detectedObjects.toString());
                }
                
                mainThread.post(() -> {
                    if (!prioritizedDetections.isEmpty()) {
                        callback.onNavigationUpdate(prioritizedDetections);
                    } else {
                        // Clear announcement history when no objects detected to prevent ghost announcements
                        clearAnnouncementHistory();
                        callback.onPathClear();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Analysis failed", e);
            }
        });
    }
    
    private boolean isRateLimited() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastGlobalAlert) < DETECTION_INTERVAL;
    }

    /**
     * Optimized YOLO11 inference with false positive reduction
     */
    private List<Detection> runOptimizedYOLO11Inference(Bitmap frame) {
        List<Detection> detections = new ArrayList<>();
        
        try {
            // Resize to YOLO11 input size (640x640) with quality preservation
            Bitmap resizedFrame = Bitmap.createScaledBitmap(frame, 640, 640, true);
            float[][][][] input = preprocessImageAdvanced(resizedFrame);
            
            // Run YOLO11 inference - outputs [1, 84, 8400]
            float[][][] output = new float[1][84][8400];
            model.run(input, output);
            
            // Parse with advanced filtering to reduce false positives
            detections = parseYOLO11WithFiltering(transposeOutput(output[0]), frame.getWidth(), frame.getHeight());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ YOLO11 inference failed", e);
        }
        
        return detections;
    }
    
    private float[][][][] preprocessImageAdvanced(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[][][][] input = new float[1][height][width][3];
        
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Advanced preprocessing for better accuracy
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                // Normalized RGB with slight contrast enhancement
                input[0][y][x][0] = Math.min(1.0f, ((pixel >> 16) & 0xFF) / 255.0f * 1.05f); // Red
                input[0][y][x][1] = Math.min(1.0f, ((pixel >> 8) & 0xFF) / 255.0f * 1.05f);  // Green  
                input[0][y][x][2] = Math.min(1.0f, (pixel & 0xFF) / 255.0f * 1.05f);         // Blue
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
     * Advanced YOLO11 parsing with false positive reduction
     */
    private List<Detection> parseYOLO11WithFiltering(float[][] output, int originalWidth, int originalHeight) {
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
        
        int totalProcessed = 0;
        int validDetections = 0;
        
        // Process all 8400 potential detections
        for (int i = 0; i < output.length && detections.size() < 10; i++) { // Increased limit for comprehensive detection
            float[] detection = output[i];
            if (detection.length < 84) continue;
            
            totalProcessed++;
            
            // Extract bounding box (normalized coordinates)
            float centerX = detection[0];
            float centerY = detection[1];
            float width = detection[2];
            float height = detection[3];
            
            // Strict coordinate validation to reduce false positives
            if (centerX < 0.05f || centerX > 0.95f || centerY < 0.05f || centerY > 0.95f ||
                width < 0.02f || width > 0.9f || height < 0.02f || height > 0.9f) {
                continue;
            }
            
            // Find best class prediction with confidence analysis
            int bestClassIdx = 0;
            float bestScore = 0;
            float secondBestScore = 0;
            
            for (int j = 0; j < 80; j++) {
                float score = detection[4 + j];
                if (score > bestScore) {
                    secondBestScore = bestScore;
                    bestScore = score;
                    bestClassIdx = j;
                } else if (score > secondBestScore) {
                    secondBestScore = score;
                }
            }
            
            // Confidence gap analysis to reduce false positives - enhanced validation
            float confidenceGap = bestScore - secondBestScore;
            String className = cocoClasses[bestClassIdx];
            float requiredConfidence = getSmartConfidenceThreshold(className);
            
            // Enhanced validation to prevent misclassification
            boolean isPersonDetection = className.equals("person");
            boolean hasGoodConfidenceGap = confidenceGap > (isPersonDetection ? 0.15f : 0.10f); // Higher gap for person detection
            
            // Apply confidence threshold with enhanced gap analysis
            if (bestScore >= requiredConfidence && hasGoodConfidenceGap) {
                // Convert to pixel coordinates
                int pixelX = (int)(centerX * originalWidth);
                int pixelY = (int)(centerY * originalHeight);
                int pixelW = (int)(width * originalWidth);
                int pixelH = (int)(height * originalHeight);
                
                int left = Math.max(0, pixelX - pixelW/2);
                int top = Math.max(0, pixelY - pixelH/2);
                int right = Math.min(originalWidth, pixelX + pixelW/2);
                int bottom = Math.min(originalHeight, pixelY + pixelH/2);
                
                // Size validation - reject tiny or implausibly large detections
                int detectionWidth = right - left;
                int detectionHeight = bottom - top;
                
                if (detectionWidth > 20 && detectionHeight > 20 && 
                    detectionWidth < originalWidth * 0.8f && detectionHeight < originalHeight * 0.8f) {
                    
                    detections.add(new Detection(className, bestScore, left, top, right, bottom, 
                                               originalWidth, originalHeight));
                    validDetections++;
                    
                    // Debug logging for person detection accuracy
                    if (className.equals("person") || className.equals("refrigerator")) {
                        Log.d(TAG, "🔍 " + className + " detected - confidence: " + String.format("%.3f", bestScore) + 
                                   ", gap: " + String.format("%.3f", confidenceGap) + 
                                   ", size: " + detectionWidth + "x" + detectionHeight);
                    }
                }
            }
        }
        
        Log.d(TAG, "🔍 Processed " + totalProcessed + " candidates, found " + validDetections + " valid detections");
        
        return applyAdvancedNMS(detections);
    }
    
    private float getSmartConfidenceThreshold(String className) {
        if (criticalHazards.contains(className) || vehicles.contains(className)) {
            return HIGH_PRIORITY_CONFIDENCE;
        } else if (navigationBarriers.contains(className) || peoplePets.contains(className)) {
            return MEDIUM_PRIORITY_CONFIDENCE;
        } else {
            return LOW_PRIORITY_CONFIDENCE;
        }
    }

    /**
     * Advanced Non-Maximum Suppression with category awareness
     */
    private List<Detection> applyAdvancedNMS(List<Detection> detections) {
        if (detections.isEmpty()) return detections;
        
        // Sort by confidence first
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        List<Detection> filtered = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];
        
        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;
            
            Detection current = detections.get(i);
            filtered.add(current);
            
            // Suppress overlapping detections with lower confidence
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                
                Detection other = detections.get(j);
                
                // Same class - apply standard NMS
                if (current.getLabel().equals(other.getLabel()) && 
                    calculateIoU(current, other) > 0.3f) {
                    suppressed[j] = true;
                }
                // Different classes - more lenient for better detection diversity
                else if (calculateIoU(current, other) > 0.6f) {
                    suppressed[j] = true;
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * Intelligent prioritization for all 80 COCO objects
     * Priority: Critical Hazards > People/Pets > Vehicles > Navigation Barriers > Objects
     */
    private List<Detection> intelligentPrioritization(List<Detection> detections) {
        List<Detection> prioritized = new ArrayList<>();
        
        for (Detection detection : detections) {
            String object = detection.getLabel();
            float size = detection.getRelativeSize();
            int priority = getObjectPriority(object);
            float minSize = getMinimumSizeForPriority(priority);
            
            // Include if object meets size threshold for its priority level
            if (size >= minSize) {
                prioritized.add(detection);
                Log.d(TAG, "✅ " + object + " (P" + priority + ", size: " + String.format("%.3f", size) + ")");
            } else {
                Log.d(TAG, "⏭️ " + object + " too small (size: " + String.format("%.3f", size) + " < " + String.format("%.3f", minSize) + ")");
            }
        }
        
        // Sort by priority, then by size within same priority
        prioritized.sort((a, b) -> {
            int priorityA = getObjectPriority(a.getLabel());
            int priorityB = getObjectPriority(b.getLabel());
            
            if (priorityA != priorityB) {
                return Integer.compare(priorityA, priorityB); // Lower number = higher priority
            }
            
            return Float.compare(b.getRelativeSize(), a.getRelativeSize()); // Larger objects first
        });
        
        // Return top detections to avoid overwhelming user
        List<Detection> finalList = prioritized.subList(0, Math.min(5, prioritized.size()));
        Log.d(TAG, "🎯 Final priority list: " + finalList.size() + " objects");
        
        return finalList;
    }
    
    private int getObjectPriority(String object) {
        if (criticalHazards.contains(object)) return 1; // Highest priority
        if (peoplePets.contains(object)) return 2; // People and animals
        if (vehicles.contains(object)) return 3; // Vehicles and transport
        if (navigationBarriers.contains(object)) return 4; // Furniture and barriers
        if (electronics.contains(object)) return 5; // Technology items
        if (handTools.contains(object)) return 6; // Tools and utensils
        if (foodDrinks.contains(object)) return 7; // Food and drinks
        return 8; // Everything else
    }
    
    private float getMinimumSizeForPriority(int priority) {
        switch (priority) {
            case 1: return 0.05f; // Critical hazards - detect even small ones
            case 2: return 0.08f; // People/pets - important for navigation
            case 3: return 0.10f; // Vehicles - safety critical
            case 4: return 0.15f; // Navigation barriers - need to be substantial
            case 5: return 0.12f; // Electronics - moderate size requirement
            case 6: return 0.08f; // Hand tools - can be smaller
            case 7: return 0.06f; // Food/drinks - can be small
            default: return 0.20f; // Other objects - only if significant
        }
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
     * Intelligent announcement system for moving blind users
     * Only announces objects that are CURRENTLY detected - no ghost announcements
     */
    private void processIntelligentAnnouncements(List<Detection> detections) {
        long currentTime = System.currentTimeMillis();
        
        // Create set of currently detected objects to prevent ghost announcements
        Set<String> currentlyDetected = new HashSet<>();
        for (Detection detection : detections) {
            currentlyDetected.add(detection.getLabel());
        }
        
        // Clear announcement times for objects no longer detected
        lastAlertTime.entrySet().removeIf(entry -> {
            String object = entry.getKey();
            if (!currentlyDetected.contains(object)) {
                Log.d(TAG, "🚫 " + object + " no longer detected - clearing announcement history");
                return true; // Remove from lastAlertTime
            }
            return false; // Keep in lastAlertTime
        });
        
        // Process announcements only for currently detected objects
        for (Detection detection : detections) {
            String object = detection.getLabel();
            int priority = getObjectPriority(object);
            
            // Check if we should announce this object based on its priority
            if (shouldAnnounceObject(object, priority, currentTime)) {
                generateContextualAnnouncement(detection);
                lastAlertTime.put(object, currentTime);
            }
        }
        
        lastGlobalAlert = currentTime;
    }
    
    /**
     * Clear announcement history when no objects are detected
     * Prevents ghost announcements of previously seen objects
     */
    private void clearAnnouncementHistory() {
        if (!lastAlertTime.isEmpty()) {
            Log.d(TAG, "🧹 Clearing announcement history for " + lastAlertTime.size() + " objects - no detections");
            lastAlertTime.clear();
        }
    }
    
    private boolean shouldAnnounceObject(String object, int priority, long currentTime) {
        Long lastAnnouncement = lastAlertTime.get(object);
        
        if (lastAnnouncement == null) return true; // First detection
        
        // Different intervals based on priority
        long interval;
        if (priority <= 2) { // Critical hazards and people
            interval = CRITICAL_ANNOUNCEMENT_INTERVAL;
        } else if (priority <= 4) { // Vehicles and navigation barriers
            interval = NORMAL_ANNOUNCEMENT_INTERVAL;
        } else { // Everything else
            interval = MINOR_ANNOUNCEMENT_INTERVAL;
        }
        
        return (currentTime - lastAnnouncement) > interval;
    }
    
    private void generateContextualAnnouncement(Detection detection) {
        String object = detection.getLabel();
        String direction = getDetailedDirection(detection);
        String distance = getDetailedDistance(detection.getRelativeSize());
        
        // Create context-aware, helpful announcements
        String message = createIntelligentMessage(object, direction, distance);
        
        // Appropriate haptic feedback based on object importance
        int priority = getObjectPriority(object);
        if (priority <= 2) {
            triggerUrgentHaptic();
        } else if (priority <= 4) {
            triggerWarningHaptic();
        } else {
            triggerInfoHaptic();
        }
        
        // Speak the announcement
        speakIntelligently(message);
        
        Log.i(TAG, "🔊 " + message);
    }
    
    private String createIntelligentMessage(String object, String direction, String distance) {
        // Category-based intelligent messaging
        if (criticalHazards.contains(object)) {
            return "⚠️ " + object + " " + distance + " " + direction + " - be careful";
        } else if (peoplePets.contains(object)) {
            if (object.equals("person")) {
                return "👤 Someone " + distance + " " + direction;
            } else {
                return "🐕 " + object + " " + distance + " " + direction;
            }
        } else if (vehicles.contains(object)) {
            return "🚗 " + object + " " + distance + " " + direction;
        } else if (navigationBarriers.contains(object)) {
            return "🪑 " + object + " " + distance + " " + direction + " - navigate around";
        } else if (electronics.contains(object)) {
            return "💻 " + object + " " + distance + " " + direction;
        } else if (handTools.contains(object)) {
            return "🔧 " + object + " " + distance + " " + direction;
        } else if (foodDrinks.contains(object)) {
            return "🍽️ " + object + " " + distance + " " + direction;
        } else {
            return "📦 " + object + " " + distance + " " + direction;
        }
    }
    
    private String getDetailedDirection(Detection detection) {
        float centerX = detection.getCenterX();
        float imageWidth = detection.getImageWidth();
        float position = centerX / imageWidth;
        
        if (position < 0.2f) return "far left";
        else if (position < 0.4f) return "left";
        else if (position < 0.6f) return "center";
        else if (position < 0.8f) return "right";
        else return "far right";
    }
    
    private String getDetailedDistance(float size) {
        if (size > IMMEDIATE_ZONE) return "very close";
        else if (size > CLOSE_ZONE) return "nearby";
        else if (size > AWARENESS_ZONE) return "in distance";
        else return "far away";
    }

    /**
     * Haptic feedback patterns for different alert levels
     * Compatible with API 24+ using legacy vibration methods
     */
    private void triggerUrgentHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Rapid pulses for immediate danger - API 24 compatible
            long[] pattern = {0, 100, 50, 100, 50, 100}; // wait, vibrate, wait, vibrate...
            hapticFeedback.vibrate(pattern, -1); // -1 means don't repeat
        }
    }
    
    private void triggerWarningHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Double pulse for warnings - API 24 compatible
            long[] pattern = {0, 200, 100, 200}; // wait, vibrate, wait, vibrate
            hapticFeedback.vibrate(pattern, -1);
        }
    }
    
    private void triggerInfoHaptic() {
        if (hapticFeedback != null && hapticFeedback.hasVibrator()) {
            // Single pulse for information - API 24 compatible
            hapticFeedback.vibrate(150); // Simple vibration for 150ms
        }
    }

    /**
     * Advanced Text-to-Speech optimized for blind navigation
     */
    private void initializeAdvancedTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.85f); // Optimal speed for comprehension while moving
                tts.setPitch(0.95f); // Slightly lower pitch for clarity
                Log.i(TAG, "🔊 Advanced TTS initialized for blind assistance");
            } else {
                Log.e(TAG, "❌ TTS initialization failed");
            }
        });
    }
    
    private void speakIntelligently(String message) {
        if (tts != null) {
            // Smart queue management - interrupt for critical items, queue for others
            if (message.contains("⚠️")) {
                tts.stop(); // Interrupt for critical hazards
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "critical_alert");
            } else {
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, "object_info");
            }
        }
    }

    /**
     * Initialize intelligent object categorization for all 80 COCO classes
     * Optimized for blind navigation and environmental awareness
     */
    private void initializeIntelligentCategories() {
        // 🚨 CRITICAL HAZARDS - Immediate safety concerns (Priority 1)
        criticalHazards = new HashSet<>(Arrays.asList(
            "knife", "scissors", // Sharp objects
            "fire hydrant", "stop sign", "traffic light" // Traffic safety indicators
        ));
        
        // 👥 PEOPLE & PETS - Living beings (Priority 2)
        peoplePets = new HashSet<>(Arrays.asList(
            "person", "cat", "dog", "bird", "horse", "sheep", "cow", 
            "elephant", "bear", "zebra", "giraffe"
        ));
        
        // 🚗 VEHICLES - All transportation (Priority 3)
        vehicles = new HashSet<>(Arrays.asList(
            "bicycle", "car", "motorcycle", "airplane", "bus", "train", 
            "truck", "boat"
        ));
        
        // 🪑 NAVIGATION BARRIERS - Physical obstacles (Priority 4)
        navigationBarriers = new HashSet<>(Arrays.asList(
            "chair", "couch", "dining table", "bed", "bench", "toilet",
            "refrigerator", "tv", "sink", "potted plant", "backpack", 
            "suitcase", "surfboard", "skateboard", "skis", "snowboard"
        ));
        
        // 💻 ELECTRONICS - Technology items (Priority 5)
        electronics = new HashSet<>(Arrays.asList(
            "laptop", "mouse", "remote", "keyboard", "cell phone", "clock"
        ));
        
        // 🔧 HAND TOOLS & UTENSILS - Graspable objects (Priority 6)
        handTools = new HashSet<>(Arrays.asList(
            "umbrella", "handbag", "tie", "bottle", "wine glass", "cup", 
            "fork", "spoon", "bowl", "vase", "frisbee", "sports ball", 
            "kite", "baseball bat", "baseball glove", "tennis racket",
            "book", "hair drier", "toothbrush", "teddy bear"
        ));
        
        // 🍽️ FOOD & DRINKS - Consumables (Priority 7)
        foodDrinks = new HashSet<>(Arrays.asList(
            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", 
            "hot dog", "pizza", "donut", "cake"
        ));
        
        // 🏠 HOUSEHOLD FIXTURES - Appliances and fixed items (Priority 8)
        furnitureFixtures = new HashSet<>(Arrays.asList(
            "microwave", "oven", "toaster", "parking meter"
        ));
        
        int totalCategorized = criticalHazards.size() + peoplePets.size() + vehicles.size() + 
                              navigationBarriers.size() + electronics.size() + handTools.size() + 
                              foodDrinks.size() + furnitureFixtures.size();
        
        Log.i(TAG, "🎯 Intelligent categories initialized:");
        Log.i(TAG, "   🚨 Critical: " + criticalHazards.size());
        Log.i(TAG, "   👥 People/Pets: " + peoplePets.size());
        Log.i(TAG, "   🚗 Vehicles: " + vehicles.size());
        Log.i(TAG, "   🪑 Barriers: " + navigationBarriers.size());
        Log.i(TAG, "   💻 Electronics: " + electronics.size());
        Log.i(TAG, "   🔧 Tools: " + handTools.size());
        Log.i(TAG, "   🍽️ Food: " + foodDrinks.size());
        Log.i(TAG, "   🏠 Fixtures: " + furnitureFixtures.size());
        Log.i(TAG, "   📊 Total: " + totalCategorized + "/80 COCO classes");
        
        // Verify complete coverage
        verifyIntelligentCoverage();
    }
    
    /**
     * Verify that all 80 COCO classes are intelligently categorized
     */
    private void verifyIntelligentCoverage() {
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
            if (!criticalHazards.contains(className) && 
                !peoplePets.contains(className) && 
                !vehicles.contains(className) &&
                !navigationBarriers.contains(className) &&
                !electronics.contains(className) &&
                !handTools.contains(className) &&
                !foodDrinks.contains(className) &&
                !furnitureFixtures.contains(className)) {
                uncategorized.add(className);
            }
        }
        
        if (uncategorized.isEmpty()) {
            Log.i(TAG, "✅ Perfect! All 80 COCO classes intelligently categorized");
        } else {
            Log.w(TAG, "⚠️ Missing categories for: " + uncategorized);
        }
    }

    /**
     * Helper methods for navigation callbacks
     */
    private boolean hasCriticalHazards(List<Detection> alerts) {
        return alerts.stream().anyMatch(d -> 
            criticalHazards.contains(d.getLabel()) && d.getRelativeSize() > IMMEDIATE_ZONE);
    }
    
    private Detection getMostCritical(List<Detection> alerts) {
        return alerts.stream()
            .filter(d -> criticalHazards.contains(d.getLabel()))
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

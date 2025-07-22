package com.shanodh.seeforme.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.shanodh.seeforme.ml.Detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom overlay view to display object detection bounding boxes
 * Color-coded by priority for demonstration purposes
 */
public class DetectionOverlayView extends View {
    private static final String TAG = "DetectionOverlay";
    
    private List<Detection> detections = new ArrayList<>();
    private Paint boundingBoxPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    
    // Object categories for color coding (matching ObjectDetectionManager)
    private Set<String> criticalHazards;
    private Set<String> peoplePets;
    private Set<String> vehicles;
    private Set<String> navigationBarriers;
    private Set<String> electronics;
    
    public DetectionOverlayView(Context context) {
        super(context);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        initializeCategories();
        
        // Initialize paints for drawing
        boundingBoxPaint = new Paint();
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(4f);
        boundingBoxPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
    }
    
    private void initializeCategories() {
        // Initialize object categories for color coding
        criticalHazards = new HashSet<>(Arrays.asList(
            "knife", "scissors", "fire hydrant", "stop sign", "traffic light"
        ));
        
        peoplePets = new HashSet<>(Arrays.asList(
            "person", "cat", "dog", "bird", "horse", "sheep", "cow", 
            "elephant", "bear", "zebra", "giraffe"
        ));
        
        vehicles = new HashSet<>(Arrays.asList(
            "bicycle", "car", "motorcycle", "airplane", "bus", "train", 
            "truck", "boat"
        ));
        
        navigationBarriers = new HashSet<>(Arrays.asList(
            "chair", "couch", "dining table", "bed", "bench", "toilet",
            "refrigerator", "tv", "sink", "potted plant", "backpack", 
            "suitcase", "surfboard", "skateboard", "skis", "snowboard"
        ));
        
        electronics = new HashSet<>(Arrays.asList(
            "laptop", "mouse", "remote", "keyboard", "cell phone", "clock"
        ));
    }
    
    /**
     * Update detections and trigger redraw
     */
    public void updateDetections(List<Detection> newDetections) {
        this.detections = new ArrayList<>(newDetections);
        invalidate(); // Trigger onDraw
    }
    
    /**
     * Clear all detections
     */
    public void clearDetections() {
        this.detections.clear();
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (detections == null || detections.isEmpty()) {
            return;
        }
        
        for (Detection detection : detections) {
            drawDetection(canvas, detection);
        }
    }
    
    private void drawDetection(Canvas canvas, Detection detection) {
        // Get priority-based color
        int color = getPriorityColor(detection.getLabel());
        boundingBoxPaint.setColor(color);
        
        // Draw bounding box
        canvas.drawRect(
            detection.getLeft(),
            detection.getTop(),
            detection.getRight(),
            detection.getBottom(),
            boundingBoxPaint
        );
        
        // Prepare label text
        String label = detection.getLabel() + " " + 
                      String.format("%.0f%%", detection.getConfidence() * 100);
        
        // Calculate text background
        Rect textBounds = new Rect();
        textPaint.getTextBounds(label, 0, label.length(), textBounds);
        
        float textX = detection.getLeft();
        float textY = detection.getTop() - 8;
        
        // Ensure text stays within view bounds
        if (textY < textBounds.height()) {
            textY = detection.getTop() + textBounds.height() + 8;
        }
        
        // Draw semi-transparent background for text
        backgroundPaint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(
            textX - 4,
            textY - textBounds.height() - 4,
            textX + textBounds.width() + 8,
            textY + 4,
            backgroundPaint
        );
        
        // Draw text
        canvas.drawText(label, textX, textY, textPaint);
        
        // Draw priority indicator (small colored circle)
        float circleX = detection.getRight() - 15;
        float circleY = detection.getTop() + 15;
        boundingBoxPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(circleX, circleY, 8, boundingBoxPaint);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
    }
    
    /**
     * Get color based on object priority
     */
    private int getPriorityColor(String objectLabel) {
        if (criticalHazards.contains(objectLabel)) {
            return Color.argb(255, 255, 0, 0); // Red - Critical hazards
        } else if (peoplePets.contains(objectLabel)) {
            return Color.argb(255, 255, 165, 0); // Orange - People/pets
        } else if (vehicles.contains(objectLabel)) {
            return Color.argb(255, 255, 255, 0); // Yellow - Vehicles
        } else if (navigationBarriers.contains(objectLabel)) {
            return Color.argb(255, 0, 100, 255); // Blue - Navigation barriers
        } else if (electronics.contains(objectLabel)) {
            return Color.argb(255, 128, 0, 128); // Purple - Electronics
        } else {
            return Color.argb(255, 0, 255, 0); // Green - Other objects
        }
    }
    
    /**
     * Get priority level for object (for debugging)
     */
    public int getObjectPriority(String object) {
        if (criticalHazards.contains(object)) return 1;
        if (peoplePets.contains(object)) return 2;
        if (vehicles.contains(object)) return 3;
        if (navigationBarriers.contains(object)) return 4;
        if (electronics.contains(object)) return 5;
        return 6; // Default priority
    }
}

package com.shanodh.seeforme.ml;

import android.graphics.RectF;

/**
 * Detection result class for object detection
 */
public class Detection {
    private String label;
    private float confidence;
    private RectF boundingBox;
    private float relativeSize;
    private float centerX;
    private float centerY;
    private float imageWidth;
    private float imageHeight;

    public Detection(String label, float confidence, float left, float top, 
                    float right, float bottom, float imageWidth, float imageHeight) {
        this.label = label;
        this.confidence = confidence;
        this.boundingBox = new RectF(left, top, right, bottom);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        
        // Calculate derived properties
        this.centerX = (left + right) / 2;
        this.centerY = (top + bottom) / 2;
        float area = (right - left) * (bottom - top);
        float totalArea = imageWidth * imageHeight;
        this.relativeSize = area / totalArea;
    }

    // Getters
    public String getLabel() { return label; }
    public float getConfidence() { return confidence; }
    public RectF getBoundingBox() { return boundingBox; }
    public float getLeft() { return boundingBox.left; }
    public float getTop() { return boundingBox.top; }
    public float getRight() { return boundingBox.right; }
    public float getBottom() { return boundingBox.bottom; }
    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getRelativeSize() { return relativeSize; }
    public float getImageWidth() { return imageWidth; }
    public float getImageHeight() { return imageHeight; }
    
    public float getArea() {
        return boundingBox.width() * boundingBox.height();
    }
    
    @Override
    public String toString() {
        return String.format("Detection{label='%s', confidence=%.2f, center=(%.1f,%.1f), size=%.3f}", 
                           label, confidence, centerX, centerY, relativeSize);
    }
}

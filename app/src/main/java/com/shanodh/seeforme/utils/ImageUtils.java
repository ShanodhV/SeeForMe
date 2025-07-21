package com.shanodh.seeforme.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Utility class for image processing and conversion
 * Optimized for camera feed to ML model pipeline
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    
    /**
     * Convert CameraX ImageProxy to Bitmap
     * Handles various image formats efficiently
     */
    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            Image.Plane[] planes = image.getImage().getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 
                    image.getWidth(), image.getHeight(), null);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, 
                    yuvImage.getWidth(), yuvImage.getHeight()), 85, out);
            
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            
            // Rotate bitmap if needed (camera images are often rotated)
            return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) {
            return bitmap;
        }
        
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, 
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory rotating bitmap", e);
            return bitmap; // Return original if rotation fails
        }
    }
    
    /**
     * Resize bitmap to target size while maintaining aspect ratio
     * Optimized for ML model input
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null) return null;
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Calculate scaling factor to maintain aspect ratio
        float scaleWidth = (float) targetWidth / width;
        float scaleHeight = (float) targetHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        // Calculate new dimensions
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        // Create scaled bitmap
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        
        // Create final bitmap with target dimensions (pad if necessary)
        Bitmap finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(finalBitmap);
        
        // Center the scaled bitmap
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;
        canvas.drawBitmap(scaledBitmap, x, y, null);
        
        return finalBitmap;
    }
    
    /**
     * Convert bitmap to normalized float array for ML input
     */
    public static float[][][][] bitmapToFloatArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float[][][][] array = new float[1][height][width][3];
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = bitmap.getPixel(j, i);
                
                // Normalize to [0, 1] range
                array[0][i][j][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red
                array[0][i][j][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green
                array[0][i][j][2] = (pixel & 0xFF) / 255.0f;         // Blue
            }
        }
        
        return array;
    }
    
    /**
     * Convert bitmap to byte array for quantized models
     */
    public static byte[][][][] bitmapToByteArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        byte[][][][] array = new byte[1][height][width][3];
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = bitmap.getPixel(j, i);
                
                array[0][i][j][0] = (byte) ((pixel >> 16) & 0xFF); // Red
                array[0][i][j][1] = (byte) ((pixel >> 8) & 0xFF);  // Green
                array[0][i][j][2] = (byte) (pixel & 0xFF);         // Blue
            }
        }
        
        return array;
    }
    
    /**
     * Simple and fast ImageProxy to Bitmap conversion for real-time processing
     * Uses a more direct approach for better performance
     */
    public static Bitmap fastImageProxyToBitmap(ImageProxy image) {
        try {
            // For JPEG format (if supported by camera)
            if (image.getFormat() == ImageFormat.JPEG) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            
            // For YUV format (most common)
            return yuv420ToBitmap(image);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in fast conversion", e);
            return null;
        }
    }
    
    /**
     * Convert YUV_420_888 to Bitmap
     */
    private static Bitmap yuv420ToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        
        // Interleave U and V bytes
        byte[] uvPixels = new byte[uSize + vSize];
        uBuffer.get(uvPixels, 0, uSize);
        vBuffer.get(uvPixels, uSize, vSize);

        // Convert to NV21 format
        int uvPixelCount = 0;
        for (int i = ySize; i < nv21.length; i += 2) {
            nv21[i] = uvPixels[uvPixelCount + 1]; // V
            nv21[i + 1] = uvPixels[uvPixelCount]; // U
            uvPixelCount += 2;
        }

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 
                image.getWidth(), image.getHeight(), null);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 
                75, out); // Lower quality for speed
        
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    
    /**
     * Calculate distance estimation based on bounding box size
     * This is a simplified estimation - real distance would require depth sensors
     */
    public static String estimateDistance(float boundingBoxHeight, float imageHeight, String objectType) {
        float relativeSize = boundingBoxHeight / imageHeight;
        
        // Object-specific distance estimation
        switch (objectType.toLowerCase()) {
            case "person":
                if (relativeSize > 0.7f) return "very close (within arm's reach)";
                if (relativeSize > 0.4f) return "close (about 2-3 steps away)";
                if (relativeSize > 0.2f) return "medium distance (several steps away)";
                return "far distance";
                
            case "car":
            case "truck":
            case "bus":
                if (relativeSize > 0.5f) return "DANGER - very close vehicle";
                if (relativeSize > 0.3f) return "close vehicle - be careful";
                if (relativeSize > 0.15f) return "nearby vehicle";
                return "distant vehicle";
                
            case "chair":
            case "table":
                if (relativeSize > 0.4f) return "right in front of you";
                if (relativeSize > 0.2f) return "nearby furniture";
                return "distant furniture";
                
            default:
                if (relativeSize > 0.6f) return "very close";
                if (relativeSize > 0.3f) return "close";
                if (relativeSize > 0.15f) return "medium distance";
                return "far away";
        }
    }
}

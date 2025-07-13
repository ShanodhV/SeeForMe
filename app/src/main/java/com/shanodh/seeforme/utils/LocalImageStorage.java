package com.shanodh.seeforme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Helper class for local image storage since Firebase Storage requires billing
 */
public class LocalImageStorage {
    private static final String TAG = "LocalImageStorage";
    private static final String IMAGES_FOLDER = "face_images";
    
    /**
     * Save a bitmap image to local storage
     * @param context Application context
     * @param bitmap Image to save
     * @param userId User ID for organizing images
     * @return Local file path of saved image
     */
    public static String saveImageLocally(Context context, Bitmap bitmap, String userId) {
        try {
            // Create user-specific directory
            File userDir = new File(context.getFilesDir(), IMAGES_FOLDER + "/" + userId);
            if (!userDir.exists()) {
                userDir.mkdirs();
            }
            
            // Create unique filename
            String filename = UUID.randomUUID().toString() + ".jpg";
            File imageFile = new File(userDir, filename);
            
            // Save bitmap to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            
            Log.d(TAG, "Image saved locally: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving image locally", e);
            return null;
        }
    }
    
    /**
     * Load a bitmap from local storage
     * @param imagePath Local file path
     * @return Bitmap or null if failed
     */
    public static Bitmap loadImageFromLocal(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return null;
            }
            
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.w(TAG, "Image file not found: " + imagePath);
                return null;
            }
            
            return BitmapFactory.decodeFile(imagePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from local storage", e);
            return null;
        }
    }
    
    /**
     * Delete an image from local storage
     * @param imagePath Local file path
     * @return true if deleted successfully
     */
    public static boolean deleteImageFromLocal(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return false;
            }
            
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                Log.d(TAG, "Image deleted: " + imagePath + " - Success: " + deleted);
                return deleted;
            }
            
            return true; // File doesn't exist, consider it deleted
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image from local storage", e);
            return false;
        }
    }
    
    /**
     * Get the size of all stored images for a user
     * @param context Application context
     * @param userId User ID
     * @return Size in bytes
     */
    public static long getUserImagesSize(Context context, String userId) {
        File userDir = new File(context.getFilesDir(), IMAGES_FOLDER + "/" + userId);
        if (!userDir.exists()) {
            return 0;
        }
        
        long totalSize = 0;
        File[] files = userDir.listFiles();
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        
        return totalSize;
    }
    
    /**
     * Clean up old images for a user (optional - for storage management)
     * @param context Application context
     * @param userId User ID
     * @param maxImages Maximum number of images to keep
     */
    public static void cleanupOldImages(Context context, String userId, int maxImages) {
        File userDir = new File(context.getFilesDir(), IMAGES_FOLDER + "/" + userId);
        if (!userDir.exists()) {
            return;
        }
        
        File[] files = userDir.listFiles();
        if (files == null || files.length <= maxImages) {
            return;
        }
        
        // Sort by last modified (oldest first)
        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
        
        // Delete oldest files
        int filesToDelete = files.length - maxImages;
        for (int i = 0; i < filesToDelete; i++) {
            boolean deleted = files[i].delete();
            Log.d(TAG, "Cleaned up old image: " + files[i].getName() + " - Success: " + deleted);
        }
    }
}

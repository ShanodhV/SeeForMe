package com.shanodh.seeforme.utils;

import android.content.Context;
import android.util.Log;
import java.io.IOException;

/**
 * Simple utility to verify that the TensorFlow Lite model exists
 */
public class ModelVerifier {
    private static final String TAG = "ModelVerifier";
    private static final String MODEL_PATH = "yolo_mobile_model.tflite";
    
    /**
     * Verify that the TensorFlow Lite model file exists
     * Call this from onCreate() to check model presence
     */
    public static boolean verifyModel(Context context) {
        try {
            Log.d(TAG, "Checking if TensorFlow Lite model exists...");
            
            // Check if model file exists in assets
            context.getAssets().open(MODEL_PATH).close();
            
            Log.d(TAG, "✅ Model file found in assets!");
            getModelInfo(context);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Model verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get information about the model file
     */
    public static void getModelInfo(Context context) {
        try {
            android.content.res.AssetFileDescriptor fileDescriptor = 
                context.getAssets().openFd(MODEL_PATH);
            
            long fileSize = fileDescriptor.getLength();
            Log.d(TAG, "Model file size: " + (fileSize / 1024) + " KB");
            
            fileDescriptor.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Error getting model info: " + e.getMessage());
        }
    }
}

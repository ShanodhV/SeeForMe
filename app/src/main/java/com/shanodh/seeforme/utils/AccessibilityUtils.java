package com.shanodh.seeforme.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;

/**
 * Utility class for accessibility features
 */
public class AccessibilityUtils {

    /**
     * Provides a short vibration feedback
     * @param context Context to get the vibration service
     */
    public static void performHapticFeedback(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    /**
     * Provides vibration feedback for error conditions
     * @param context Context to get the vibration service
     */
    public static void performErrorHapticFeedback(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern for error feedback: vibrate, pause, vibrate
                long[] pattern = {0, 100, 100, 100};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                long[] pattern = {0, 100, 100, 100};
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    /**
     * Announces a message to the screen reader
     * @param context Context to get the accessibility service
     * @param text Text to announce
     */
    public static void announceForAccessibility(Context context, String text) {
        AccessibilityManager accessibilityManager = 
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(text);
            event.setClassName(AccessibilityUtils.class.getName());
            event.setPackageName(context.getPackageName());
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    /**
     * Shows an accessible snackbar message
     * @param rootView The view to attach the snackbar to
     * @param message The message to display and announce
     * @param duration Duration of the snackbar
     * @param tts Optional TextToSpeech instance to speak the message
     */
    public static void showAccessibleMessage(View rootView, String message, int duration, TextToSpeech tts) {
        Snackbar snackbar = Snackbar.make(rootView, message, duration);
        View snackbarView = snackbar.getView();
        
        // Make text larger for visibility
        TextView textView = snackbarView.findViewById(R.id.snackbar_text);
        if (textView != null) {
            textView.setTextSize(18);
            textView.setContentDescription(message);
        }
        
        // Set high contrast background
        snackbarView.setBackgroundResource(com.shanodh.seeforme.R.color.primary_dark);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(rootView.getContext(),
                    com.shanodh.seeforme.R.color.white));
        }
        
        snackbar.show();
        
        // Speak the message if TTS is available
        if (tts != null && tts.isSpeaking()) {
            tts.speak(message, TextToSpeech.QUEUE_ADD, null, "announcement_id");
        }
        
        // Also announce for accessibility
        announceForAccessibility(rootView.getContext(), message);
    }

    /**
     * Checks if a screen reader is active
     * @param context Context to get the accessibility service
     * @return true if a screen reader is active
     */
    public static boolean isScreenReaderActive(Context context) {
        AccessibilityManager accessibilityManager = 
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager != null && accessibilityManager.isEnabled() 
                && accessibilityManager.isTouchExplorationEnabled();
    }
} 
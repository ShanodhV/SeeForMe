package com.shanodh.seeforme.ml;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Spatial Audio Manager for providing directional audio cues
 * Helps blind users understand object positions through audio feedback
 */
public class SpatialAudioManager {
    private static final String TAG = "SpatialAudioManager";
    
    private Context context;
    private ToneGenerator toneGenerator;
    private Vibrator vibrator;
    private AudioManager audioManager;
    
    // Audio settings
    private static final int TONE_DURATION = 300; // milliseconds
    private static final int BASE_FREQUENCY = 800; // Hz
    private static final int VOLUME = 80; // 0-100
    
    public SpatialAudioManager(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME);
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            Log.d(TAG, "Spatial audio initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing spatial audio", e);
        }
    }
    
    /**
     * Play directional alert based on object position
     * @param objectX X position of object in image
     * @param imageWidth Width of the image
     */
    public void playDirectionalAlert(float objectX, float imageWidth) {
        if (toneGenerator == null) return;
        
        try {
            // Calculate relative position (0.0 = far left, 1.0 = far right)
            float relativePosition = objectX / imageWidth;
            
            // Generate spatial audio cue
            if (relativePosition < 0.3f) {
                // Object on left - lower frequency, left-biased
                playTone(BASE_FREQUENCY - 200, TONE_DURATION);
                vibratePattern(new long[]{0, 100, 100, 100}); // Short-pause-short pattern
            } else if (relativePosition > 0.7f) {
                // Object on right - higher frequency, right-biased  
                playTone(BASE_FREQUENCY + 200, TONE_DURATION);
                vibratePattern(new long[]{0, 200, 50, 100}); // Long-short-short pattern
            } else {
                // Object in center - base frequency
                playTone(BASE_FREQUENCY, TONE_DURATION);
                vibratePattern(new long[]{0, 150}); // Single vibration
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing directional alert", e);
        }
    }
    
    /**
     * Play collision warning sound
     */
    public void playCollisionWarning() {
        if (toneGenerator == null) return;
        
        try {
            // Rapid high-frequency beeps for immediate danger
            for (int i = 0; i < 3; i++) {
                playTone(1500, 150);
                Thread.sleep(100);
            }
            
            // Strong vibration pattern
            vibratePattern(new long[]{0, 200, 100, 200, 100, 200});
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing collision warning", e);
        }
    }
    
    /**
     * Play hazard alert sound
     */
    public void playHazardAlert() {
        if (toneGenerator == null) return;
        
        try {
            // Medium frequency warning tone
            playTone(1000, 400);
            
            // Medium vibration
            vibratePattern(new long[]{0, 300, 200, 300});
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing hazard alert", e);
        }
    }
    
    private void playTone(int frequency, int duration) {
        // Note: ToneGenerator doesn't support custom frequencies easily
        // Using predefined tones as approximation
        if (frequency < 700) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, duration);
        } else if (frequency > 1200) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, duration);
        } else {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, duration);
        }
    }
    
    private void vibratePattern(long[] pattern) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    public void cleanup() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}

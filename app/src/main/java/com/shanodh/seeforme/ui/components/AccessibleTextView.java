package com.shanodh.seeforme.ui.components;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.shanodh.seeforme.utils.AccessibilityUtils;
import com.shanodh.seeforme.utils.AccessibleGestureDetector;

/**
 * Enhanced TextView with accessibility features for visually impaired users
 */
public class AccessibleTextView extends AppCompatTextView {
    
    private TextToSpeech tts;
    private boolean isSpeaking = false;

    public AccessibleTextView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AccessibleTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AccessibleTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setFocusable(true);
        
        // Set minimum touch target size (48dp)
        setMinimumHeight(convertDpToPx(48));
        setMinimumWidth(convertDpToPx(48));
        
        // Setup gesture detection
        setupGestureDetection();
    }
    
    private void setupGestureDetection() {
        AccessibleGestureDetector.setupGestures(this, 
            new AccessibleGestureDetector.SimpleOnAccessibleGestureListener() {
                @Override
                public boolean onSingleTap() {
                    if (tts != null) {
                        if (isSpeaking) {
                            stopSpeaking();
                        } else {
                            speakText();
                        }
                        return true;
                    }
                    return false;
                }
                
                @Override
                public boolean onDoubleTap() {
                    // Double tap to announce the full content
                    if (tts != null) {
                        AccessibilityUtils.announceForAccessibility(getContext(), getText().toString());
                        speakText();
                        return true;
                    }
                    return false;
                }
        });
    }
    
    private int convertDpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    /**
     * Sets the TextToSpeech engine
     * @param tts TextToSpeech instance
     */
    public void setTextToSpeech(TextToSpeech tts) {
        this.tts = tts;
    }
    
    private void speakText() {
        if (tts != null && getText() != null) {
            isSpeaking = true;
            tts.speak(getText().toString(), TextToSpeech.QUEUE_FLUSH, null, "TextView_" + getId());
            
            // Provide haptic feedback
            AccessibilityUtils.performHapticFeedback(getContext());
        }
    }
    
    private void stopSpeaking() {
        if (tts != null && isSpeaking) {
            tts.stop();
            isSpeaking = false;
            
            // Provide haptic feedback
            AccessibilityUtils.performHapticFeedback(getContext());
        }
    }
    
    @Override
    public void setContentDescription(CharSequence contentDescription) {
        super.setContentDescription(contentDescription);
        
        // Ensure we announce content description changes
        if (isShown() && getVisibility() == View.VISIBLE && contentDescription != null) {
            AccessibilityUtils.announceForAccessibility(getContext(), contentDescription.toString());
        }
    }
    
    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        
        // Update the content description if not set
        if (getContentDescription() == null || getContentDescription().toString().isEmpty()) {
            setContentDescription(text);
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSpeaking();
    }
} 
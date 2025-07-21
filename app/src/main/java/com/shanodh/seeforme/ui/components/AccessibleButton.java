package com.shanodh.seeforme.ui.components;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.shanodh.seeforme.utils.AccessibilityUtils;
import com.shanodh.seeforme.utils.AccessibleGestureDetector;

/**
 * Enhanced Button with accessibility features for visually impaired users
 */
public class AccessibleButton extends MaterialButton {
    
    private TextToSpeech tts;
    private OnClickListener onClickListener;

    public AccessibleButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AccessibleButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AccessibleButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // Set minimum touch target size (48dp x 48dp for accessibility)
        setMinimumHeight(convertDpToPx(48));
        setMinimumWidth(convertDpToPx(100)); // Wider for text
        
        // Apply default accessibility styles
        setTextSize(20); // Larger text
        
        // Setup gesture detection
        setupGestureDetection();
        
        // Set our enhanced click listener
        super.setOnClickListener(v -> {
            // Provide haptic feedback
            AccessibilityUtils.performHapticFeedback(getContext());
            
            // Speak button text if TTS is available
            if (tts != null && !tts.isSpeaking() && getText() != null) {
                String announcement = getContentDescription() != null ? 
                        getContentDescription().toString() : 
                        getText().toString();
                tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "Button_" + getId());
            }
            
            // Call the original click listener
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
    }
    
    private void setupGestureDetection() {
        AccessibleGestureDetector.setupGestures(this, 
            new AccessibleGestureDetector.SimpleOnAccessibleGestureListener() {
                @Override
                public boolean onDoubleTap() {
                    // Double tap to announce what the button does
                    if (getContentDescription() != null) {
                        AccessibilityUtils.announceForAccessibility(
                                getContext(), getContentDescription().toString());
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
    
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        // Store the listener but don't call super to avoid overriding our enhanced listener
        this.onClickListener = l;
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
} 
package com.shanodh.seeforme.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.shanodh.seeforme.R;

/**
 * Reusable loading spinner component with glassmorphism design
 */
public class LoadingSpinnerComponent extends LinearLayout {
    
    private ProgressBar progressBar;
    private TextView loadingText;
    
    public LoadingSpinnerComponent(Context context) {
        super(context);
        init(context);
    }
    
    public LoadingSpinnerComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public LoadingSpinnerComponent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.component_loading_spinner, this, true);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);
    }
    
    /**
     * Set the loading text
     */
    public void setLoadingText(String text) {
        if (loadingText != null) {
            loadingText.setText(text);
        }
    }
    
    /**
     * Show the loading spinner
     */
    public void show() {
        setVisibility(View.VISIBLE);
    }
    
    /**
     * Hide the loading spinner
     */
    public void hide() {
        setVisibility(View.GONE);
    }
    
    /**
     * Check if loading spinner is visible
     */
    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }
    
    /**
     * Show with custom loading text
     */
    public void show(String loadingText) {
        setLoadingText(loadingText);
        show();
    }
}

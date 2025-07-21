package com.shanodh.seeforme.utils;

import android.view.View;
import android.widget.TextView;

/**
 * Utility class for managing loading states across the app
 */
public class LoadingManager {
    
    /**
     * Show loading spinner and hide other views
     */
    public static void showLoading(View loadingSpinner, View... viewsToHide) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        
        for (View view : viewsToHide) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Hide loading spinner and show other views
     */
    public static void hideLoading(View loadingSpinner, View... viewsToShow) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.GONE);
        }
        
        for (View view : viewsToShow) {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Set loading text if the loading component has a text view
     */
    public static void setLoadingText(View loadingSpinner, String text) {
        if (loadingSpinner != null) {
            TextView loadingText = loadingSpinner.findViewById(com.shanodh.seeforme.R.id.loadingText);
            if (loadingText != null) {
                loadingText.setText(text);
            }
        }
    }
    
    /**
     * Show loading with custom text
     */
    public static void showLoadingWithText(View loadingSpinner, String text, View... viewsToHide) {
        setLoadingText(loadingSpinner, text);
        showLoading(loadingSpinner, viewsToHide);
    }
}

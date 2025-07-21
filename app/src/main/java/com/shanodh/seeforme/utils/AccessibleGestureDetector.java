package com.shanodh.seeforme.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Specialized gesture detector for accessibility features
 * This detector handles common accessibility gestures like double tap, 
 * two-finger double tap, and swipe gestures with appropriate callbacks.
 */
public class AccessibleGestureDetector implements View.OnTouchListener {
    
    private final GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public interface OnAccessibleGestureListener {
        boolean onSingleTap();
        boolean onDoubleTap();
        boolean onSwipeRight();
        boolean onSwipeLeft();
        boolean onSwipeUp();
        boolean onSwipeDown();
        boolean onTwoFingerTap();
    }

    public static class SimpleOnAccessibleGestureListener implements OnAccessibleGestureListener {
        @Override
        public boolean onSingleTap() { return false; }
        
        @Override
        public boolean onDoubleTap() { return false; }
        
        @Override
        public boolean onSwipeRight() { return false; }
        
        @Override
        public boolean onSwipeLeft() { return false; }
        
        @Override
        public boolean onSwipeUp() { return false; }
        
        @Override
        public boolean onSwipeDown() { return false; }
        
        @Override
        public boolean onTwoFingerTap() { return false; }
    }

    public AccessibleGestureDetector(Context context, final OnAccessibleGestureListener listener) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return listener.onSingleTap();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return listener.onDoubleTap();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                return listener.onSwipeRight();
                            } else {
                                return listener.onSwipeLeft();
                            }
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                return listener.onSwipeDown();
                            } else {
                                return listener.onSwipeUp();
                            }
                        }
                    }
                } catch (Exception exception) {
                    // Ignore
                }
                return false;
            }
        });
        
        gestureDetector.setIsLongpressEnabled(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Handle two-finger tap
        if (event.getPointerCount() == 2 && event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            return ((OnAccessibleGestureListener) v.getTag()).onTwoFingerTap();
        }
        
        return gestureDetector.onTouchEvent(event);
    }

    /**
     * Sets up the gesture detector on a view
     * @param view The view to attach the detector to
     * @param listener The gesture listener
     */
    public static void setupGestures(View view, OnAccessibleGestureListener listener) {
        view.setTag(listener);
        view.setOnTouchListener(new AccessibleGestureDetector(view.getContext(), listener));
    }
} 
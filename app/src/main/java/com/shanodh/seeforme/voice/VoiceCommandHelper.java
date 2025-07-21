package com.shanodh.seeforme.voice;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.ui.AddFaceActivity;
import com.shanodh.seeforme.ui.AddNoteActivity;
import com.shanodh.seeforme.ui.ViewNotesActivity;

import java.util.ArrayList;
import java.util.Random;

public class VoiceCommandHelper implements SpeechRecognitionHelper.SpeechRecognitionCallback {
    private static final String TAG = "VoiceCommandHelper";
    
    private final Context context;
    private final SpeechRecognitionHelper speechHelper;
    private final TextToSpeechHelper ttsHelper;
    private BottomSheetDialog bottomSheetDialog;
    private TextView tvRecognizedText;
    private LinearProgressIndicator progressIndicator;
    private View[] voiceWaveBars;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final ArrayList<ValueAnimator> waveAnimators = new ArrayList<>();
    
    private NavController navController;
    private VoiceCommandListener commandListener;
    
    public interface VoiceCommandListener {
        void onCommandRecognized(String command);
        void onVoiceUiClosed();
    }
    
    public VoiceCommandHelper(Context context, VoiceCommandListener listener) {
        this.context = context;
        this.commandListener = listener;
        if (context instanceof Activity) {
            this.speechHelper = new SpeechRecognitionHelper((Activity) context, this);
        } else {
            Log.e(TAG, "Context is not an Activity. Speech recognition may not work properly.");
            this.speechHelper = null;
        }
        this.ttsHelper = new TextToSpeechHelper(context, null);
    }
    
    public void setNavController(NavController navController) {
        this.navController = navController;
    }
    
    @SuppressLint("InflateParams")
    public void showVoiceUI() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            return;
        }
        
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity. Cannot show voice UI.");
            return;
        }
        
        // Create bottom sheet dialog
        bottomSheetDialog = new BottomSheetDialog(context);
        View bottomSheetView = ((Activity) context).getLayoutInflater()
                .inflate(R.layout.bottom_sheet_voice_commands, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Get views
        tvRecognizedText = bottomSheetView.findViewById(R.id.tvRecognizedText);
        progressIndicator = bottomSheetView.findViewById(R.id.voiceProgressIndicator);
        MaterialButton btnCancel = bottomSheetView.findViewById(R.id.btnCancelVoice);
        
        // Setup voice wave animation bars
        voiceWaveBars = new View[5];
        voiceWaveBars[0] = bottomSheetView.findViewById(R.id.wave1);
        voiceWaveBars[1] = bottomSheetView.findViewById(R.id.wave2);
        voiceWaveBars[2] = bottomSheetView.findViewById(R.id.wave3);
        voiceWaveBars[3] = bottomSheetView.findViewById(R.id.wave4);
        voiceWaveBars[4] = bottomSheetView.findViewById(R.id.wave5);
        
        // Set cancel button listener
        btnCancel.setOnClickListener(v -> {
            stopListening();
            bottomSheetDialog.dismiss();
            if (commandListener != null) {
                commandListener.onVoiceUiClosed();
            }
        });
        
        // Set dismiss listener
        bottomSheetDialog.setOnDismissListener(dialog -> {
            stopVoiceWaveAnimations();
            if (speechHelper != null) {
                speechHelper.stopListening();
            }
            if (commandListener != null) {
                commandListener.onVoiceUiClosed();
            }
        });
        
        // Set bottom sheet behavior
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        
        // Start voice recognition
        startVoiceRecognition();
        
        // Show the dialog
        bottomSheetDialog.show();
    }
    
    private void startVoiceRecognition() {
        if (speechHelper != null) {
            tvRecognizedText.setText("");
            startVoiceWaveAnimations();
            progressIndicator.setVisibility(View.VISIBLE);
            speechHelper.startListening();
        }
    }
    
    private void stopListening() {
        if (speechHelper != null) {
            speechHelper.stopListening();
        }
        stopVoiceWaveAnimations();
    }
    
    private void startVoiceWaveAnimations() {
        stopVoiceWaveAnimations(); // Clear any existing animations
        
        for (View bar : voiceWaveBars) {
            int originalHeight = bar.getLayoutParams().height;
            int minHeight = originalHeight / 3;
            int maxHeight = originalHeight * 2;
            
            ValueAnimator animator = ValueAnimator.ofInt(minHeight, maxHeight);
            animator.setDuration(1000 + random.nextInt(1000));
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                bar.getLayoutParams().height = value;
                bar.requestLayout();
            });
            
            animator.start();
            waveAnimators.add(animator);
        }
    }
    
    private void stopVoiceWaveAnimations() {
        for (ValueAnimator animator : waveAnimators) {
            animator.cancel();
        }
        waveAnimators.clear();
    }
    
    @Override
    public void onResults(ArrayList<String> results) {
        if (results != null && !results.isEmpty()) {
            String command = results.get(0);
            tvRecognizedText.setText(command);
            
            // Stop animations
            stopVoiceWaveAnimations();
            progressIndicator.setVisibility(View.INVISIBLE);
            
            // Process command
            processVoiceCommand(command);
        }
    }
    
    @Override
    public void onError(int errorCode) {
        tvRecognizedText.setText("Error in speech recognition. Please try again.");
        stopVoiceWaveAnimations();
        progressIndicator.setVisibility(View.INVISIBLE);
    }
    
    @Override
    public void onReadyForSpeech() {
        tvRecognizedText.setText("Listening...");
    }
    
    @Override
    public void onEndOfSpeech() {
        tvRecognizedText.setText("Processing...");
    }
    
    private void processVoiceCommand(String command) {
        Log.d(TAG, "Processing command: " + command);
        final String finalCommand = command.toLowerCase().trim();
        
        // Notify listener
        if (commandListener != null) {
            commandListener.onCommandRecognized(finalCommand);
        }
        
        handler.postDelayed(() -> {
            if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
                bottomSheetDialog.dismiss();
            }
            
            // Execute command
            if (finalCommand.contains("home") || finalCommand.contains("go to home")) {
                if (navController != null) {
                    navController.navigate(R.id.navigation_home);
                    ttsHelper.speak("Going to home screen");
                }
            } else if (finalCommand.contains("assist") || finalCommand.contains("go to assist")) {
                if (navController != null) {
                    navController.navigate(R.id.navigation_assist);
                    ttsHelper.speak("Opening assist mode");
                }
            } else if (finalCommand.contains("profile") || finalCommand.contains("go to profile")) {
                if (navController != null) {
                    navController.navigate(R.id.navigation_profile);
                    ttsHelper.speak("Opening profile settings");
                }
            } else if (finalCommand.contains("add note")) {
                context.startActivity(new Intent(context, AddNoteActivity.class));
                ttsHelper.speak("Opening add note screen");
            } else if (finalCommand.contains("add face")) {
                context.startActivity(new Intent(context, AddFaceActivity.class));
                ttsHelper.speak("Opening add face screen");
            } else if (finalCommand.contains("view notes") || finalCommand.contains("show notes")) {
                context.startActivity(new Intent(context, ViewNotesActivity.class));
                ttsHelper.speak("Opening your notes");
            } else {
                ttsHelper.speak("Command not recognized, please try again");
            }
        }, 1000); // Delay to show the recognized command before dismissing
    }
    
    public void destroy() {
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
        stopVoiceWaveAnimations();
    }
} 
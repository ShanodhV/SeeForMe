package com.shanodh.seeforme.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.MainActivity;

public class AssistFragment extends Fragment {
    private PreviewView viewFinder;
    private TextView detectionResults;
    private TextView statusText;
    private MaterialButton toggleAssistButton;
    private MaterialButton speakButton;
    private FloatingActionButton fabVoiceCommand;
    private boolean isAssisting = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        viewFinder = view.findViewById(R.id.viewFinder);
        detectionResults = view.findViewById(R.id.detectionResults);
        statusText = view.findViewById(R.id.statusText);
        toggleAssistButton = view.findViewById(R.id.toggleAssistButton);
        speakButton = view.findViewById(R.id.speakButton);
        fabVoiceCommand = view.findViewById(R.id.fabVoiceCommand);

        // Setup click listeners
        toggleAssistButton.setOnClickListener(v -> toggleAssistance());
        speakButton.setOnClickListener(v -> 
            ((MainActivity) requireActivity()).startVoiceRecognition());
            
        fabVoiceCommand.setOnClickListener(v -> 
            ((MainActivity) requireActivity()).startVoiceRecognition());

        // Check camera permission
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                statusText.setText("Camera permission is required for object detection");
            }
        }
    }

    private void startCamera() {
        // TODO: Implement camera preview and object detection
        // This will be implemented when we add ML model integration
    }

    private void toggleAssistance() {
        isAssisting = !isAssisting;
        if (isAssisting) {
            toggleAssistButton.setText("Stop Assistance");
            statusText.setText("Detecting objects...");
            // TODO: Start object detection
        } else {
            toggleAssistButton.setText("Start Assistance");
            statusText.setText("Ready to assist");
            detectionResults.setText("No objects detected");
            // TODO: Stop object detection
        }
    }
    
    public void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isAssisting) {
            toggleAssistance();
        }
    }
} 
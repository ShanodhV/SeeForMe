package com.shanodh.seeforme.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shanodh.seeforme.MainActivity;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.ui.AddFaceActivity;
import com.shanodh.seeforme.ui.AddNoteActivity;
import com.shanodh.seeforme.ui.ViewNotesActivity;

public class HomeFragment extends Fragment {
    private TextView tvStatus;
    private FloatingActionButton fabVoiceCommand;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        tvStatus = view.findViewById(R.id.tvStatus);
        fabVoiceCommand = view.findViewById(R.id.fabVoiceCommand);

        // Setup click listeners
        view.findViewById(R.id.cardSpeakCommand).setOnClickListener(v -> {
            ((MainActivity) requireActivity()).performHapticFeedback();
            ((MainActivity) requireActivity()).startVoiceRecognition();
        });

        view.findViewById(R.id.cardViewNotes).setOnClickListener(v -> {
            ((MainActivity) requireActivity()).performHapticFeedback();
            startActivity(new Intent(requireContext(), ViewNotesActivity.class));
        });

        fabVoiceCommand.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).performHapticFeedback();
            ((MainActivity) requireActivity()).startVoiceRecognition();
        });

        enhanceAccessibility(view);
    }

    private void enhanceAccessibility(View view) {
        // Add custom actions for TalkBack
        ViewCompat.replaceAccessibilityAction(
            view.findViewById(R.id.cardSpeakCommand),
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            "Speak a command",
            (v, args) -> {
                ((MainActivity) requireActivity()).performHapticFeedback();
                ((MainActivity) requireActivity()).startVoiceRecognition();
                return true;
            }
        );
        
        ViewCompat.replaceAccessibilityAction(
            view.findViewById(R.id.cardViewNotes),
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            "View your notes",
            (v, args) -> {
                ((MainActivity) requireActivity()).performHapticFeedback();
                startActivity(new Intent(requireContext(), ViewNotesActivity.class));
                return true;
            }
        );
        
        ViewCompat.replaceAccessibilityAction(
            fabVoiceCommand,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            "Speak a command",
            (v, args) -> {
                ((MainActivity) requireActivity()).performHapticFeedback();
                ((MainActivity) requireActivity()).startVoiceRecognition();
                return true;
            }
        );
    }

    public void updateStatus(String status) {
        if (tvStatus != null) {
            tvStatus.setText(status);
        }
    }
} 
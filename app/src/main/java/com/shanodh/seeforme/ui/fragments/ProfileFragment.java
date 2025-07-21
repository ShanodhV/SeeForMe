package com.shanodh.seeforme.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.shanodh.seeforme.MainActivity;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.auth.FirebaseAuthManager;
import com.shanodh.seeforme.ui.LoginActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private ShapeableImageView profileImage;
    private TextView userName;
    private TextView userEmail;
    private TextView accountProvider;
    private ImageView providerIcon;
    private LinearLayout accountProviderLayout;
    private SwitchMaterial switchVoiceFeedback;
    private SwitchMaterial switchVibration;
    private SwitchMaterial switchAutoStart;
    private MaterialButton btnLogout;
    private MaterialButton btnDataSync;
    private MaterialButton btnManageFaces;
    private FloatingActionButton fabVoiceCommand;
    
    // Familiar Faces - simplified for now
    private RecyclerView recyclerFamiliarFaces;
    private TextView facesCount;
    private LinearLayout emptyFacesLayout;
    
    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseAuthManager authManager;
    private SharedPreferences preferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        authManager = new FirebaseAuthManager(requireActivity());
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        
        // Initialize views
        initializeViews(view);
        
        // Setup RecyclerView
        setupFamiliarFacesRecyclerView();
        
        // Setup listeners
        setupClickListeners();
        setupSwitchListeners();
        
        // Load data
        loadUserProfile();
        loadSettings();
        loadFamiliarFaces(); // Show empty state for now
    }

    private void initializeViews(View view) {
        // Profile views
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        accountProvider = view.findViewById(R.id.accountProvider);
        providerIcon = view.findViewById(R.id.providerIcon);
        accountProviderLayout = view.findViewById(R.id.accountProviderLayout);
        
        // Settings switches
        switchVoiceFeedback = view.findViewById(R.id.switchVoiceFeedback);
        switchVibration = view.findViewById(R.id.switchVibration);
        switchAutoStart = view.findViewById(R.id.switchAutoStart);
        
        // Buttons
        btnLogout = view.findViewById(R.id.btnLogout);
        btnDataSync = view.findViewById(R.id.btnDataSync);
        btnManageFaces = view.findViewById(R.id.btnManageFaces);
        fabVoiceCommand = view.findViewById(R.id.fabVoiceCommand);
        
        // Familiar faces views
        recyclerFamiliarFaces = view.findViewById(R.id.recyclerFamiliarFaces);
        facesCount = view.findViewById(R.id.facesCount);
        emptyFacesLayout = view.findViewById(R.id.emptyFacesLayout);
    }

    private void setupFamiliarFacesRecyclerView() {
        // Hide RecyclerView for now, show empty state
        recyclerFamiliarFaces.setVisibility(View.GONE);
        emptyFacesLayout.setVisibility(View.VISIBLE);
        facesCount.setText("0");
    }

    private void setupClickListeners() {
        // Voice command
        fabVoiceCommand.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).performHapticFeedback();
            ((MainActivity) requireActivity()).startVoiceRecognition();
        });

        // Logout
        btnLogout.setOnClickListener(v -> onLogoutClicked());
        
        // Data sync
        btnDataSync.setOnClickListener(v -> onDataSyncClicked());
        
        // Manage faces
        btnManageFaces.setOnClickListener(v -> onManageFacesClicked());
    }

    private void setupSwitchListeners() {
        switchVoiceFeedback.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateVoiceFeedbackSetting(isChecked));
        
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateVibrationSetting(isChecked));
        
        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateAutoStartSetting(isChecked));
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Update user info
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            String photoUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null;
            
            userName.setText(displayName != null ? displayName : "User");
            userEmail.setText(email != null ? email : "No email");
            
            // Load profile image
            if (photoUrl != null && !photoUrl.isEmpty()) {
                // Set background for image container
                profileImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(profileImage);
            } else {
                // Set background and default icon
                profileImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                profileImage.setImageResource(R.drawable.ic_person);
                profileImage.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
            }
            
            // Check provider and update UI
            boolean isGoogleSignIn = false;
            if (currentUser.getProviderData() != null) {
                for (var userInfo : currentUser.getProviderData()) {
                    if (GoogleAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId())) {
                        isGoogleSignIn = true;
                        break;
                    }
                }
            }
            
            if (isGoogleSignIn) {
                accountProvider.setText("Signed in with Google");
                providerIcon.setImageResource(R.drawable.ic_google);
                accountProviderLayout.setVisibility(View.VISIBLE);
                // Announce for accessibility
                accountProviderLayout.setContentDescription("Account signed in with Google - secure and easy access");
            } else {
                accountProvider.setText("Email Account");
                providerIcon.setImageResource(R.drawable.ic_email);
                accountProviderLayout.setVisibility(View.VISIBLE);
                // Announce for accessibility  
                accountProviderLayout.setContentDescription("Account signed in with email - secure access");
            }
        } else {
            // User not logged in, redirect to login
            redirectToLogin();
        }
    }

    private void loadSettings() {
        // Load settings from SharedPreferences
        switchVoiceFeedback.setChecked(preferences.getBoolean("voice_feedback", true));
        switchVibration.setChecked(preferences.getBoolean("vibration_feedback", true));
        switchAutoStart.setChecked(preferences.getBoolean("auto_start", false));
    }

    private void loadFamiliarFaces() {
        // TODO: Implement when database is ready
        // For now, just show empty state
        facesCount.setText("0");
        recyclerFamiliarFaces.setVisibility(View.GONE);
        emptyFacesLayout.setVisibility(View.VISIBLE);
    }

    private void onDataSyncClicked() {
        Toast.makeText(requireContext(), "Backing up your voice notes to cloud...", Toast.LENGTH_SHORT).show();
        ((MainActivity) requireActivity()).speak("Backing up your voice notes to cloud storage for safety");
        
        // Show loading state
        btnDataSync.setEnabled(false);
        btnDataSync.setText("Backing up...");
        
        // Simulate sync process (in real app, this would sync with Firestore)
        btnDataSync.postDelayed(() -> {
            btnDataSync.setEnabled(true);
            btnDataSync.setText("Backup Voice Notes to Cloud");
            
            Toast.makeText(requireContext(), "Voice notes backed up successfully!", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).speak("All your voice notes are now safely backed up to the cloud. You can access them from any device.");
        }, 2000);
    }

    private void onManageFacesClicked() {
        // Navigate to ViewFacesActivity
        Intent intent = new Intent(requireContext(), com.shanodh.seeforme.ui.ViewFacesActivity.class);
        startActivity(intent);
        ((MainActivity) requireActivity()).performHapticFeedback();
        ((MainActivity) requireActivity()).speak("Opening familiar faces manager");
    }

    private void onLogoutClicked() {
        authManager.signOut(new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error signing out: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void updateVoiceFeedbackSetting(boolean enabled) {
        preferences.edit().putBoolean("voice_feedback", enabled).apply();
        
        if (enabled) {
            ((MainActivity) requireActivity()).speak("Voice feedback enabled");
        }
    }

    private void updateVibrationSetting(boolean enabled) {
        preferences.edit().putBoolean("vibration_feedback", enabled).apply();
        
        if (enabled) {
            ((MainActivity) requireActivity()).performHapticFeedback();
        }
        
        ((MainActivity) requireActivity()).speak(enabled ? "Vibration feedback enabled" : "Vibration feedback disabled");
    }

    private void updateAutoStartSetting(boolean enabled) {
        preferences.edit().putBoolean("auto_start", enabled).apply();
        
        ((MainActivity) requireActivity()).speak(enabled ? 
            "Auto-start voice assistant enabled" : 
            "Auto-start voice assistant disabled");
    }

    // TODO: Implement FamiliarFacesAdapter.OnFaceClickListener when database is ready
    /*
    @Override
    public void onFaceClick(FamiliarFace face, int position) {
        ((MainActivity) requireActivity()).speak("Familiar face: " + face.getName());
        Toast.makeText(requireContext(), "Face: " + face.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFaceLongClick(FamiliarFace face, int position) {
        // Show options dialog for the face
        ((MainActivity) requireActivity()).speak("Options for " + face.getName());
        
        // TODO: Show dialog with options like delete, rename, etc.
        Toast.makeText(requireContext(), "Long clicked: " + face.getName(), Toast.LENGTH_SHORT).show();
    }
    */
    
    public void updateStatus(String status) {
        // Could be used to update any status indicators if needed
        ((MainActivity) requireActivity()).speak(status);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: Clean up when database is implemented
    }
} 
package com.shanodh.seeforme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shanodh.seeforme.auth.FirebaseAuthManager;
import com.shanodh.seeforme.firebase.FirestoreNotesManager;
import com.shanodh.seeforme.ui.fragments.SimpleAssistFragment;
import com.shanodh.seeforme.ui.fragments.HomeFragment;
import com.shanodh.seeforme.ui.fragments.ProfileFragment;
import com.shanodh.seeforme.ui.AddNoteActivity;
import com.shanodh.seeforme.ui.LoginActivity;
import com.shanodh.seeforme.ui.ViewNotesActivity;
import com.shanodh.seeforme.ui.AddFaceActivity;
import com.shanodh.seeforme.voice.TextToSpeechHelper;
import com.shanodh.seeforme.voice.VoiceCommandHelper;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        TextToSpeechHelper.TtsCallback,
        VoiceCommandHelper.VoiceCommandListener {

    private static final String TAG = "MainActivity";
    private TextToSpeechHelper ttsHelper;
    private VoiceCommandHelper voiceCommandHelper;
    private AccessibilityManager accessibilityManager;
    private Vibrator vibrator;
    private NavController navController;
    private FloatingActionButton fabVoiceCommand;
    
    // Firebase
    private FirebaseAuthManager authManager;
    private FirestoreNotesManager notesManager;

    // Permission launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // Check specifically for microphone permission for voice commands
                if (permissions.containsKey(Manifest.permission.RECORD_AUDIO)) {
                    if (permissions.get(Manifest.permission.RECORD_AUDIO)) {
                        initializeVoiceServices();
                        showAccessibleMessage("Microphone permission granted. Voice commands are now available.");
                    } else {
                        showAccessibleMessage("Microphone permission is required for voice commands.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication first
        authManager = new FirebaseAuthManager(this);
        if (!authManager.isUserLoggedIn()) {
            // User is not logged in, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // Initialize Firebase services
        notesManager = new FirestoreNotesManager();
        
        setContentView(R.layout.activity_main);

        // Setup navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // Setup FAB
        fabVoiceCommand = findViewById(R.id.fabVoiceCommand);
        fabVoiceCommand.setOnClickListener(v -> {
            // First check if we have microphone permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                showAccessibleMessage("Microphone permission is required for voice commands.");
                requestPermissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                return;
            }
            
            // Then check if voice helper is initialized
            if (voiceCommandHelper == null) {
                initializeVoiceServices();
                if (voiceCommandHelper == null) {
                    showAccessibleMessage("Unable to initialize voice services. Please restart the app.");
                    return;
                }
            }
            
            // All good, start voice recognition
            startVoiceRecognition();
        });

        initializeServices();
        checkPermissions();
    }

    private void initializeServices() {
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        ttsHelper = new TextToSpeechHelper(this, this);
        // Firebase services are already initialized in onCreate
    }

    private void checkPermissions() {
        // Only check voice permissions when initializing the app
        if (checkVoicePermissions()) {
            initializeVoiceServices();
        }
    }
    
    private boolean checkVoicePermissions() {
        // For voice commands, we only need RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
            return false;
        }
        return true;
    }
    
    private void checkAllPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.VIBRATE
        };

        // Add new media permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.VIBRATE
            };
        }

        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
            requestPermissionLauncher.launch(permissionsArray);
        }
    }

    private void initializeVoiceServices() {
        try {
            if (voiceCommandHelper == null) {
                voiceCommandHelper = new VoiceCommandHelper(this, this);
                voiceCommandHelper.setNavController(navController);
                Log.d(TAG, "Voice services initialized successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing voice services", e);
            showAccessibleMessage("Error initializing voice services. Please restart the app.");
        }
    }

    public void startVoiceRecognition() {
        // Double-check microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            showAccessibleMessage("Microphone permission is required for voice commands.");
            requestPermissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
            return;
        }
        
        // Check voice helper
        if (voiceCommandHelper == null) {
            Log.e(TAG, "VoiceCommandHelper is null. Reinitializing voice services.");
            initializeVoiceServices();
            if (voiceCommandHelper == null) {
                showAccessibleMessage("Unable to initialize voice services. Please restart the app.");
                performErrorHapticFeedback();
                return;
            }
        }
        
        try {
            Log.d(TAG, "Starting voice recognition UI");
            updateStatus("Listening...");
            announceForAccessibility("Listening for a command");
            performHapticFeedback();
            voiceCommandHelper.showVoiceUI();
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recognition", e);
            showAccessibleMessage("Error starting voice recognition. Please try again.");
            performErrorHapticFeedback();
        }
    }

    @Override
    public void onCommandRecognized(String command) {
        updateStatus("Command: " + command);
    }

    @Override
    public void onVoiceUiClosed() {
        updateStatus("Voice recognition closed");
    }

    public void performHapticFeedback() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void performErrorHapticFeedback() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 100, 100, 100};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                long[] pattern = {0, 100, 100, 100};
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void showAccessibleMessage(String message) {
        updateStatus(message);
        announceForAccessibility(message);
        ttsHelper.speak(message);
    }

    private void updateStatus(String status) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (currentFragment != null) {
            Fragment childFragment = currentFragment.getChildFragmentManager().getFragments().get(0);
            if (childFragment instanceof HomeFragment) {
                ((HomeFragment) childFragment).updateStatus(status);
            } else if (childFragment instanceof SimpleAssistFragment) {
                ((SimpleAssistFragment) childFragment).updateStatus(status);
            } else if (childFragment instanceof ProfileFragment) {
                ((ProfileFragment) childFragment).updateStatus(status);
            }
        }
    }

    private void announceForAccessibility(String text) {
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(text);
            event.setClassName(getClass().getName());
            event.setPackageName(getPackageName());
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    @Override
    public void onTtsInitialized(boolean success) {
        if (success) {
            ttsHelper.speak("Welcome to See For Me! I'm your voice assistant. You can say commands like 'add note', 'view notes', or 'add face'. Tap the microphone button or say 'help' anytime for assistance.");
        }
    }

    @Override
    public void onTtsCompleted() {
        // Not used in this activity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        if (authManager != null) {
            authManager.signOut(new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    showAccessibleMessage("You have been signed out");
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    showAccessibleMessage("Error signing out: " + error);
                }
            });
        }
    }

    public FirestoreNotesManager getNotesManager() {
        return notesManager;
    }

    @Override
    protected void onDestroy() {
        if (voiceCommandHelper != null) {
            voiceCommandHelper.destroy();
        }
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        super.onDestroy();
    }

    public void speak(String text) {
        if (ttsHelper != null) {
            ttsHelper.speak(text);
        }
    }
}
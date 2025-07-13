package com.shanodh.seeforme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.shanodh.seeforme.MainActivity;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.auth.FirebaseAuthManager;
import com.shanodh.seeforme.voice.TextToSpeechHelper;

/**
 * Login Activity with Firebase Authentication
 */
public class LoginActivity extends AppCompatActivity implements TextToSpeechHelper.TtsCallback {
    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvCreateAccount;
    private ProgressBar progressBar;
    
    private FirebaseAuthManager authManager;
    private TextToSpeechHelper ttsHelper;

    // Activity result launcher for Google Sign In
    private ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    authManager.handleGoogleSignInResult(result.getData(), new FirebaseAuthManager.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            hideProgressBar();
                            String welcomeMessage = "Welcome " + (user.getDisplayName() != null ? user.getDisplayName() : user.getEmail()) + "! Your account is now securely connected.";
                            showMessage(welcomeMessage);
                            navigateToMain();
                        }

                        @Override
                        public void onError(String error) {
                            hideProgressBar();
                            showMessage("Google sign in failed: " + error + ". You can try again or use email sign-in instead.");
                        }
                    });
                } else {
                    hideProgressBar();
                    showMessage("Google sign in was cancelled. You can try again or use email sign-in.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_login);

        initializeViews();
        initializeServices();
        setupClickListeners();
        
        // Check if user is already logged in
        if (authManager.isUserLoggedIn()) {
            navigateToMain();
        }
        
        // Announce welcome message for accessibility and emphasize Google Sign-In
        announceWelcomeMessage();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initializeServices() {
        authManager = new FirebaseAuthManager(this);
        ttsHelper = new TextToSpeechHelper(this, this);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginWithEmail());
        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        showProgressBar();
        authManager.signInWithEmail(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                hideProgressBar();
                showMessage("Welcome back!");
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                hideProgressBar();
                showMessage("Login failed: " + error);
            }
        });
    }

    private void signInWithGoogle() {
        showProgressBar();
        showMessage("Opening Google Sign-In...");
        Intent signInIntent = authManager.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void announceWelcomeMessage() {
        if (ttsHelper != null) {
            ttsHelper.speak("Welcome to See For Me! Your AI-powered visual assistant. We recommend signing in with Google for the fastest and most secure experience. You can also create an account with email and password if preferred.");
        }
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            showMessage("Please enter your email");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            showMessage("Please enter a valid email address");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            showMessage("Please enter your password");
            return false;
        }

        return true;
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnGoogleSignIn.setEnabled(false);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        btnGoogleSignIn.setEnabled(true);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        // Also announce for accessibility
        if (ttsHelper != null) {
            ttsHelper.speak(message);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTtsInitialized(boolean success) {
        if (success) {
            ttsHelper.speak("Welcome to See For Me. Please sign in or create an account to continue.");
        }
    }

    @Override
    public void onTtsCompleted() {
        // Not used
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsHelper != null) {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        }
    }
}

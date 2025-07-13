package com.shanodh.seeforme.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.models.FaceData;
import com.shanodh.seeforme.voice.TextToSpeechHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddFaceActivity extends AppCompatActivity implements TextToSpeechHelper.TtsCallback {

    private static final String TAG = "AddFaceActivity";
    private ImageView ivPreview;
    private TextInputEditText etPersonName;
    private AutoCompleteTextView etRelationship;
    private MaterialButton btnSaveFace, btnTakePhoto, btnSelectFromGallery;
    private MaterialCardView cardCapturePhoto, cardPreview;
    private Bitmap capturedImage;
    private TextToSpeechHelper ttsHelper;
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    // Camera permission launcher
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos. You can still select from gallery.", Toast.LENGTH_LONG).show();
                }
            }
    );
    
    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        // Get the thumbnail bitmap
                        capturedImage = (Bitmap) extras.get("data");
                        if (capturedImage != null) {
                            displayCapturedImage();
                        }
                    }
                }
            }
    );
    
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                            capturedImage = BitmapFactory.decodeStream(inputStream);
                            if (inputStream != null) {
                                inputStream.close();
                            }
                            if (capturedImage != null) {
                                displayCapturedImage();
                            } else {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_add_face);
        
        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Familiar Face");
        }
        
        initializeViews();
        setupDropdown();
        setupClickListeners();
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add faces", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        ttsHelper = new TextToSpeechHelper(this, this);
    }
    
    private void initializeViews() {
        ivPreview = findViewById(R.id.ivPreview);
        etPersonName = findViewById(R.id.etPersonName);
        etRelationship = findViewById(R.id.etRelationship);
        btnSaveFace = findViewById(R.id.btnSaveFace);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSelectFromGallery = findViewById(R.id.btnSelectFromGallery);
        cardCapturePhoto = findViewById(R.id.cardCapturePhoto);
        cardPreview = findViewById(R.id.cardPreview);
    }
    
    private void setupDropdown() {
        // Setup relationship dropdown
        String[] relationships = {"Father", "Mother", "Brother", "Sister", "Wife", "Child", "Friend"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, relationships);
        etRelationship.setAdapter(adapter);
        etRelationship.setOnClickListener(v -> etRelationship.showDropDown());
    }
    
    private void setupClickListeners() {
        btnTakePhoto.setOnClickListener(v -> {
            performHapticFeedback();
            captureImage();
        });
        
        btnSelectFromGallery.setOnClickListener(v -> {
            performHapticFeedback();
            selectFromGallery();
        });
        
        btnSaveFace.setOnClickListener(v -> {
            performHapticFeedback();
            saveFace();
        });
    }
    
    private void captureImage() {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            // Request camera permission
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    
    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void selectFromGallery() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        if (pickImageIntent.resolveActivity(getPackageManager()) != null) {
            pickImageLauncher.launch(pickImageIntent);
        } else {
            Toast.makeText(this, "No gallery app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayCapturedImage() {
        ivPreview.setImageBitmap(capturedImage);
        cardPreview.setVisibility(View.VISIBLE);
    }
    
    private void saveFace() {
        String personName = etPersonName.getText() != null ? etPersonName.getText().toString().trim() : "";
        String relationship = etRelationship.getText() != null ? etRelationship.getText().toString().trim() : "";
        
        if (personName.isEmpty()) {
            Toast.makeText(this, "Please provide a name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (capturedImage == null) {
            Toast.makeText(this, "Please capture or select an image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to save faces", Toast.LENGTH_SHORT).show();
            android.util.Log.e(TAG, "User not authenticated");
            return;
        }
        
        android.util.Log.d(TAG, "Saving face for user: " + currentUser.getUid());
        
        // Disable save button to prevent multiple saves
        btnSaveFace.setEnabled(false);
        btnSaveFace.setText("Saving...");
        
        // Generate unique ID for this face
        String faceId = UUID.randomUUID().toString();
        
        // Save image locally first
        executor.execute(() -> {
            try {
                String localImagePath = saveImageLocally(capturedImage, faceId);
                if (localImagePath != null) {
                    // Save face data to Firebase
                    saveFaceToFirebase(faceId, personName, relationship, localImagePath);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving face: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                });
            }
        });
    }
    
    private String saveImageLocally(Bitmap bitmap, String faceId) {
        try {
            // Create faces directory in app's internal storage
            File facesDir = new File(getFilesDir(), "faces");
            if (!facesDir.exists()) {
                facesDir.mkdirs();
            }
            
            // Create file with unique name
            File imageFile = new File(facesDir, faceId + ".jpg");
            
            // Save bitmap to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void saveFaceToFirebase(String faceId, String personName, String relationship, String localImagePath) {
        FaceData faceData = new FaceData(faceId, personName, relationship, localImagePath, currentUser.getUid());
        
        // Log for debugging
        android.util.Log.d(TAG, "Saving face to Firebase: " + personName + " for user: " + currentUser.getUid());
        android.util.Log.d(TAG, "FaceData - ID: " + faceData.getId());
        android.util.Log.d(TAG, "FaceData - Name: " + faceData.getPersonName());
        android.util.Log.d(TAG, "FaceData - Relationship: " + faceData.getRelationship());
        android.util.Log.d(TAG, "FaceData - ImagePath: " + faceData.getLocalImagePath());
        android.util.Log.d(TAG, "FaceData - UserID: " + faceData.getUserId());
        android.util.Log.d(TAG, "FaceData - Timestamp: " + faceData.getTimestamp());
        
        db.collection("faces")
                .document(faceId)
                .set(faceData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "Face saved successfully to Firebase");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Face saved successfully: " + personName, Toast.LENGTH_SHORT).show();
                        if (ttsHelper != null) {
                            ttsHelper.speak("Face saved for " + personName);
                        }
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to save face to Firebase", e);
                    runOnUiThread(() -> {
                        String errorMsg = "Failed to save to database";
                        if (e.getMessage() != null) {
                            errorMsg += ": " + e.getMessage();
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        resetSaveButton();
                    });
                });
    }
    
    private void resetSaveButton() {
        btnSaveFace.setEnabled(true);
        btnSaveFace.setText("Save Face");
    }

    private void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            performHapticFeedback();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTtsInitialized(boolean success) {
        if (success) {
            ttsHelper.speak("Add a familiar face. Please select an image and provide details.");
        }
    }

    @Override
    public void onTtsCompleted() {
        // Not used in this activity
    }
    
    @Override
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        
        super.onDestroy();
    }
}
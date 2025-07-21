package com.shanodh.seeforme;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddFaceActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private MaterialCardView cardPreview;
    private TextInputEditText etPersonName;
    private AutoCompleteTextView etRelationship;
    private MaterialButton btnSaveFace;
    private MaterialCardView cardCapturePhoto;
    private Bitmap capturedImage;
    
    // Predefined relationship options
    private final String[] relationships = {
        "Father", "Mother", "Brother", "Sister", "Friend", "Wife", "Child"
    };
    
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
                            capturedImage = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), selectedImageUri);
                            displayCapturedImage();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
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
        setupClickListeners();
    }
    
    private void initializeViews() {
        ivPreview = findViewById(R.id.ivPreview);
        cardPreview = findViewById(R.id.cardPreview);
        etPersonName = findViewById(R.id.etPersonName);
        etRelationship = findViewById(R.id.etRelationship);
        btnSaveFace = findViewById(R.id.btnSaveFace);
        cardCapturePhoto = findViewById(R.id.cardCapturePhoto);
        
        setupRelationshipDropdown();
    }
    
    private void setupRelationshipDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, relationships);
        etRelationship.setAdapter(adapter);
        etRelationship.setOnClickListener(v -> etRelationship.showDropDown());
    }
    
    private void setupClickListeners() {
        cardCapturePhoto.setOnClickListener(v -> {
            performHapticFeedback();
            captureImage();
        });
        
        btnSaveFace.setOnClickListener(v -> {
            performHapticFeedback();
            saveFace();
        });
    }
    
    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayCapturedImage() {
        ivPreview.setImageBitmap(capturedImage);
        cardPreview.setVisibility(MaterialCardView.VISIBLE);
    }
    
    private void saveFace() {
        String personName = etPersonName.getText() != null ? etPersonName.getText().toString().trim() : "";
        String relationship = etRelationship.getText() != null ? etRelationship.getText().toString().trim() : "";
        
        if (personName.isEmpty()) {
            Toast.makeText(this, "Please enter a person's name", Toast.LENGTH_SHORT).show();
            etPersonName.requestFocus();
            return;
        }
        
        if (relationship.isEmpty()) {
            Toast.makeText(this, "Please select a relationship", Toast.LENGTH_SHORT).show();
            etRelationship.showDropDown();
            return;
        }
        
        if (capturedImage == null) {
            Toast.makeText(this, "Please capture an image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save face image to device storage
        if (saveFaceImageToStorage(personName, relationship)) {
            Toast.makeText(this, "Face saved successfully: " + personName + " (" + relationship + ")", 
                Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save face image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean saveFaceImageToStorage(String personName, String relationship) {
        try {
            // Create faces directory in app's internal storage
            File facesDir = new File(getFilesDir(), "faces");
            if (!facesDir.exists()) {
                facesDir.mkdirs();
            }
            
            // Create filename with timestamp to avoid conflicts
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = personName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".jpg";
            File imageFile = new File(facesDir, filename);
            
            // Save bitmap to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            capturedImage.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            
            // Save metadata (you might want to use a database for this in a real app)
            saveFaceMetadata(filename, personName, relationship);
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void saveFaceMetadata(String filename, String personName, String relationship) {
        // For now, we'll just use SharedPreferences to store basic metadata
        // In a real app, you'd use a proper database
        android.content.SharedPreferences prefs = getSharedPreferences("faces_metadata", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        // Store metadata with filename as key
        editor.putString(filename + "_name", personName);
        editor.putString(filename + "_relationship", relationship);
        editor.putLong(filename + "_timestamp", System.currentTimeMillis());
        editor.apply();
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
} 
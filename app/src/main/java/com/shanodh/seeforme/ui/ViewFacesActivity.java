package com.shanodh.seeforme.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.models.FaceData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewFacesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFaces;
    private TextView tvEmptyState;
    private TextView tvFaceCount;
    private MaterialButton btnAddFace;
    private View loadingSpinner;
    private FacesAdapter facesAdapter;
    private List<FaceItem> facesList;
    
    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_view_faces);

        // Setup toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Familiar Faces");
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        loadFaces();
    }

    private void initializeViews() {
        recyclerViewFaces = findViewById(R.id.recyclerViewFaces);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvFaceCount = findViewById(R.id.tvFaceCount);
        btnAddFace = findViewById(R.id.btnAddFace);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        
        // Update loading text for faces
        TextView loadingText = loadingSpinner.findViewById(R.id.loadingText);
        if (loadingText != null) {
            loadingText.setText("Loading faces...");
        }
        
        facesList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        facesAdapter = new FacesAdapter(facesList, new FacesAdapter.OnFaceActionListener() {
            @Override
            public void onDeleteFace(FaceItem face, int position) {
                showDeleteConfirmationDialog(face, position);
            }

            @Override
            public void onFaceClick(FaceItem face) {
                performHapticFeedback();
                Toast.makeText(ViewFacesActivity.this, 
                    face.getName() + " (" + face.getRelationship() + ")", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        recyclerViewFaces.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFaces.setAdapter(facesAdapter);
    }

    private void setupClickListeners() {
        btnAddFace.setOnClickListener(v -> {
            performHapticFeedback();
            startActivity(new Intent(this, AddFaceActivity.class));
        });
    }

    private void loadFaces() {
        if (currentUser == null) {
            showEmptyState();
            Toast.makeText(this, "Please log in to view your faces", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading spinner
        showLoadingState();
        
        // Clear the list to prevent duplication
        facesList.clear();
        if (facesAdapter != null) {
            facesAdapter.notifyDataSetChanged();
        }
        
        // Log for debugging
        android.util.Log.d("ViewFacesActivity", "Loading faces for user: " + currentUser.getUid());
        
        // Query Firebase for faces belonging to current user
        db.collection("faces")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("ViewFacesActivity", "Query successful, found " + queryDocumentSnapshots.size() + " documents");
                    
                    // Clear list again to ensure no duplication
                    facesList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            FaceData faceData = document.toObject(FaceData.class);
                            android.util.Log.d("ViewFacesActivity", "Processing face: " + faceData.getPersonName());
                            
                            // Load image from local path
                            Bitmap bitmap = loadImageFromPath(faceData.getLocalImagePath());
                            if (bitmap != null) {
                                FaceItem faceItem = new FaceItem(
                                    faceData.getId(),
                                    faceData.getPersonName(),
                                    faceData.getRelationship() != null ? faceData.getRelationship() : "Unknown",
                                    faceData.getTimestamp(),
                                    bitmap
                                );
                                facesList.add(faceItem);
                            } else {
                                android.util.Log.w("ViewFacesActivity", "Failed to load image for: " + faceData.getPersonName());
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ViewFacesActivity", "Error processing document: " + document.getId(), e);
                        }
                    }
                    
                    // Sort by timestamp (newest first) in code instead of Firestore
                    facesList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    
                    runOnUiThread(() -> {
                        hideLoadingState();
                        updateFaceCount();
                        if (facesList.isEmpty()) {
                            showEmptyState();
                        } else {
                            showFacesList();
                        }
                        facesAdapter.notifyDataSetChanged();
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ViewFacesActivity", "Failed to load faces from Firebase", e);
                    runOnUiThread(() -> {
                        hideLoadingState();
                        updateFaceCount(); // Update count even on failure
                        String errorMsg = "Failed to load faces";
                        if (e.getMessage() != null) {
                            errorMsg += ": " + e.getMessage();
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        showEmptyState();
                    });
                });
    }
    
    private Bitmap loadImageFromPath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // Reduce size for thumbnail
                return BitmapFactory.decodeFile(imagePath, options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void showLoadingState() {
        loadingSpinner.setVisibility(View.VISIBLE);
        recyclerViewFaces.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvFaceCount.setVisibility(View.GONE);
    }
    
    private void hideLoadingState() {
        loadingSpinner.setVisibility(View.GONE);
    }
    
    private void updateFaceCount() {
        int count = facesList.size();
        String countText = count == 1 ? "1 face saved" : count + " faces saved";
        tvFaceCount.setText(countText);
        tvFaceCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState() {
        recyclerViewFaces.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvFaceCount.setVisibility(View.GONE);
    }

    private void showFacesList() {
        recyclerViewFaces.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        tvFaceCount.setVisibility(View.VISIBLE);
    }

    private void showDeleteConfirmationDialog(FaceItem face, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Face")
                .setMessage("Are you sure you want to delete " + face.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteFace(face, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFace(FaceItem face, int position) {
        // Delete image file
        File facesDir = new File(getFilesDir(), "faces");
        File imageFile = new File(facesDir, face.getFilename());
        if (imageFile.exists()) {
            imageFile.delete();
        }

        // Delete metadata
        SharedPreferences prefs = getSharedPreferences("faces_metadata", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(face.getFilename() + "_name");
        editor.remove(face.getFilename() + "_relationship");
        editor.remove(face.getFilename() + "_timestamp");
        editor.apply();

        // Remove from list and update UI
        facesList.remove(position);
        facesAdapter.notifyItemRemoved(position);

        if (facesList.isEmpty()) {
            showEmptyState();
        }

        performHapticFeedback();
        Toast.makeText(this, face.getName() + " deleted", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();
        // Reload faces when returning to this activity
        loadFaces();
    }

    // Data class for face items
    public static class FaceItem {
        private String filename;
        private String name;
        private String relationship;
        private long timestamp;
        private Bitmap bitmap;

        public FaceItem(String filename, String name, String relationship, long timestamp, Bitmap bitmap) {
            this.filename = filename;
            this.name = name;
            this.relationship = relationship;
            this.timestamp = timestamp;
            this.bitmap = bitmap;
        }

        // Getters
        public String getFilename() { return filename; }
        public String getName() { return name; }
        public String getRelationship() { return relationship; }
        public long getTimestamp() { return timestamp; }
        public Bitmap getBitmap() { return bitmap; }

        public String getFormattedDate() {
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}

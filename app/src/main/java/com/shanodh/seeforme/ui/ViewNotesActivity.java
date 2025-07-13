package com.shanodh.seeforme.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shanodh.seeforme.R;
import com.shanodh.seeforme.data.Note;
import com.shanodh.seeforme.firebase.FirestoreNotesManager;
import com.shanodh.seeforme.voice.TextToSpeechHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ViewNotesActivity extends AppCompatActivity implements TextToSpeechHelper.TtsCallback {

    private RecyclerView rvNotes;
    private TextView tvEmptyNotes;
    private Button btnAddNote;
    private View loadingSpinner;
    private NotesAdapter adapter;
    private List<Note> notesList = new ArrayList<>();
    private TextToSpeechHelper ttsHelper;
    private FirestoreNotesManager notesManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_view_notes);
        
        // Setup the toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Your Notes");
        }
        
        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notes_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initializeViews();
        setupRecyclerView();
        
        // Initialize Firebase
        notesManager = new FirestoreNotesManager();
        loadNotes();
        
        ttsHelper = new TextToSpeechHelper(this, this);
        
        btnAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(ViewNotesActivity.this, AddNoteActivity.class);
            startActivity(intent);
        });
        
        // Initialize accessibility features
        initializeAccessibilityFeatures();
    }
    
    private void initializeViews() {
        rvNotes = findViewById(R.id.recyclerViewNotes);
        tvEmptyNotes = findViewById(R.id.tvEmptyNotes);
        btnAddNote = findViewById(R.id.btnAddNote);
        loadingSpinner = findViewById(R.id.loadingSpinner);
    }
    
    private void setupRecyclerView() {
        adapter = new NotesAdapter(notesList);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(adapter);
    }
    
    private void loadNotes() {
        // Show loading spinner
        showLoading(true);
        
        notesManager.getUserNotes(new FirestoreNotesManager.FirestoreCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> notes) {
                // Hide loading spinner
                showLoading(false);
                
                notesList.clear();
                if (notes != null) {
                    notesList.addAll(notes);
                    updateEmptyState();
                    if (notes.isEmpty()) {
                        ttsHelper.speak("You don't have any notes yet. Tap the Add New Note button to create one.");
                    } else {
                        ttsHelper.speak("Showing " + notes.size() + " notes. Swipe to browse through them.");
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                // Hide loading spinner
                showLoading(false);
                
                Toast.makeText(ViewNotesActivity.this, "Error loading notes: " + error, Toast.LENGTH_SHORT).show();
                ttsHelper.speak("Error loading notes");
                updateEmptyState();
            }
        });
    }
    
    private void showLoading(boolean show) {
        if (show) {
            loadingSpinner.setVisibility(View.VISIBLE);
            rvNotes.setVisibility(View.GONE);
            tvEmptyNotes.setVisibility(View.GONE);
        } else {
            loadingSpinner.setVisibility(View.GONE);
            updateEmptyState();
        }
    }
    
    private void updateEmptyState() {
        if (notesList.isEmpty()) {
            tvEmptyNotes.setVisibility(View.VISIBLE);
            rvNotes.setVisibility(View.GONE);
        } else {
            tvEmptyNotes.setVisibility(View.GONE);
            rvNotes.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onTtsInitialized(boolean success) {
        if (success) {
            ttsHelper.speak("Your notes for visual recognition.");
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
    
    private void initializeAccessibilityFeatures() {
        // Enhance UI elements with accessibility features
        RecyclerView notesRecyclerView = findViewById(R.id.recyclerViewNotes);
        notesRecyclerView.setContentDescription("List of your saved notes");
        
        // Add a touch helper for swipe actions with haptic feedback
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.RIGHT) {
                    // Delete action with haptic feedback
                    performHapticFeedback();
                    // ... delete note logic ...
                    announceForAccessibility("Note deleted");
                } else {
                    // Edit action with haptic feedback
                    performHapticFeedback();
                    // ... edit note logic ...
                    announceForAccessibility("Editing note");
                }
            }
        };
        
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(notesRecyclerView);
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

    private void announceForAccessibility(String text) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(text);
            event.setClassName(getClass().getName());
            event.setPackageName(getPackageName());
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notes when returning from AddNoteActivity
        loadNotes();
    }
    
    private void editNote(Note note) {
        Intent intent = new Intent(ViewNotesActivity.this, AddNoteActivity.class);
        intent.putExtra(AddNoteActivity.EXTRA_NOTE_ID, note.getFirestoreId());
        intent.putExtra(AddNoteActivity.EXTRA_NOTE_TITLE, note.getTitle());
        intent.putExtra(AddNoteActivity.EXTRA_NOTE_CONTENT, note.getContent());
        startActivity(intent);
        ttsHelper.speak("Opening note for editing");
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
    
    // RecyclerView Adapter
    class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
        
        private final List<Note> notes;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        
        NotesAdapter(List<Note> notes) {
            this.notes = notes;
        }
        
        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
            return new NoteViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.tvNoteContent.setText(note.getTitle() != null && !note.getTitle().trim().isEmpty() ? 
                note.getTitle() : note.getContent());
            
            String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
            String status = "Created: " + formattedDate;
            holder.tvNoteStatus.setText(status);
            
            holder.btnEdit.setOnClickListener(v -> {
                performHapticFeedback();
                editNote(note);
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                performHapticFeedback();
                ttsHelper.speak("Deleting note");
                deleteNote(note, position);
            });
            
            // Make the entire item accessible via TalkBack
            String noteTitle = note.getTitle() != null && !note.getTitle().trim().isEmpty() ? 
                note.getTitle() : note.getContent();
            holder.itemView.setContentDescription("Note: " + noteTitle + ". " + status);
            
            // Read out note when clicked
            holder.itemView.setOnClickListener(v -> {
                ttsHelper.speak("Note: " + noteTitle + ". " + status);
            });
        }
        
        @Override
        public int getItemCount() {
            return notes.size();
        }
        
        private void deleteNote(Note note, int position) {
            notesManager.deleteNote(note.getFirestoreId(), new FirestoreNotesManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    notes.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, notes.size());
                    updateEmptyState();
                    Toast.makeText(ViewNotesActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                    ttsHelper.speak("Note deleted successfully");
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(ViewNotesActivity.this, "Failed to delete note: " + error, Toast.LENGTH_SHORT).show();
                    ttsHelper.speak("Failed to delete note");
                }
            });
        }
        
        class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvNoteContent, tvNoteStatus;
            Button btnEdit, btnDelete;
            
            NoteViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
                tvNoteStatus = itemView.findViewById(R.id.tvNoteStatus);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
} 
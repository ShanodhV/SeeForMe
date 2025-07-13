package com.shanodh.seeforme.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.shanodh.seeforme.MainActivity;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.data.Note;
import com.shanodh.seeforme.firebase.FirestoreNotesManager;
import com.shanodh.seeforme.voice.SpeechRecognitionHelper;
import com.shanodh.seeforme.voice.TextToSpeechHelper;

import java.util.ArrayList;

public class AddNoteActivity extends AppCompatActivity implements
        SpeechRecognitionHelper.SpeechRecognitionCallback,
        TextToSpeechHelper.TtsCallback {

    private static final String TAG = "AddNoteActivity";
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_NOTE_TITLE = "note_title";
    public static final String EXTRA_NOTE_CONTENT = "note_content";
    
    private EditText etNoteTitle, etNoteContent;
    private Button btnVoiceInput, btnSaveNote;
    private TextView tvStatus;
    private SpeechRecognitionHelper speechHelper;
    private TextToSpeechHelper ttsHelper;
    private FirestoreNotesManager notesManager;
    
    private boolean isEditMode = false;
    private String editingNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);
        
        // Check if this is edit mode
        if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
            isEditMode = true;
            editingNoteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        }
        
        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Note" : "Add Note");
        }
        
        initializeViews();
        setupClickListeners();
        
        // Load note content if in edit mode
        if (isEditMode) {
            loadNoteForEditing();
        }
        
        speechHelper = new SpeechRecognitionHelper(this, this);
        ttsHelper = new TextToSpeechHelper(this, this);
    }
    
    private void initializeViews() {
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        tvStatus = findViewById(R.id.tvStatus);
        
        // Initialize Firestore manager
        notesManager = new FirestoreNotesManager();
    }
    
    private void setupClickListeners() {
        btnVoiceInput.setOnClickListener(v -> startVoiceInput());
        btnSaveNote.setOnClickListener(v -> saveNote());
        btnSaveNote.setText(isEditMode ? "Update Note" : "Save Note");
    }
    
    private void loadNoteForEditing() {
        String title = getIntent().getStringExtra(EXTRA_NOTE_TITLE);
        String content = getIntent().getStringExtra(EXTRA_NOTE_CONTENT);
        
        if (title != null) {
            etNoteTitle.setText(title);
        }
        if (content != null) {
            etNoteContent.setText(content);
        }
        
        tvStatus.setText("Ready to edit your note");
    }
    
    private void startVoiceInput() {
        if (speechHelper != null) {
            tvStatus.setText("Listening...");
            ttsHelper.speak("Speak your note");
            speechHelper.startListening();
        }
    }
    
    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        
        if (content.isEmpty()) {
            tvStatus.setText("Please enter some content");
            ttsHelper.speak("Please enter some content for your note");
            return;
        }

        // Create note object
        Note note = new Note();
        note.setTitle(TextUtils.isEmpty(title) ? generateTitleFromContent(content) : title);
        note.setContent(content);
        note.setVoiceNote(false); // Set to true if it was created via voice
        note.setTimestamp(System.currentTimeMillis());

        // Show saving status
        tvStatus.setText(isEditMode ? "Updating note..." : "Saving note...");
        btnSaveNote.setEnabled(false);

        if (isEditMode) {
            // Update existing note
            note.setFirestoreId(editingNoteId);
            notesManager.updateNote(note, new FirestoreNotesManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    tvStatus.setText("Note updated successfully!");
                    ttsHelper.speak("Note updated successfully");
                    finish();
                }

                @Override
                public void onError(String error) {
                    tvStatus.setText("Failed to update note");
                    ttsHelper.speak("Failed to update note. Please try again.");
                    btnSaveNote.setEnabled(true);
                    Toast.makeText(AddNoteActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Save new note to Firestore
            notesManager.addNote(note, new FirestoreNotesManager.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String noteId) {
                    tvStatus.setText("Note saved successfully!");
                    ttsHelper.speak("Note saved successfully");
                    finish();
                }

                @Override
                public void onError(String error) {
                    tvStatus.setText("Failed to save note");
                    ttsHelper.speak("Failed to save note. Please try again.");
                    btnSaveNote.setEnabled(true);
                    Toast.makeText(AddNoteActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Untitled Note";
        }
        String trimmed = content.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) + "..." : trimmed;
    }

    @Override
    public void onResults(ArrayList<String> results) {
        if (results != null && !results.isEmpty()) {
            String text = results.get(0);
            etNoteContent.setText(text);
            tvStatus.setText("Ready to save your note");
            ttsHelper.speak("I heard: " + text + ". Ready to save your note.");
        }
    }

    @Override
    public void onError(int errorCode) {
        tvStatus.setText("Error in speech recognition");
        ttsHelper.speak("I couldn't hear you properly. Please try again.");
    }

    @Override
    public void onReadyForSpeech() {
        tvStatus.setText("Listening...");
    }

    @Override
    public void onEndOfSpeech() {
        tvStatus.setText("Processing...");
    }

    @Override
    public void onTtsInitialized(boolean success) {
        if (success) {
            String message = isEditMode ? "Ready to edit your note. You can update the content or use voice input." : "Ready to add a new note. You can type or use voice input.";
            ttsHelper.speak(message);
        }
    }

    @Override
    public void onTtsCompleted() {
        // Not used in this activity
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        
        super.onDestroy();
    }
} 
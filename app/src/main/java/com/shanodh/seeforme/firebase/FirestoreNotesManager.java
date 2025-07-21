package com.shanodh.seeforme.firebase;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.shanodh.seeforme.data.Note;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore manager for handling notes in Firebase
 */
public class FirestoreNotesManager {
    private static final String TAG = "FirestoreNotesManager";
    private static final String COLLECTION_NOTES = "notes";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MutableLiveData<List<Note>> notesLiveData;
    private MutableLiveData<String> errorLiveData;

    public interface FirestoreCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public FirestoreNotesManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        notesLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
    }

    public LiveData<List<Note>> getNotesLiveData() {
        return notesLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Add a new note to Firestore
     */
    public void addNote(Note note, FirestoreCallback<String> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            errorLiveData.setValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        // Create note data
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("title", note.getTitle());
        noteData.put("content", note.getContent());
        noteData.put("timestamp", note.getTimestamp());
        noteData.put("userId", currentUser.getUid());
        noteData.put("isVoiceNote", note.isVoiceNote());
        noteData.put("category", note.getCategory() != null ? note.getCategory() : "");

        db.collection(COLLECTION_NOTES)
                .add(noteData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Note added with ID: " + documentReference.getId());
                        if (callback != null) callback.onSuccess(documentReference.getId());
                        loadUserNotes(); // Refresh the list
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding note", e);
                        String error = "Failed to save note: " + e.getMessage();
                        errorLiveData.setValue(error);
                        if (callback != null) callback.onError(error);
                    }
                });
    }

    /**
     * Update an existing note
     */
    public void updateNote(String noteId, Note note, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            errorLiveData.setValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("title", note.getTitle());
        noteData.put("content", note.getContent());
        noteData.put("timestamp", note.getTimestamp());
        noteData.put("isVoiceNote", note.isVoiceNote());
        noteData.put("category", note.getCategory() != null ? note.getCategory() : "");

        db.collection(COLLECTION_NOTES).document(noteId)
                .update(noteData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Note updated successfully");
                        if (callback != null) callback.onSuccess(null);
                        loadUserNotes(); // Refresh the list
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating note", e);
                        String error = "Failed to update note: " + e.getMessage();
                        errorLiveData.setValue(error);
                        if (callback != null) callback.onError(error);
                    }
                });
    }

    /**
     * Update an existing note using Note object
     */
    public void updateNote(Note note, FirestoreCallback<Void> callback) {
        if (note.getFirestoreId() == null || note.getFirestoreId().isEmpty()) {
            String error = "Note Firestore ID is required for update";
            if (callback != null) callback.onError(error);
            return;
        }
        updateNote(note.getFirestoreId(), note, callback);
    }

    /**
     * Delete a note
     */
    public void deleteNote(String noteId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_NOTES).document(noteId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Note deleted successfully");
                        if (callback != null) callback.onSuccess(null);
                        loadUserNotes(); // Refresh the list
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting note", e);
                        String error = "Failed to delete note: " + e.getMessage();
                        errorLiveData.setValue(error);
                        if (callback != null) callback.onError(error);
                    }
                });
    }

    /**
     * Load all notes for the current user with callback
     */
    public void getUserNotes(FirestoreCallback<List<Note>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            errorLiveData.setValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        db.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Note> notes = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Note note = documentToNote(document);
                                if (note != null) {
                                    notes.add(note);
                                }
                            }
                            // Sort notes by timestamp in descending order (newest first)
                            notes.sort((note1, note2) -> Long.compare(note2.getTimestamp(), note1.getTimestamp()));
                            notesLiveData.setValue(notes);
                            Log.d(TAG, "Loaded " + notes.size() + " notes");
                            if (callback != null) callback.onSuccess(notes);
                        } else {
                            Log.w(TAG, "Error getting notes", task.getException());
                            String error = "Failed to load notes: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error");
                            errorLiveData.setValue(error);
                            if (callback != null) callback.onError(error);
                        }
                    }
                });
    }

    /**
     * Load all notes for the current user
     */
    public void loadUserNotes() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            errorLiveData.setValue(error);
            return;
        }

        db.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Note> notes = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Note note = documentToNote(document);
                                if (note != null) {
                                    notes.add(note);
                                }
                            }
                            // Sort notes by timestamp in descending order (newest first)
                            notes.sort((note1, note2) -> Long.compare(note2.getTimestamp(), note1.getTimestamp()));
                            notesLiveData.setValue(notes);
                            Log.d(TAG, "Loaded " + notes.size() + " notes");
                        } else {
                            Log.w(TAG, "Error getting notes", task.getException());
                            String error = "Failed to load notes: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error");
                            errorLiveData.setValue(error);
                        }
                    }
                });
    }

    /**
     * Get a specific note by ID
     */
    public void getNoteById(String noteId, FirestoreCallback<Note> callback) {
        db.collection(COLLECTION_NOTES).document(noteId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Note note = documentToNote(document);
                                if (callback != null) {
                                    if (note != null) {
                                        callback.onSuccess(note);
                                    } else {
                                        callback.onError("Failed to parse note data");
                                    }
                                }
                            } else {
                                if (callback != null) callback.onError("Note not found");
                            }
                        } else {
                            Log.w(TAG, "Error getting note", task.getException());
                            String error = "Failed to get note: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error");
                            if (callback != null) callback.onError(error);
                        }
                    }
                });
    }

    /**
     * Convert Firestore document to Note object
     */
    private Note documentToNote(DocumentSnapshot document) {
        try {
            Note note = new Note();
            note.setFirestoreId(document.getId());
            note.setTitle(document.getString("title"));
            note.setContent(document.getString("content"));
            
            // Handle timestamp
            Object timestampObj = document.get("timestamp");
            if (timestampObj instanceof Long) {
                note.setTimestamp((Long) timestampObj);
            } else if (timestampObj instanceof Date) {
                note.setTimestamp(((Date) timestampObj).getTime());
            } else {
                note.setTimestamp(System.currentTimeMillis());
            }
            
            // Handle boolean fields
            Boolean isVoiceNote = document.getBoolean("isVoiceNote");
            note.setVoiceNote(isVoiceNote != null ? isVoiceNote : false);
            
            String category = document.getString("category");
            note.setCategory(category != null ? category : "");
            
            return note;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Note", e);
            return null;
        }
    }

    /**
     * Search notes by content
     */
    public void searchNotes(String searchQuery, FirestoreCallback<List<Note>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            errorLiveData.setValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        // Note: Firestore doesn't support full-text search natively
        // This is a simple implementation that filters by title
        db.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Note> filteredNotes = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Note note = documentToNote(document);
                                if (note != null && 
                                    (note.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                     note.getContent().toLowerCase().contains(searchQuery.toLowerCase()))) {
                                    filteredNotes.add(note);
                                }
                            }
                            // Sort filtered notes by timestamp in descending order (newest first)
                            filteredNotes.sort((note1, note2) -> Long.compare(note2.getTimestamp(), note1.getTimestamp()));
                            if (callback != null) callback.onSuccess(filteredNotes);
                        } else {
                            Log.w(TAG, "Error searching notes", task.getException());
                            String error = "Failed to search notes: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error");
                            if (callback != null) callback.onError(error);
                        }
                    }
                });
    }
}

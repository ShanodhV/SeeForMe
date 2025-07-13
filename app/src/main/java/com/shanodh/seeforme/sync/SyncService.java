package com.shanodh.seeforme.sync;

import android.content.Context;
import android.util.Log;

import com.shanodh.seeforme.data.AppDatabase;
import com.shanodh.seeforme.data.FamiliarFace;
import com.shanodh.seeforme.data.Note;
import com.shanodh.seeforme.firebase.FirestoreNotesManager;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service to synchronize local data with Firebase
 */
public class SyncService {
    private static final String TAG = "SyncService";
    
    private Context context;
    private FirestoreNotesManager notesManager;
    private AppDatabase localDb;
    private Executor executor;
    
    public interface SyncCallback {
        void onSyncStarted();
        void onSyncCompleted(boolean success, String message);
        void onSyncProgress(String operation);
    }
    
    public SyncService(Context context) {
        this.context = context;
        this.notesManager = new FirestoreNotesManager();
        this.localDb = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public void syncAllData(SyncCallback callback) {
        // For now, skip auth check since we'll handle auth in the calling activity
        Log.d(TAG, "Starting sync process...");
        callback.onSyncStarted();
        
        callback.onSyncStarted();
        // Temporarily disabled Firebase sync
        callback.onSyncCompleted(true, "Sync disabled - Firebase not configured");
        
        executor.execute(() -> {
            try {
                // Sync unsynced notes to Firebase
                callback.onSyncProgress("Syncing notes to cloud...");
                syncLocalNotesToFirebase();
                
                // Sync unsynced faces to Firebase
                callback.onSyncProgress("Syncing faces to cloud...");
                syncLocalFacesToFirebase();
                
                // Download notes from Firebase
                callback.onSyncProgress("Downloading notes from cloud...");
                downloadNotesFromFirebase();
                
                // Download faces from Firebase
                callback.onSyncProgress("Downloading faces from cloud...");
                downloadFacesFromFirebase();
                
                callback.onSyncCompleted(true, "Sync completed successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Sync failed", e);
                callback.onSyncCompleted(false, "Sync failed: " + e.getMessage());
            }
        });
    }
    
    private void syncLocalNotesToFirebase() {
        List<Note> unsyncedNotes = localDb.noteDao().getUnsyncedNotes();
        
        for (Note note : unsyncedNotes) {
            // If note already has Firebase ID, update it; otherwise add it
            if (note.getFirebaseId() != null && !note.getFirebaseId().isEmpty()) {
                notesManager.updateNote(note.getFirebaseId(), note, new FirestoreNotesManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        executor.execute(() -> {
                            note.setSynced(true);
                            localDb.noteDao().update(note);
                            Log.d(TAG, "Note updated in Firebase: " + note.getContent());
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to update note in Firebase: " + error);
                    }
                });
            } else {
                notesManager.addNote(note, new FirestoreNotesManager.FirestoreCallback<String>() {
                    @Override
                    public void onSuccess(String firebaseId) {
                        executor.execute(() -> {
                            note.setFirebaseId(firebaseId);
                            note.setSynced(true);
                            localDb.noteDao().update(note);
                            Log.d(TAG, "Note synced to Firebase: " + note.getContent());
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to sync note to Firebase: " + error);
                    }
                });
            }
        }
    }
    
    private void syncLocalFacesToFirebase() {
        List<FamiliarFace> unsyncedFaces = localDb.familiarFaceDao().getUnsyncedFaces();
        
        for (FamiliarFace face : unsyncedFaces) {
            // Note: For faces with images, you'd need to handle bitmap loading
            // This is a simplified version
            if (face.getImagePath() != null && !face.getImagePath().isEmpty()) {
                // Load bitmap from local path and upload
                // This would require additional image handling code
                Log.d(TAG, "Face sync requires image handling: " + face.getName());
            }
        }
    }
    
    private void downloadNotesFromFirebase() {
        // For now, we'll skip downloading from Firebase during sync
        // The notes are loaded via LiveData when the user opens the notes screen
        Log.d(TAG, "Note downloading from Firebase is handled via LiveData, not during sync");
    }
    
    private void downloadFacesFromFirebase() {
        // Since we decided to keep familiar faces in local storage only,
        // this method is disabled for now
        Log.d(TAG, "Familiar faces sync is disabled - local storage only");
    }
    
    public void schedulePeriodicSync() {
        // This would typically use WorkManager for background sync
        // For now, just log the intention
        Log.d(TAG, "Periodic sync would be scheduled here using WorkManager");
    }
}

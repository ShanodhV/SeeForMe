package com.shanodh.seeforme.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a note in the database
 * Now supports both Room (local) and Firestore (cloud) storage
 */
@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String firestoreId; // For Firestore document ID

    private String title;
    private String content;
    private long timestamp;
    private boolean isDetected;
    private boolean isVoiceNote;
    private String category;
    private boolean isSynced; // Track if synced with Firestore

    // Default constructor for Room and Firestore
    public Note() {
        this.timestamp = System.currentTimeMillis();
        this.isDetected = false;
        this.isVoiceNote = false;
        this.isSynced = false;
        this.category = "";
    }

    @Ignore
    public Note(String content) {
        this();
        this.content = content;
        this.title = generateTitleFromContent(content);
    }

    @Ignore
    public Note(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    // Generate title from content (first 30 characters)
    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Untitled Note";
        }
        String trimmed = content.trim();
        return trimmed.length() > 30 ? trimmed.substring(0, 30) + "..." : trimmed;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }

    // Alias for backward compatibility
    public String getFirebaseId() {
        return firestoreId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firestoreId = firebaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        // Auto-generate title if not set
        if (this.title == null || this.title.trim().isEmpty()) {
            this.title = generateTitleFromContent(content);
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isDetected() {
        return isDetected;
    }

    public void setDetected(boolean detected) {
        isDetected = detected;
    }

    public boolean isVoiceNote() {
        return isVoiceNote;
    }

    public void setVoiceNote(boolean voiceNote) {
        isVoiceNote = voiceNote;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", firestoreId='" + firestoreId + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", isVoiceNote=" + isVoiceNote +
                ", category='" + category + '\'' +
                ", isSynced=" + isSynced +
                '}';
    }
}
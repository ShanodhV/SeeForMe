package com.shanodh.seeforme.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a familiar face in the database
 */
@Entity(tableName = "familiar_faces")
public class FamiliarFace {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String relationship;
    private String imagePath;
    private boolean isSynced;
    private long timestamp;
    private String firebaseId; // For cloud storage reference

    // Default constructor for Room
    public FamiliarFace() {
        this.isSynced = false;
        this.timestamp = System.currentTimeMillis();
    }

    @Ignore
    public FamiliarFace(String name, String relationship, String imagePath) {
        this();
        this.name = name;
        this.relationship = relationship;
        this.imagePath = imagePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }
}
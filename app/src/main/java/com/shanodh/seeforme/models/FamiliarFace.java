package com.shanodh.seeforme.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "familiar_faces")
public class FamiliarFace {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String imagePath;
    private String description;
    private long createdAt;

    public FamiliarFace() {
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public FamiliarFace(String name, String imagePath) {
        this.name = name;
        this.imagePath = imagePath;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

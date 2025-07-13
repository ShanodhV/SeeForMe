package com.shanodh.seeforme.models;

public class FaceData {
    private String id;
    private String personName;
    private String relationship;
    private String localImagePath;
    private long timestamp;
    private String userId; // For user-specific data

    public FaceData() {
        // Default constructor required for Firebase
    }

    public FaceData(String id, String personName, String relationship, String localImagePath, String userId) {
        this.id = id;
        this.personName = personName;
        this.relationship = relationship;
        this.localImagePath = localImagePath;
        this.timestamp = System.currentTimeMillis();
        this.userId = userId;
    }

    // Getters
    public String getId() { return id; }
    public String getPersonName() { return personName; }
    public String getRelationship() { return relationship; }
    public String getLocalImagePath() { return localImagePath; }
    public long getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setPersonName(String personName) { this.personName = personName; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public void setLocalImagePath(String localImagePath) { this.localImagePath = localImagePath; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUserId(String userId) { this.userId = userId; }
}

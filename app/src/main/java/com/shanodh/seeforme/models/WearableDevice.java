package com.shanodh.seeforme.models;

/**
 * Model class for wearable devices that can be paired with the app
 */
public class WearableDevice {
    private int id;
    private String name;
    private String type;
    private String macAddress;
    private boolean isSelected;
    
    public WearableDevice(int id, String name, String type, String macAddress) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.macAddress = macAddress;
        this.isSelected = false;
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
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
} 
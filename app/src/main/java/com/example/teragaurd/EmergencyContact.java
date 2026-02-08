package com.example.teragaurd;

/**
 * Data model class for emergency contacts
 */
public class EmergencyContact {
    private long id; // SQLite primary key
    private String name;
    private String phoneNumber;
    private boolean isEditable;

    public EmergencyContact(String name, String phoneNumber, boolean isEditable) {
        this.id = -1; // -1 indicates not yet saved to database
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isEditable = isEditable;
    }

    // Constructor with ID (for loading from database)
    public EmergencyContact(long id, String name, String phoneNumber, boolean isEditable) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isEditable = isEditable;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean editable) {
        isEditable = editable;
    }
}

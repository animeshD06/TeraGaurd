package com.example.teragaurd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Emergency Contacts
 * Provides CRUD operations for emergency contacts in SQLite database
 */
public class ContactDAO {

    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public ContactDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Open database connection for writing
     */
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Close database connection
     */
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    /**
     * Insert a new emergency contact
     * @param contact The contact to insert
     * @return The row ID of the newly inserted contact, or -1 if error
     */
    public long insertContact(EmergencyContact contact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, contact.getName());
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, contact.getPhoneNumber());
        values.put(DatabaseHelper.COLUMN_IS_EDITABLE, contact.isEditable() ? 1 : 0);

        long id = database.insert(DatabaseHelper.TABLE_EMERGENCY_CONTACTS, null, values);
        contact.setId(id); // Set the generated ID back to the contact
        return id;
    }

    /**
     * Get all emergency contacts from database
     * @return List of all emergency contacts
     */
    public List<EmergencyContact> getAllContacts() {
        List<EmergencyContact> contacts = new ArrayList<>();

        String[] columns = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME,
                DatabaseHelper.COLUMN_PHONE_NUMBER,
                DatabaseHelper.COLUMN_IS_EDITABLE
        };

        Cursor cursor = database.query(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                columns,
                null, null, null, null,
                DatabaseHelper.COLUMN_ID + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                EmergencyContact contact = cursorToContact(cursor);
                contacts.add(contact);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return contacts;
    }

    /**
     * Get only editable (user-added) contacts
     * @return List of editable contacts
     */
    public List<EmergencyContact> getEditableContacts() {
        List<EmergencyContact> contacts = new ArrayList<>();

        String[] columns = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME,
                DatabaseHelper.COLUMN_PHONE_NUMBER,
                DatabaseHelper.COLUMN_IS_EDITABLE
        };

        Cursor cursor = database.query(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                columns,
                DatabaseHelper.COLUMN_IS_EDITABLE + " = ?",
                new String[]{"1"},
                null, null,
                DatabaseHelper.COLUMN_ID + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                EmergencyContact contact = cursorToContact(cursor);
                contacts.add(contact);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return contacts;
    }

    /**
     * Get a single contact by ID
     * @param id The contact ID
     * @return The contact, or null if not found
     */
    public EmergencyContact getContactById(long id) {
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        EmergencyContact contact = null;
        if (cursor != null && cursor.moveToFirst()) {
            contact = cursorToContact(cursor);
            cursor.close();
        }

        return contact;
    }

    /**
     * Update an existing contact
     * @param contact The contact with updated values
     * @return Number of rows affected
     */
    public int updateContact(EmergencyContact contact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, contact.getName());
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, contact.getPhoneNumber());
        values.put(DatabaseHelper.COLUMN_IS_EDITABLE, contact.isEditable() ? 1 : 0);

        return database.update(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(contact.getId())}
        );
    }

    /**
     * Delete a contact by ID
     * @param id The contact ID to delete
     * @return Number of rows affected
     */
    public int deleteContact(long id) {
        return database.delete(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * Delete a contact object
     * @param contact The contact to delete
     * @return Number of rows affected
     */
    public int deleteContact(EmergencyContact contact) {
        return deleteContact(contact.getId());
    }

    /**
     * Delete all editable (user-added) contacts
     * @return Number of rows affected
     */
    public int deleteAllEditableContacts() {
        return database.delete(
                DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                DatabaseHelper.COLUMN_IS_EDITABLE + " = ?",
                new String[]{"1"}
        );
    }

    /**
     * Check if database has any contacts
     * @return true if contacts exist, false otherwise
     */
    public boolean hasContacts() {
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                null
        );

        boolean hasRecords = false;
        if (cursor != null && cursor.moveToFirst()) {
            hasRecords = cursor.getInt(0) > 0;
            cursor.close();
        }

        return hasRecords;
    }

    /**
     * Get count of all contacts
     * @return Total number of contacts
     */
    public int getContactsCount() {
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_EMERGENCY_CONTACTS,
                null
        );

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }

        return count;
    }

    /**
     * Convert cursor row to EmergencyContact object
     * @param cursor Database cursor positioned at a row
     * @return EmergencyContact object
     */
    private EmergencyContact cursorToContact(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
        String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE_NUMBER));
        boolean isEditable = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_EDITABLE)) == 1;

        EmergencyContact contact = new EmergencyContact(name, phoneNumber, isEditable);
        contact.setId(id);
        return contact;
    }
}

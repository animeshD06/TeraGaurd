package com.example.teragaurd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Database Helper class for teraGaurd app
 * Manages database creation and version management
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "teragaurd.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_EMERGENCY_CONTACTS = "emergency_contacts";

    // Emergency Contacts Table Columns
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_IS_EDITABLE = "is_editable";

    // Create Emergency Contacts Table SQL
    private static final String CREATE_TABLE_EMERGENCY_CONTACTS =
            "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_PHONE_NUMBER + " TEXT NOT NULL, " +
            COLUMN_IS_EDITABLE + " INTEGER DEFAULT 1);";

    // Singleton instance
    private static DatabaseHelper instance;

    /**
     * Get singleton instance of DatabaseHelper
     * @param context Application context
     * @return DatabaseHelper instance
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the emergency contacts table
        db.execSQL(CREATE_TABLE_EMERGENCY_CONTACTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, simply drop and recreate
        // In production, you'd want to migrate data properly
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMERGENCY_CONTACTS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

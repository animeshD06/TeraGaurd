package com.example.teragaurd;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for accessing Call Log Content Provider
 * Provides methods to read call history for emergency features
 */
public class CallLogHelper {

    private static final String TAG = "CallLogHelper";
    
    private Context context;
    private ContentResolver contentResolver;

    public CallLogHelper(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    /**
     * Data class representing a call log entry
     */
    public static class CallLogEntry {
        private String number;
        private String name;
        private int type; // INCOMING, OUTGOING, MISSED
        private long date;
        private long duration; // in seconds

        public CallLogEntry(String number, String name, int type, long date, long duration) {
            this.number = number;
            this.name = name;
            this.type = type;
            this.date = date;
            this.duration = duration;
        }

        public String getNumber() { return number; }
        public String getName() { return name; }
        public int getType() { return type; }
        public long getDate() { return date; }
        public long getDuration() { return duration; }

        public String getTypeString() {
            switch (type) {
                case CallLog.Calls.INCOMING_TYPE:
                    return "Incoming";
                case CallLog.Calls.OUTGOING_TYPE:
                    return "Outgoing";
                case CallLog.Calls.MISSED_TYPE:
                    return "Missed";
                case CallLog.Calls.REJECTED_TYPE:
                    return "Rejected";
                default:
                    return "Unknown";
            }
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(date));
        }

        public String getFormattedDuration() {
            if (duration < 60) {
                return duration + " sec";
            } else {
                long minutes = duration / 60;
                long seconds = duration % 60;
                return minutes + " min " + seconds + " sec";
            }
        }
    }

    /**
     * Check if app has READ_CALL_LOG permission
     */
    public boolean hasCallLogPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get recent call logs
     * @param limit Maximum number of entries to retrieve
     * @return List of CallLogEntry objects
     */
    public List<CallLogEntry> getRecentCalls(int limit) {
        List<CallLogEntry> callLogs = new ArrayList<>();
        
        if (!hasCallLogPermission()) {
            Log.w(TAG, "READ_CALL_LOG permission not granted");
            return callLogs;
        }

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT " + limit;

        try {
            Cursor cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                    callLogs.add(new CallLogEntry(number, name, type, date, duration));
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log: " + e.getMessage());
        }

        return callLogs;
    }

    /**
     * Get calls made to a specific number
     * @param phoneNumber The phone number to search for
     * @param limit Maximum number of entries
     * @return List of CallLogEntry objects
     */
    public List<CallLogEntry> getCallsToNumber(String phoneNumber, int limit) {
        List<CallLogEntry> callLogs = new ArrayList<>();
        
        if (!hasCallLogPermission()) {
            Log.w(TAG, "READ_CALL_LOG permission not granted");
            return callLogs;
        }

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        // Clean phone number for comparison
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        String selection = "REPLACE(REPLACE(" + CallLog.Calls.NUMBER + ", '-', ''), ' ', '') LIKE ?";
        String[] selectionArgs = {"%" + cleanNumber};
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT " + limit;

        try {
            Cursor cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                    callLogs.add(new CallLogEntry(number, name, type, date, duration));
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log: " + e.getMessage());
        }

        return callLogs;
    }

    /**
     * Get calls to emergency numbers
     * @return List of CallLogEntry objects for emergency calls
     */
    public List<CallLogEntry> getEmergencyCalls() {
        List<CallLogEntry> emergencyCalls = new ArrayList<>();
        
        // Common emergency numbers (India)
        String[] emergencyNumbers = {"100", "101", "102", "108", "1091", "112"};
        
        for (String number : emergencyNumbers) {
            List<CallLogEntry> calls = getCallsToNumber(number, 5);
            emergencyCalls.addAll(calls);
        }
        
        // Sort by date (most recent first)
        emergencyCalls.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
        
        return emergencyCalls;
    }

    /**
     * Get outgoing calls only
     * @param limit Maximum number of entries
     * @return List of outgoing CallLogEntry objects
     */
    public List<CallLogEntry> getOutgoingCalls(int limit) {
        List<CallLogEntry> callLogs = new ArrayList<>();
        
        if (!hasCallLogPermission()) {
            return callLogs;
        }

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        String selection = CallLog.Calls.TYPE + " = ?";
        String[] selectionArgs = {String.valueOf(CallLog.Calls.OUTGOING_TYPE)};
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT " + limit;

        try {
            Cursor cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                    callLogs.add(new CallLogEntry(number, name, type, date, duration));
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log: " + e.getMessage());
        }

        return callLogs;
    }

    /**
     * Get count of calls to a specific number
     * @param phoneNumber The phone number to count calls for
     * @return Number of calls made to/from this number
     */
    public int getCallCountForNumber(String phoneNumber) {
        return getCallsToNumber(phoneNumber, 1000).size();
    }

    /**
     * Check if a call was made to emergency services recently
     * @param withinMinutes Check within last N minutes
     * @return true if emergency call was made recently
     */
    public boolean hasRecentEmergencyCall(int withinMinutes) {
        List<CallLogEntry> emergencyCalls = getEmergencyCalls();
        
        if (emergencyCalls.isEmpty()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long threshold = withinMinutes * 60 * 1000L;
        
        for (CallLogEntry call : emergencyCalls) {
            if ((currentTime - call.getDate()) <= threshold) {
                return true;
            }
        }
        
        return false;
    }
}

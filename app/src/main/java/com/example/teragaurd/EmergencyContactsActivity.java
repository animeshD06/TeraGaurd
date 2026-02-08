package com.example.teragaurd;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsActivity extends AppCompatActivity implements EmergencyContactsAdapter.OnContactActionListener {

    private static final int CALL_PERMISSION_REQUEST_CODE = 201;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 202;
    private static final int CALL_LOG_PERMISSION_REQUEST_CODE = 203;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 204;

    private RecyclerView recyclerView;
    private EmergencyContactsAdapter adapter;
    private List<EmergencyContact> contactsList;
    private String pendingCallNumber;
    
    // SQLite Database
    private ContactDAO contactDAO;
    
    // Content Provider Helpers
    private CallLogHelper callLogHelper;
    private EmergencySettingsHelper emergencySettingsHelper;
    
    // Contact Picker Launcher (Modern approach using Activity Result API)
    private ActivityResultLauncher<Intent> contactPickerLauncher;
    
    // SOS Button
    private ImageButton btnSOS;
    private boolean isSOSActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_contacts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize SQLite Database
        contactDAO = new ContactDAO(this);
        contactDAO.open();
        
        // Initialize Content Provider Helpers
        callLogHelper = new CallLogHelper(this);
        emergencySettingsHelper = new EmergencySettingsHelper(this);
        
        // Initialize Contact Picker Launcher
        initContactPickerLauncher();

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Setup Import from Phonebook button
        findViewById(R.id.btnImportContact).setOnClickListener(v -> openContactPicker());
        
        // Setup SOS Emergency Mode button
        btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> toggleSOSMode());
        btnSOS.setOnLongClickListener(v -> {
            showCallLogDialog();
            return true;
        });

        // Initialize contacts list
        contactsList = new ArrayList<>();
        loadContacts();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactsAdapter(contactsList, this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        FloatingActionButton fab = findViewById(R.id.fabAddContact);
        fab.setOnClickListener(v -> showAddContactDialog());
        
        // Request necessary permissions
        requestContentProviderPermissions();
    }
    
    /**
     * Initialize the contact picker activity result launcher
     * Uses modern Activity Result API instead of deprecated startActivityForResult
     */
    private void initContactPickerLauncher() {
        contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleContactPickerResult(result.getData());
                }
            }
        );
    }
    
    /**
     * Request permissions for Contact, Call Log, and Camera (flashlight)
     */
    private void requestContentProviderPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                CONTACTS_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure database is open when activity resumes
        if (contactDAO != null) {
            contactDAO.open();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Close database when activity is paused to free resources
        if (contactDAO != null) {
            contactDAO.close();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up emergency settings
        if (emergencySettingsHelper != null) {
            emergencySettingsHelper.cleanup();
        }
    }

    private void loadContacts() {
        // Check if preset contacts are already in database
        if (!contactDAO.hasContacts()) {
            // First time - add preset emergency numbers (India) to database
            initializePresetContacts();
        }
        
        // Load all contacts from SQLite database
        contactsList.clear();
        contactsList.addAll(contactDAO.getAllContacts());
    }

    /**
     * Initialize preset emergency contacts in database (only runs once)
     */
    private void initializePresetContacts() {
        // Add preset emergency numbers (India) - these are not editable
        contactDAO.insertContact(new EmergencyContact("Police", "100", false));
        contactDAO.insertContact(new EmergencyContact("Fire Department", "101", false));
        contactDAO.insertContact(new EmergencyContact("Ambulance", "102", false));
        contactDAO.insertContact(new EmergencyContact("Disaster Management", "108", false));
        contactDAO.insertContact(new EmergencyContact("Women Helpline", "1091", false));
    }
    
    // ==================== CONTENT PROVIDER 1: CONTACTS CONTRACT ====================
    
    /**
     * Open system contact picker to import from phonebook
     */
    private void openContactPicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_CONTACTS}, 
                CONTACTS_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Launch contact picker using ContactsContract Content Provider
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, 
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(pickContactIntent);
    }
    
    /**
     * Handle the result from contact picker
     * Reads contact data using ContactsContract Content Provider
     */
    private void handleContactPickerResult(Intent data) {
        Uri contactUri = data.getData();
        if (contactUri == null) {
            Toast.makeText(this, "Failed to get contact", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] projection = {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        
        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                
                // Clean phone number (remove spaces, dashes)
                phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
                
                // Check if contact already exists
                if (isContactExists(phoneNumber)) {
                    Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Add to database
                EmergencyContact newContact = new EmergencyContact(name, phoneNumber, true);
                long id = contactDAO.insertContact(newContact);
                
                if (id != -1) {
                    newContact.setId(id);
                    adapter.addItem(newContact);
                    Toast.makeText(this, "Contact imported: " + name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to import contact", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error importing contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check if a phone number already exists in the database
     */
    private boolean isContactExists(String phoneNumber) {
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        for (EmergencyContact contact : contactsList) {
            String existingNumber = contact.getPhoneNumber().replaceAll("[^0-9]", "");
            if (existingNumber.equals(cleanNumber) || 
                existingNumber.endsWith(cleanNumber) || 
                cleanNumber.endsWith(existingNumber)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== CONTENT PROVIDER 2: CALL LOG ====================
    
    /**
     * Show recent call log dialog
     * Uses CallLog Content Provider via CallLogHelper
     */
    private void showCallLogDialog() {
        if (!callLogHelper.hasCallLogPermission()) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_CALL_LOG}, 
                CALL_LOG_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Get recent emergency calls
        List<CallLogHelper.CallLogEntry> emergencyCalls = callLogHelper.getEmergencyCalls();
        List<CallLogHelper.CallLogEntry> recentCalls = callLogHelper.getRecentCalls(10);
        
        StringBuilder message = new StringBuilder();
        
        // Show emergency calls first
        if (!emergencyCalls.isEmpty()) {
            message.append("📞 EMERGENCY CALLS:\n");
            for (int i = 0; i < Math.min(3, emergencyCalls.size()); i++) {
                CallLogHelper.CallLogEntry call = emergencyCalls.get(i);
                message.append("• ").append(call.getNumber())
                       .append(" - ").append(call.getFormattedDate())
                       .append("\n");
            }
            message.append("\n");
        }
        
        // Show recent calls
        message.append("📱 RECENT CALLS:\n");
        if (recentCalls.isEmpty()) {
            message.append("No recent calls found");
        } else {
            for (int i = 0; i < Math.min(5, recentCalls.size()); i++) {
                CallLogHelper.CallLogEntry call = recentCalls.get(i);
                String name = call.getName() != null ? call.getName() : call.getNumber();
                message.append("• ").append(name)
                       .append(" (").append(call.getTypeString()).append(")")
                       .append("\n  ").append(call.getFormattedDate())
                       .append("\n");
            }
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Call History")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .setNeutralButton("Add from History", (dialog, which) -> {
                showAddFromCallLogDialog(recentCalls);
            })
            .show();
    }
    
    /**
     * Show dialog to add contact from call history
     */
    private void showAddFromCallLogDialog(List<CallLogHelper.CallLogEntry> calls) {
        if (calls.isEmpty()) {
            Toast.makeText(this, "No calls to add", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] items = new String[Math.min(10, calls.size())];
        for (int i = 0; i < items.length; i++) {
            CallLogHelper.CallLogEntry call = calls.get(i);
            String name = call.getName() != null ? call.getName() : "Unknown";
            items[i] = name + " (" + call.getNumber() + ")";
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Select Contact to Add")
            .setItems(items, (dialog, which) -> {
                CallLogHelper.CallLogEntry selectedCall = calls.get(which);
                String name = selectedCall.getName() != null ? selectedCall.getName() : "Contact";
                String number = selectedCall.getNumber();
                
                if (!isContactExists(number)) {
                    EmergencyContact newContact = new EmergencyContact(name, number, true);
                    long id = contactDAO.insertContact(newContact);
                    if (id != -1) {
                        newContact.setId(id);
                        adapter.addItem(newContact);
                        Toast.makeText(this, "Contact added from call log", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    // ==================== CONTENT PROVIDER 3: SETTINGS (EMERGENCY MODE) ====================
    
    /**
     * Toggle SOS Emergency Mode
     * Activates flashlight SOS pattern, max volume, and vibration
     */
    private void toggleSOSMode() {
        // Check camera permission for flashlight
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        
        if (isSOSActive) {
            // Deactivate SOS mode
            emergencySettingsHelper.deactivateEmergencyMode();
            isSOSActive = false;
            btnSOS.setBackgroundResource(R.drawable.sos_button_background);
            Toast.makeText(this, "🔴 SOS Mode Deactivated", Toast.LENGTH_SHORT).show();
        } else {
            // Show confirmation dialog before activating
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Activate SOS Mode?")
                .setMessage("This will:\n\n" +
                           "• Flash SOS pattern with flashlight\n" +
                           "• Set volume to maximum\n" +
                           "• Vibrate SOS pattern\n\n" +
                           "Use only in real emergencies!")
                .setPositiveButton("Activate SOS", (dialog, which) -> {
                    emergencySettingsHelper.activateEmergencyMode();
                    isSOSActive = true;
                    btnSOS.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, null));
                    Toast.makeText(this, "🆘 SOS Mode Activated!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void showAddContactDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        EditText editName = dialogView.findViewById(R.id.editContactName);
        EditText editNumber = dialogView.findViewById(R.id.editContactNumber);

        new AlertDialog.Builder(this)
                .setTitle("Add Emergency Contact")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String number = editNumber.getText().toString().trim();

                    if (name.isEmpty() || number.isEmpty()) {
                        Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create new contact and save to SQLite database
                    EmergencyContact newContact = new EmergencyContact(name, number, true);
                    long id = contactDAO.insertContact(newContact);
                    
                    if (id != -1) {
                        newContact.setId(id);
                        adapter.addItem(newContact);
                        Toast.makeText(this, "Contact saved to database", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to save contact", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Import", (dialog, which) -> {
                    openContactPicker();
                })
                .show();
    }

    @Override
    public void onCallClick(EmergencyContact contact) {
        makePhoneCall(contact.getPhoneNumber());
    }

    @Override
    public void onDeleteClick(EmergencyContact contact, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from SQLite database
                    int rowsAffected = contactDAO.deleteContact(contact);
                    
                    if (rowsAffected > 0) {
                        adapter.removeItem(position);
                        Toast.makeText(this, "Contact deleted from database", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void makePhoneCall(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } else {
            pendingCallNumber = phoneNumber;
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CALL_PHONE}, 
                    CALL_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CALL_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (pendingCallNumber != null) {
                        makePhoneCall(pendingCallNumber);
                        pendingCallNumber = null;
                    }
                } else {
                    // If permission denied, open dialer instead
                    if (pendingCallNumber != null) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                        dialIntent.setData(Uri.parse("tel:" + pendingCallNumber));
                        startActivity(dialIntent);
                        pendingCallNumber = null;
                    }
                }
                break;
                
            case CONTACTS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Contacts permission granted", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case CALL_LOG_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCallLogDialog();
                } else {
                    Toast.makeText(this, "Call log permission required to view history", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleSOSMode();
                } else {
                    Toast.makeText(this, "Camera permission required for flashlight SOS", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}


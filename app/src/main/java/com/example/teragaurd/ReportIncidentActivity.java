package com.example.teragaurd;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ReportIncidentActivity extends AppCompatActivity {

    private Spinner spinnerIncidentType;
    private RadioGroup radioGroupSeverity;
    private CheckBox checkboxAmbulance;
    private Button btnSubmitReport;
    private ListView listViewReports;

    private ArrayList<String> reportHistory;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_incident);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Binding Views
        ImageButton btnBack = findViewById(R.id.btnBack);
        spinnerIncidentType = findViewById(R.id.spinnerIncidentType);
        radioGroupSeverity = findViewById(R.id.radioGroupSeverity);
        checkboxAmbulance = findViewById(R.id.checkboxAmbulance);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        listViewReports = findViewById(R.id.listViewReports);

        btnBack.setOnClickListener(v -> finish());

        // Setup ListView & ArrayList
        reportHistory = new ArrayList<>();
        
        // Custom ArrayAdapter to handle white text on the dark background
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, reportHistory) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE); // Make text readable on dark mode
                textView.setTextSize(14f);
                return view;
            }
        };
        listViewReports.setAdapter(listAdapter);

        // Submit Button Click Listener
        btnSubmitReport.setOnClickListener(v -> submitIncident());
    }

    private void submitIncident() {
        // 1. Get Spinner selected item
        String incidentType = spinnerIncidentType.getSelectedItem().toString();

        // 2. Get RadioButton selection
        int selectedRadioId = radioGroupSeverity.getCheckedRadioButtonId();
        String severity = "Unknown";
        if (selectedRadioId != -1) {
            RadioButton selectedRadio = findViewById(selectedRadioId);
            severity = selectedRadio.getText().toString();
        }

        // 3. Get CheckBox state
        boolean needsAmbulance = checkboxAmbulance.isChecked();

        // 4. Generate Timestamp
        long timestampMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault());
        String formattedTimestamp = sdf.format(new Date(timestampMillis));

        // Format a string for our ListView history
        String reportBlurb = formattedTimestamp + "\n" +
                "Type: " + incidentType + " | Severity: " + severity + "\n" +
                "Ambulance Required: " + (needsAmbulance ? "Yes" : "No");

        // Add to array list and notify the list view adapter
        reportHistory.add(0, reportBlurb); // Add to the top of the list
        listAdapter.notifyDataSetChanged();

        // Reset the form for demonstrating purposes
        spinnerIncidentType.setSelection(0);
        radioGroupSeverity.check(R.id.radioMedium); // Medium as default
        checkboxAmbulance.setChecked(false);

        Toast.makeText(this, "Incident Reported Successfully!", Toast.LENGTH_SHORT).show();
    }
}

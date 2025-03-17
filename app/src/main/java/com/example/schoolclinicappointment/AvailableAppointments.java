package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AvailableAppointments extends AppCompatActivity {

    private Spinner consultationTypeSpinner;
    private ListView appointmentsListView;
    private Button backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_appointments);

        // Initialize views
        consultationTypeSpinner = findViewById(R.id.consultationTypeSpinner);
        appointmentsListView = findViewById(R.id.appointmentsListView);
        backButton = findViewById(R.id.backButton);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Populate the spinner with consultation types
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.consultationType, // Ensure this array exists in your strings.xml
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        consultationTypeSpinner.setAdapter(spinnerAdapter);

        // Set listener for consultation type spinner
        consultationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedView, int position, long id) {
                String selectedConsultationType = (String) parentView.getItemAtPosition(position);
                fetchAvailableAppointments(selectedConsultationType); // Fetch available appointments based on selection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        // Back button functionality
        backButton.setOnClickListener(view -> finish());

        // Default to show "Dental Examination" appointments
        new Handler().postDelayed(() -> fetchAvailableAppointments("Dental Examination"), 500);
    }

    // Fetch available appointments from Firestore based on consultation type
    private void fetchAvailableAppointments(String consultationType) {
        db.collection("bookedAppointments")
                .whereEqualTo("consultationType", consultationType)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        ArrayList<String> appointmentList = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String time = document.getString("time");
                            String date = document.getString("date");
                            String studentName = document.getString("studentName");
                            String reason = document.getString("reason");

                            if (time != null && date != null && studentName != null && reason != null) {
                                String appointment = date + " | " + time + " | " + consultationType + " | " + studentName + " | Reason: " + reason;
                                appointmentList.add(appointment);
                            }
                        }

                        // Set up ListView with available appointments
                        if (appointmentList.isEmpty()) {
                            Toast.makeText(AvailableAppointments.this, "No appointments available for this type.", Toast.LENGTH_SHORT).show();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AvailableAppointments.this, android.R.layout.simple_list_item_1, appointmentList);
                        appointmentsListView.setAdapter(adapter);

                        // Handle item click on ListView
                        appointmentsListView.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedAppointment = appointmentList.get(position);
                            String[] appointmentDetails = selectedAppointment.split(" \\| ");
                            String selectedDate = appointmentDetails[0];
                            String selectedTime = appointmentDetails[1];
                            String selectedConsultationType = appointmentDetails[2];
                            String selectedStudentName = appointmentDetails[3];
                            String selectedReason = appointmentDetails[4];

                            // Show confirmation dialog to mark appointment as Confirmed or Cancelled
                            showConfirmationDialog(selectedDate, selectedTime, selectedConsultationType, selectedStudentName, selectedReason);
                        });

                    } else {
                        Toast.makeText(AvailableAppointments.this, "Error fetching appointments.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Show confirmation dialog to mark appointment as Confirmed or Cancelled
    private void showConfirmationDialog(String date, String time, String consultationType, String studentName, String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Appointment Status");
        builder.setMessage("Would you like to Confirm or Cancel this appointment?\n\nDate: " + date + "\nTime: " + time + "\nConsultation Type: " + consultationType + "\nStudent Name: " + studentName + "\nReason: " + reason);

        builder.setPositiveButton("Confirm", (dialog, which) -> updateStudentRecords(date, time, consultationType, studentName, reason, "Confirmed"));

        builder.setNegativeButton("Cancel", (dialog, which) -> updateStudentRecords(date, time, consultationType, studentName, reason, "Cancelled"));

        builder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    // Update the studentRecords collection with Confirmed or Cancelled status
    private void updateStudentRecords(String date, String time, String consultationType, String studentName, String reason, String status) {
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("consultationType", consultationType);
        appointmentData.put("studentName", studentName);
        appointmentData.put("reason", reason);
        appointmentData.put("status", status); // Store status as Confirmed or Cancelled

        db.collection("studentRecords").add(appointmentData)
                .addOnSuccessListener(aVoid -> {
                    removeFromBookedAppointments(date, time, consultationType, studentName);
                    Toast.makeText(AvailableAppointments.this, "Appointment marked as " + status + "!", Toast.LENGTH_SHORT).show();
                    refreshAppointmentsList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AvailableAppointments.this, "Error updating status.", Toast.LENGTH_SHORT).show();
                });
    }

    // Remove the appointment from bookedAppointments collection
    private void removeFromBookedAppointments(String date, String time, String consultationType, String studentName) {
        db.collection("bookedAppointments")
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .whereEqualTo("consultationType", consultationType)
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        db.collection("bookedAppointments").document(document.getId()).delete();
                    }
                });
    }

    // Refresh appointments list
    private void refreshAppointmentsList() {
        String selectedConsultationType = consultationTypeSpinner.getSelectedItem().toString();
        new Handler().postDelayed(() -> fetchAvailableAppointments(selectedConsultationType), 500);
    }
}

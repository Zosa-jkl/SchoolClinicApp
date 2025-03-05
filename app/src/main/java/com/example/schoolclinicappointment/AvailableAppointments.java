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
                fetchAvailableAppointments(selectedConsultationType); // Fetch the available appointments based on selection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle this case if needed
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
                .whereEqualTo("consultationType", consultationType) // Filter based on consultation type
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        ArrayList<String> appointmentList = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String time = document.getString("time");  // Fetch time from Firestore
                            String date = document.getString("date");  // Fetch date from Firestore
                            String studentName = document.getString("studentName"); // Fetch studentName from Firestore
                            String reason = document.getString("reason"); // Fetch reason from Firestore

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
                            String[] appointmentDetails = selectedAppointment.split(" \\| "); // Split the details
                            String selectedDate = appointmentDetails[0];
                            String selectedTime = appointmentDetails[1];
                            String selectedConsultationType = appointmentDetails[2];
                            String selectedStudentName = appointmentDetails[3];
                            String selectedReason = appointmentDetails[4];  // Fetch reason from the appointment details

                            // Confirm with the user to mark appointment as completed
                            showConfirmationDialog(selectedDate, selectedTime, selectedConsultationType, selectedStudentName, selectedReason);
                        });

                    } else {
                        Toast.makeText(AvailableAppointments.this, "Error fetching appointments.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Show confirmation dialog to the user
    private void showConfirmationDialog(String date, String time, String consultationType, String studentName, String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Appointment Completion");
        builder.setMessage("Are you sure this appointment is met?\nDate: " + date + "\nTime: " + time + "\nConsultation Type: " + consultationType + "\nStudent Name: " + studentName + "\nReason: " + reason);

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Transfer the document to studentRecords collection
            transferToStudentRecords(date, time, consultationType, studentName, reason);
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Transfer the appointment to studentRecords collection
    private void transferToStudentRecords(String date, String time, String consultationType, String studentName, String reason) {
        // Create an appointment object for the studentRecords collection
        Appointment appointment = new Appointment(date, time, consultationType, studentName, reason);

        // Add the appointment to the studentRecords collection
        db.collection("studentRecords").add(appointment)
                .addOnSuccessListener(aVoid -> {
                    // After successful addition, remove the appointment from bookedAppointments
                    removeFromBookedAppointments(date, time, consultationType, studentName, reason);

                    // Show a message indicating the appointment was completed
                    Toast.makeText(AvailableAppointments.this, "Appointment marked as completed!", Toast.LENGTH_SHORT).show();

                    // Refresh the appointments list with the current consultation type
                    String selectedConsultationType = consultationTypeSpinner.getSelectedItem().toString();
                    new Handler().postDelayed(() -> fetchAvailableAppointments(selectedConsultationType), 500);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AvailableAppointments.this, "Error transferring appointment.", Toast.LENGTH_SHORT).show();
                });
    }

    // Remove the appointment from bookedAppointments collection
    private void removeFromBookedAppointments(String date, String time, String consultationType, String studentName, String reason) {
        db.collection("bookedAppointments")
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .whereEqualTo("consultationType", consultationType)
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        db.collection("bookedAppointments").document(document.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Appointment successfully removed from bookedAppointments
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AvailableAppointments.this, "Error removing appointment.", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    // Appointment class to represent appointment data
    public static class Appointment {
        private String date;
        private String time;
        private String consultationType;
        private String studentName;
        private String reason;

        public Appointment() {
            // Default constructor required for Firestore
        }

        public Appointment(String date, String time, String consultationType, String studentName, String reason) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
            this.studentName = studentName;
            this.reason = reason;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public String getConsultationType() {
            return consultationType;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getReason() {
            return reason;
        }
    }
}

package com.example.schoolclinicappointment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;


public class StudentHomeView extends AppCompatActivity {

    private Button btnBookAppointment, btnCancelAppointment, btnLogout;
    private TextView txtAppointmentStatus, txtTitle;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String studentId;
    private String studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_view);

        // Initialize views
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnCancelAppointment = findViewById(R.id.btnCancelAppointment);
        txtAppointmentStatus = findViewById(R.id.txtAppointmentStatus);
        txtTitle = findViewById(R.id.txtTitle);
        btnLogout = findViewById(R.id.btnLogout);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        studentId = auth.getCurrentUser().getUid();

        // Fetch student name
        fetchStudentName();

        // Delay checking for appointments
        new Handler().postDelayed(() -> checkStudentAppointment(), 500);

        // Navigate to StudentView (Book Appointment)
        btnBookAppointment.setOnClickListener(view -> {
            try {
                Intent intent = new Intent(StudentHomeView.this, StudentView.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(StudentHomeView.this, "Error navigating to appointment booking", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel Appointment functionality
        btnCancelAppointment.setOnClickListener(view -> showCancelConfirmation());

        // Logout button functionality
        btnLogout.setOnClickListener(view -> showLogoutConfirmation());
    }

    private void fetchStudentName() {
        db.collection("users").document(studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            studentName = document.getString("name");
                            if (studentName != null) {
                                txtTitle.setText("Welcome, " + studentName + "!");
                            }
                        }
                    } else {
                        Toast.makeText(StudentHomeView.this, "Error fetching student name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(StudentHomeView.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void checkStudentAppointment() {
        db.collection("bookedAppointments")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean hasDental = false;
                        boolean hasPhysical = false;
                        StringBuilder appointmentDetails = new StringBuilder();

                        for (DocumentSnapshot document : task.getResult()) {
                            String consultationType = document.getString("consultationType");
                            String reason = document.getString("reason");
                            String appointmentTime = document.getString("time");
                            String appointmentDate = document.getString("date");

                            appointmentDetails.append("You have an appointment for: ").append(consultationType)
                                    .append("\nDate: ").append(appointmentDate)
                                    .append("\nTime: ").append(appointmentTime)
                                    .append("\nReason: ").append(reason)
                                    .append("\n\n");

                            if ("Dental Examination".equals(consultationType)) {
                                hasDental = true;
                            } else if ("Annual Physical".equals(consultationType)) {
                                hasPhysical = true;
                            }
                        }

                        if (hasDental && hasPhysical) {
                            // Hide the book appointment button if both appointments exist
                            btnBookAppointment.setVisibility(View.GONE);
                        } else {
                            btnBookAppointment.setVisibility(View.VISIBLE);
                        }

                        if (appointmentDetails.length() > 0) {
                            txtAppointmentStatus.setText(appointmentDetails.toString().trim());
                            btnCancelAppointment.setVisibility(View.VISIBLE);
                        } else {
                            txtAppointmentStatus.setText("No appointment found for the selected consultation.");
                            btnCancelAppointment.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void showCancelConfirmation() {
        db.collection("bookedAppointments")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        int appointmentCount = task.getResult().size();

                        if (appointmentCount == 1) {
                            // If only one appointment exists, cancel it directly
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String consultationType = document.getString("consultationType");

                            new AlertDialog.Builder(this)
                                    .setTitle("Cancel Appointment")
                                    .setMessage("Are you sure you want to cancel your " + consultationType + " appointment?")
                                    .setPositiveButton("Yes", (dialog, which) -> cancelAppointment(document))
                                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                                    .create()
                                    .show();
                        } else if (appointmentCount > 1) {
                            // If multiple appointments exist, let the user choose
                            showAppointmentSelectionDialog(task.getResult());
                        }
                    }
                });
    }
    private void showAppointmentSelectionDialog(QuerySnapshot appointments) {
        String[] appointmentOptions = new String[appointments.size()];
        DocumentSnapshot[] appointmentDocs = new DocumentSnapshot[appointments.size()];

        for (int i = 0; i < appointments.size(); i++) {
            appointmentDocs[i] = appointments.getDocuments().get(i);
            appointmentOptions[i] = appointmentDocs[i].getString("consultationType") + " on " + appointmentDocs[i].getString("date");
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Appointment to Cancel")
                .setItems(appointmentOptions, (dialog, which) -> {
                    DocumentSnapshot selectedAppointment = appointmentDocs[which];
                    String selectedType = selectedAppointment.getString("consultationType");

                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Cancellation")
                            .setMessage("Are you sure you want to cancel your " + selectedType + " appointment?")
                            .setPositiveButton("Yes", (confirmDialog, confirmWhich) -> cancelAppointment(selectedAppointment))
                            .setNegativeButton("No", (confirmDialog, confirmWhich) -> confirmDialog.dismiss())
                            .create()
                            .show();
                })
                .create()
                .show();
    }

    private void cancelAppointment(DocumentSnapshot appointmentDoc) {
        String consultationType = appointmentDoc.getString("consultationType");
        String date = appointmentDoc.getString("date");
        String time12hr = appointmentDoc.getString("time"); // 12-hour format with AM/PM
        String reason = appointmentDoc.getString("reason");

        // Convert 12-hour format (AM/PM) to 24-hour format
        String time24hr = convertTo24HourFormat(time12hr);

        // Store cancellation record in studentRecords
        AppointmentRecord record = new AppointmentRecord(studentName, date, time24hr, consultationType, reason, "cancelled");

        db.collection("studentRecords").add(record)
                .addOnSuccessListener(aVoid -> {
                    // Move only consultationType, date, and time (24-hour format) back to "Appointments"
                    Map<String, Object> cancelledAppointment = new HashMap<>();
                    cancelledAppointment.put("consultationType", consultationType);
                    cancelledAppointment.put("date", date);
                    cancelledAppointment.put("time", time24hr);
                    cancelledAppointment.put("reason", reason);

                    db.collection("Appointments").add(cancelledAppointment)
                            .addOnSuccessListener(aVoid1 -> {
                                db.collection("bookedAppointments").document(appointmentDoc.getId()).delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(StudentHomeView.this, "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                                            checkStudentAppointment();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to cancel appointment.", Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to return appointment to available slots.", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to record cancellation.", Toast.LENGTH_SHORT).show());
    }

    private String convertTo24HourFormat(String time12hr) {
        try {
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.US); // 12-hour format
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.US); // 24-hour format
            Date date = sdf12.parse(time12hr);
            return sdf24.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return time12hr; // Return the original if conversion fails
        }
    }

    public static class Appointment {
        private String date;
        private String time;
        private String consultationType;
        private String reason;

        public Appointment() {}

        public Appointment(String date, String time, String consultationType, String reason) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
            this.reason = reason;
        }

        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getConsultationType() { return consultationType; }
        public String getReason() { return reason; }
    }

    public static class AppointmentRecord {
        private String studentName;
        private String date;
        private String time;
        private String consultationType;
        private String reason;
        private String status;

        public AppointmentRecord() {}

        public AppointmentRecord(String studentName, String date, String time, String consultationType, String reason, String status) {
            this.studentName = studentName;
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
            this.reason = reason;
            this.status = status;
        }

        public String getStudentName() { return studentName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getConsultationType() { return consultationType; }
        public String getReason() { return reason; }
        public String getStatus() { return status; }
    }
}

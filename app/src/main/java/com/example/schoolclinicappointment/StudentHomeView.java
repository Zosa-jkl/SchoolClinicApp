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
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String consultationType = document.getString("consultationType");
                        String reason = document.getString("reason");
                        String appointmentTime = document.getString("time");
                        String appointmentDate = document.getString("date");

                        txtAppointmentStatus.setText(
                                "You have an appointment for: " + consultationType +
                                        "\nDate: " + appointmentDate +
                                        "\nTime: " + appointmentTime +
                                        "\nReason: " + reason
                        );

                        btnBookAppointment.setVisibility(View.GONE);
                        btnCancelAppointment.setVisibility(View.VISIBLE);
                    } else {
                        txtAppointmentStatus.setText("No appointment found for the selected consultation.");
                        btnBookAppointment.setVisibility(View.VISIBLE);
                        btnCancelAppointment.setVisibility(View.GONE);
                    }
                });
    }

    private void showCancelConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Appointment");
        builder.setMessage("Are you sure you want to cancel your appointment?");
        builder.setPositiveButton("Yes", (dialog, which) -> cancelAppointment());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void cancelAppointment() {
        db.collection("bookedAppointments")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);

                        String consultationType = document.getString("consultationType");
                        String reason = document.getString("reason");
                        String time = document.getString("time");
                        String date = document.getString("date");

                        // Store cancellation record in studentRecords
                        AppointmentRecord record = new AppointmentRecord(studentName, date, time, consultationType, reason, "cancelled");

                        db.collection("studentRecords").add(record)
                                .addOnSuccessListener(aVoid -> {
                                    // Move appointment back to "Appointments"
                                    Appointment cancelledAppointment = new Appointment(date, time, consultationType, reason);
                                    db.collection("Appointments").add(cancelledAppointment)
                                            .addOnSuccessListener(aVoid1 -> {
                                                db.collection("bookedAppointments").document(document.getId()).delete()
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            Toast.makeText(StudentHomeView.this, "Appointment cancelled and recorded.", Toast.LENGTH_SHORT).show();
                                                            checkStudentAppointment();
                                                        })
                                                        .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to cancel appointment.", Toast.LENGTH_SHORT).show());
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to return appointment to available slots.", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to record cancellation.", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(StudentHomeView.this, "Failed to fetch appointment details.", Toast.LENGTH_SHORT).show());
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

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
    private String studentName;  // Store the student's name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_view);

        // Initialize views
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnCancelAppointment = findViewById(R.id.btnCancelAppointment);  // Add the Cancel Appointment button
        txtAppointmentStatus = findViewById(R.id.txtAppointmentStatus);
        txtTitle = findViewById(R.id.txtTitle);  // Reference to the title TextView
        btnLogout = findViewById(R.id.btnLogout);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        studentId = auth.getCurrentUser().getUid(); // Get student ID from Firebase Auth

        // Fetch student name and update the title text
        fetchStudentName();

        // Delay the check for the student's appointment by 3 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkStudentAppointment();  // Initialize after 3 seconds
            }
        }, 500);

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
        btnCancelAppointment.setOnClickListener(view -> {
            showCancelConfirmation();
        });

        // Logout button functionality
        btnLogout.setOnClickListener(view -> showLogoutConfirmation());
    }

    // Method to fetch student name and update the title TextView
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

    // Method to show logout confirmation
    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(StudentHomeView.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close current activity
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Method to check if the student has an appointment
    private void checkStudentAppointment() {
        db.collection("bookedAppointments")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Appointment exists
                            DocumentSnapshot document = task.getResult().getDocuments().get(0); // Get the first document (if any)
                            String consultationType = document.getString("consultationType");
                            String reason = document.getString("reason");
                            String appointmentTime = document.getString("time");

                            txtAppointmentStatus.setText("You have an appointment for: " + consultationType + " at " + appointmentTime + "\nReason: " + reason);
                            btnBookAppointment.setVisibility(View.GONE);
                            btnCancelAppointment.setVisibility(View.VISIBLE); // Show Cancel Appointment button
                        } else {
                            // No appointment found
                            txtAppointmentStatus.setText("No appointment found for the selected consultation.");
                            btnBookAppointment.setVisibility(View.VISIBLE);
                            btnCancelAppointment.setVisibility(View.GONE); // Hide Cancel Appointment button
                        }
                    } else {
                        Toast.makeText(StudentHomeView.this, "Error fetching appointment status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Show cancel appointment confirmation dialog
    private void showCancelConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Appointment");
        builder.setMessage("Are you sure you want to cancel your appointment?");

        builder.setPositiveButton("Yes", (dialog, which) -> cancelAppointment());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Method to cancel the appointment
    private void cancelAppointment() {
        db.collection("bookedAppointments")
                .whereEqualTo("studentName", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String consultationType = document.getString("consultationType");
                            String reason = document.getString("reason");
                            String time = document.getString("time");
                            String date = document.getString("date");

                            // Move data back to "Appointments" collection
                            Appointment cancelledAppointment = new Appointment(date, time, consultationType, reason);
                            db.collection("Appointments").add(cancelledAppointment)
                                    .addOnSuccessListener(aVoid -> {
                                        // Delete from bookedAppointments collection
                                        db.collection("bookedAppointments").document(document.getId()).delete()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    Toast.makeText(StudentHomeView.this, "Appointment cancelled and returned to available slots.", Toast.LENGTH_SHORT).show();
                                                    checkStudentAppointment(); // Refresh the view
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(StudentHomeView.this, "Failed to cancel appointment.", Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentHomeView.this, "Failed to return appointment to available slots.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(StudentHomeView.this, "Failed to fetch appointment details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Appointment class to represent appointment data
    public static class Appointment {
        private String date;
        private String time;
        private String consultationType;
        private String reason;

        public Appointment() {
            // Default constructor required for Firestore
        }

        public Appointment(String date, String time, String consultationType, String reason) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
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

        public String getReason() {
            return reason;
        }
    }
}

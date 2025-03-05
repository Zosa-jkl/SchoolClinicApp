package com.example.schoolclinicappointment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class NurseHomeActivity extends AppCompatActivity {

    private Button btnAppointmentSetter, btnImmediateCare, btnStudentRecords, btnAvailableAppointments, btnLogout;
    private TextView nurseWelcomeText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_home);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        nurseWelcomeText = findViewById(R.id.nurseWelcomeText); // Reference to TextView
        btnAppointmentSetter = findViewById(R.id.btnAppointmentSetter);
        btnImmediateCare = findViewById(R.id.btnImmediateCare);
        btnStudentRecords = findViewById(R.id.btnStudentRecords);
        btnAvailableAppointments = findViewById(R.id.btnAvailableAppointments);
        btnLogout = findViewById(R.id.btnLogout);

        // Fetch the logged-in nurse's name and set it in the TextView
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            fetchNurseName(user.getUid());
        }

        // Appointment Setter Click
        btnAppointmentSetter.setOnClickListener(view -> {
            Intent intent = new Intent(NurseHomeActivity.this, NurseView.class);
            startActivity(intent);
        });

        // Immediate Health Care Click
        btnImmediateCare.setOnClickListener(view -> {
            Intent intent = new Intent(NurseHomeActivity.this, ImmediateCareActivity.class);
            startActivity(intent);
        });

        // Student Records Click
        btnStudentRecords.setOnClickListener(view -> {
            Intent intent = new Intent(NurseHomeActivity.this, StudentRecordsActivity.class);
            startActivity(intent);
        });

        // Available Appointments Click
        btnAvailableAppointments.setOnClickListener(view -> {
            Intent intent = new Intent(NurseHomeActivity.this, AvailableAppointments.class);
            startActivity(intent);
        });

        // Logout Button Click
        btnLogout.setOnClickListener(view -> showLogoutConfirmation());
    }

    // Method to fetch the nurse's name from Firestore
    private void fetchNurseName(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            String name = document.getString("name");
                            if (name != null) {
                                nurseWelcomeText.setText("Welcome, " + name + "!");
                            }
                        }
                    }
                });
    }

    // Show Logout Confirmation Dialog
    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(NurseHomeActivity.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close current activity
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

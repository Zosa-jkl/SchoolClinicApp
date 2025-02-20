package com.example.schoolclinicappointment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NurseHomeActivity extends AppCompatActivity {

    private Button btnAppointmentSetter, btnImmediateCare, btnStudentRecords, btnAvailableAppointments, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_home);

        // Initialize Buttons
        btnAppointmentSetter = findViewById(R.id.btnAppointmentSetter);
        btnImmediateCare = findViewById(R.id.btnImmediateCare);
        btnStudentRecords = findViewById(R.id.btnStudentRecords);
        btnAvailableAppointments = findViewById(R.id.btnAvailableAppointments);
        btnLogout = findViewById(R.id.btnLogout);

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
        btnAvailableAppointments.setOnClickListener(view -> {
            Intent intent = new Intent(NurseHomeActivity.this, AvailableAppointmentsActivity.class);
            startActivity(intent);
        });

        // Logout Button Click
        btnLogout.setOnClickListener(view -> showLogoutConfirmation());
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

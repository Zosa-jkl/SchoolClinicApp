package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class StudentView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner availableSlotsSpinner;
    private EditText reasonInput;
    private Button confirmBookingButton;

    private String selectedDate = "";
    private ArrayList<String> availableSlots = new ArrayList<>(); // Replace with database data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view);

        calendarView = findViewById(R.id.calendarView);
        availableSlotsSpinner = findViewById(R.id.availableSlotsSpinner);
        reasonInput = findViewById(R.id.reasonInput);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        // Simulate available slots (replace with real database query)
        availableSlots.add("2025-01-30 | 10:00 AM | Annual Physical");
        availableSlots.add("2025-01-30 | 2:00 PM | Dental Examination");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableSlots);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availableSlotsSpinner.setAdapter(spinnerAdapter);

        // Listen for date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            // Filter available slots based on the selected date (implement logic here)
        });

        // Confirm booking button click listener
        confirmBookingButton.setOnClickListener(view -> {
            String selectedSlot = (String) availableSlotsSpinner.getSelectedItem();
            String reason = reasonInput.getText().toString().trim();

            if (selectedDate.isEmpty()) {
                Toast.makeText(StudentView.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSlot == null) {
                Toast.makeText(StudentView.this, "No available slots for the selected date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reason.isEmpty()) {
                Toast.makeText(StudentView.this, "Please enter a reason for your visit.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm the booking (save to database if applicable)
            Toast.makeText(StudentView.this, "Appointment booked successfully for: " + selectedSlot, Toast.LENGTH_SHORT).show();

            // Clear inputs
            reasonInput.setText("");
        });
    }
}

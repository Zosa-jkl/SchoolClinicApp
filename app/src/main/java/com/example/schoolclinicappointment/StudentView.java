package com.example.schoolclinicappointment;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class StudentView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner availableSlotsSpinner;
    private Spinner appointmentTypeSpinner; // New Spinner for appointment types
    private EditText timeSlotInput; // EditText for time slot
    private EditText reasonInput;
    private Button confirmBookingButton;

    private String selectedDate = "";
    private ArrayList<String> availableSlots = new ArrayList<>(); // Available slots for the selected date
    private int selectedHour = 8;  // Default hour to 8 AM
    private int selectedMinute = 0; // Default minute to 0

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view);

        calendarView = findViewById(R.id.calendarView);

        appointmentTypeSpinner = findViewById(R.id.appointmentTypeSpinner); // Initialize the new spinner
        timeSlotInput = findViewById(R.id.timeSlotInput); // EditText for time slot
        reasonInput = findViewById(R.id.reasonInput);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        ImageView backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(StudentView.this, Login.class);
            startActivity(intent);
            finish();
        });

        // Appointment Types for the Spinner
        ArrayList<String> appointmentTypes = new ArrayList<>();
        appointmentTypes.add("Immediate Health Care");
        appointmentTypes.add("Annual Physical");
        appointmentTypes.add("Dental Checkup");

        ArrayAdapter<String> appointmentTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appointmentTypes);
        appointmentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentTypeSpinner.setAdapter(appointmentTypeAdapter);

        // Listen for date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
        });

        // TimePicker Dialog to choose time between 8 AM and 4 PM
        timeSlotInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();

            // Open TimePicker dialog with 24-hour format disabled
            TimePickerDialog timePickerDialog = new TimePickerDialog(StudentView.this,
                    (view1, hourOfDay, minute) -> {
                        if (hourOfDay < 8 || hourOfDay > 16) {
                            Toast.makeText(StudentView.this, "Select a time between 8 AM and 4 PM", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        String time = (selectedHour < 10 ? "0" + selectedHour : selectedHour) + ":" +
                                (selectedMinute < 10 ? "0" + selectedMinute : selectedMinute);
                        timeSlotInput.setText(time);
                    },
                    8, 0, false); // Default start at 8 AM

            timePickerDialog.show();
        });

        // Confirm booking button click listener
        confirmBookingButton.setOnClickListener(view -> {
            String reason = reasonInput.getText().toString().trim();
            String timeSlot = timeSlotInput.getText().toString().trim();
            String selectedAppointmentType = (String) appointmentTypeSpinner.getSelectedItem();

            if (selectedDate.isEmpty()) {
                Toast.makeText(StudentView.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (timeSlot.isEmpty()) {
                Toast.makeText(StudentView.this, "Please select a time slot.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedAppointmentType == null) {
                Toast.makeText(StudentView.this, "Please select an appointment type.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reason.isEmpty()) {
                Toast.makeText(StudentView.this, "Please enter a reason for your visit.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm the booking (save to database if applicable)
            Toast.makeText(StudentView.this, "Appointment booked successfully for: " + selectedAppointmentType + " on " + selectedDate + " at " + timeSlot, Toast.LENGTH_SHORT).show();

            // Clear inputs
            reasonInput.setText("");
            timeSlotInput.setText("");
        });
    }
}

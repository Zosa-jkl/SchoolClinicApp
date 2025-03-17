package com.example.schoolclinicappointment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StudentView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner appointmentTypeSpinner;
    private EditText reasonInput;
    private Button confirmBookingButton;
    private ListView slotsListView;

    private String selectedDate = "";
    private String selectedTimeSlot = "";
    private ArrayList<String> availableSlots = new ArrayList<>();
    private ArrayAdapter<String> slotsAdapter;
    private FirebaseFirestore db;
    private CollectionReference appointmentsRef;
    private CollectionReference bookedAppointmentsRef;
    private CollectionReference usersRef;
    private FirebaseAuth auth;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view);

        // Initialize views
        calendarView = findViewById(R.id.calendarView);
        appointmentTypeSpinner = findViewById(R.id.appointmentTypeSpinner);
        reasonInput = findViewById(R.id.reasonInput);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        slotsListView = findViewById(R.id.slotsListView);
        ImageView backButton = findViewById(R.id.backButton);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        appointmentsRef = db.collection("Appointments");
        bookedAppointmentsRef = db.collection("bookedAppointments");
        usersRef = db.collection("users");
        auth = FirebaseAuth.getInstance();
        studentId = auth.getCurrentUser().getUid(); // Get student ID from Firebase Auth

        // Back button functionality
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(StudentView.this, StudentHomeView.class);
            startActivity(intent);
            finish();
        });

        // Populate Appointment Types Spinner
        ArrayList<String> appointmentTypes = new ArrayList<>();
        appointmentTypes.add("Dental Examination");
        appointmentTypes.add("Annual Physical");

        ArrayAdapter<String> appointmentTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appointmentTypes);
        appointmentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentTypeSpinner.setAdapter(appointmentTypeAdapter);

        // Restrict Calendar to only allow selection from the next day onwards
        long today = System.currentTimeMillis();
        calendarView.setMinDate(today + 86400000); // Add 1 day (24 * 60 * 60 * 1000 ms)

        // Listen for date selection from calendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            fetchAvailableSlots(); // Fetch available slots when the date changes
        });

        // Confirm booking button click listener
        confirmBookingButton.setOnClickListener(view -> {
            String reason = reasonInput.getText().toString().trim();
            String selectedAppointmentType = (String) appointmentTypeSpinner.getSelectedItem();

            if (selectedDate.isEmpty()) {
                Toast.makeText(StudentView.this, "Please select a date.", Toast.LENGTH_SHORT).show();
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

            if (selectedTimeSlot.isEmpty()) {
                Toast.makeText(StudentView.this, "Please select a time slot.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm the booking and delete from Appointments collection
            confirmBooking(selectedAppointmentType, reason);
        });

        // Set up ListView Adapter for available slots
        slotsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, availableSlots);
        slotsListView.setAdapter(slotsAdapter);

        // Set OnItemClickListener for slots ListView
        slotsListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedTimeSlot = availableSlots.get(position);
            Toast.makeText(StudentView.this, "Selected time: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
        });
    }

    // Fetch available slots in ascending order
    // Fetch available slots in ascending order and convert to 12-hour format
    private void fetchAvailableSlots() {
        String selectedAppointmentType = (String) appointmentTypeSpinner.getSelectedItem();

        appointmentsRef.whereEqualTo("consultationType", selectedAppointmentType)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        availableSlots.clear();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            ArrayList<String> sortedSlots = new ArrayList<>();

                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String time = document.getString("time");
                                sortedSlots.add(time); // Collect the raw 24-hour time format
                            }

                            // Sort time slots in ascending order (24-hour format)
                            sortedSlots.sort(String::compareTo);

                            // Convert to 12-hour format with AM/PM
                            for (String time : sortedSlots) {
                                availableSlots.add(convertTo12HourFormat(time));
                            }

                        } else {
                            Toast.makeText(StudentView.this, "No available slots for this date.", Toast.LENGTH_SHORT).show();
                        }
                        slotsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(StudentView.this, "Error fetching available slots.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Confirm booking and save to bookedAppointments collection, then delete from Appointments
    private void confirmBooking(String consultationType, String reason) {
        usersRef.document(studentId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String studentName = document.getString("name");

                            Appointment bookedAppointment = new Appointment(selectedDate, selectedTimeSlot, consultationType, reason, studentName);

                            bookedAppointmentsRef.add(bookedAppointment)
                                    .addOnSuccessListener(documentReference -> {
                                        deleteAppointmentFromAppointments();
                                        Toast.makeText(StudentView.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                        reasonInput.setText("");
                                        selectedTimeSlot = "";
                                        availableSlots.clear();
                                        slotsAdapter.notifyDataSetChanged();
                                        Intent intent = new Intent(StudentView.this, StudentHomeView.class);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentView.this, "Failed to book appointment.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }

    private void deleteAppointmentFromAppointments() {
        appointmentsRef.whereEqualTo("consultationType", appointmentTypeSpinner.getSelectedItem().toString())
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("time", selectedTimeSlot)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            appointmentsRef.document(document.getId()).delete()
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(StudentView.this, "Appointment removed from available slots.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(StudentView.this, "Failed to delete appointment from available slots.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(StudentView.this, "No matching appointment found to delete.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // Helper function to convert 24-hour time to 12-hour format with AM/PM
    private String convertTo12HourFormat(String time24) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(time24));
        } catch (Exception e) {
            e.printStackTrace();
            return time24; // Return original time if conversion fails
        }
    }
    public static class Appointment {
        private String date, time, consultationType, reason, studentName;

        public Appointment() { }

        public Appointment(String date, String time, String consultationType, String reason, String studentName) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
            this.reason = reason;
            this.studentName = studentName;
        }

        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getConsultationType() { return consultationType; }
        public String getReason() { return reason; }
        public String getStudentName() { return studentName; }
    }
}

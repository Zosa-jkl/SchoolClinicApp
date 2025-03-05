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

import java.util.ArrayList;

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
        appointmentTypes.add("Immediate Health Care");

        ArrayAdapter<String> appointmentTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appointmentTypes);
        appointmentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentTypeSpinner.setAdapter(appointmentTypeAdapter);

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
        slotsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, availableSlots);
        slotsListView.setAdapter(slotsAdapter);

        // Set OnItemClickListener for slots ListView
        slotsListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedTimeSlot = availableSlots.get(position);
            Toast.makeText(StudentView.this, "Selected time: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
        });
    }

    // Fetch available slots from Firestore based on date and consultation type
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
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String time = document.getString("time");
                                availableSlots.add(time); // Add the time to the available slots list
                            }
                        } else {
                            Toast.makeText(StudentView.this, "No available slots for this date.", Toast.LENGTH_SHORT).show();
                        }
                        slotsAdapter.notifyDataSetChanged(); // Update the UI with available slots
                    } else {
                        Toast.makeText(StudentView.this, "Error fetching available slots.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Confirm booking and save to bookedAppointments collection, then delete from Appointments
    private void confirmBooking(String consultationType, String reason) {
        // Fetch student name from Firestore
        usersRef.document(studentId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String studentName = document.getString("name");

                            // Create a new appointment document in the 'bookedAppointments' collection
                            Appointment bookedAppointment = new Appointment(selectedDate, selectedTimeSlot, consultationType, reason, studentName);

                            // Add the new appointment to 'bookedAppointments' collection
                            bookedAppointmentsRef.add(bookedAppointment)
                                    .addOnSuccessListener(documentReference -> {
                                        // On successful booking, delete the document from 'Appointments'
                                        deleteAppointmentFromAppointments();

                                        // Confirm booking and show success message
                                        Toast.makeText(StudentView.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                        reasonInput.setText(""); // Clear the reason input
                                        selectedTimeSlot = ""; // Clear selected time slot
                                        availableSlots.clear(); // Clear available slots
                                        slotsAdapter.notifyDataSetChanged(); // Update the UI
                                        Intent intent = new Intent(StudentView.this, StudentHomeView.class);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentView.this, "Failed to book appointment.", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    });
                        }
                    }
                });
    }

    // Delete the selected appointment from the 'Appointments' collection
    private void deleteAppointmentFromAppointments() {
        appointmentsRef.whereEqualTo("consultationType", appointmentTypeSpinner.getSelectedItem().toString())
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("time", selectedTimeSlot)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            appointmentsRef.document(document.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully deleted the appointment from 'Appointments'
                                        Toast.makeText(StudentView.this, "Appointment removed from available slots.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentView.this, "Failed to delete appointment from available slots.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(StudentView.this, "No matching appointment found to delete.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Appointment class to represent appointment data
    public static class Appointment {
        private String date;
        private String time;
        private String consultationType;
        private String reason;
        private String studentName; // Added student name

        public Appointment() {
            // Default constructor required for Firestore
        }

        public Appointment(String date, String time, String consultationType, String reason, String studentName) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
            this.reason = reason;
            this.studentName = studentName;
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

        public String getStudentName() {
            return studentName;
        }
    }
}

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

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        appointmentsRef = db.collection("Appointments");
        bookedAppointmentsRef = db.collection("bookedAppointments");
        usersRef = db.collection("users");
        auth = FirebaseAuth.getInstance();
        studentId = auth.getCurrentUser().getUid();

        // Back button functionality
        backButton.setOnClickListener(view -> {
            startActivity(new Intent(StudentView.this, StudentHomeView.class));
            finish();
        });

        // Restrict Calendar to only allow selection from the next day onwards
        long today = System.currentTimeMillis();
        calendarView.setMinDate(today + 86400000);

        // Listen for date selection from calendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            fetchAvailableSlots();
        });

        // Load available appointment types dynamically
        loadAvailableAppointmentTypes();

        // Confirm booking button click listener
        confirmBookingButton.setOnClickListener(view -> {
            String reason = reasonInput.getText().toString().trim();
            String selectedAppointmentType = (String) appointmentTypeSpinner.getSelectedItem();

            if (selectedDate.isEmpty() || selectedAppointmentType == null || reason.isEmpty() || selectedTimeSlot.isEmpty()) {
                Toast.makeText(StudentView.this, "Please complete all fields.", Toast.LENGTH_SHORT).show();
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
            slotsAdapter.notifyDataSetChanged();
            Toast.makeText(StudentView.this, "Selected time: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAvailableAppointmentTypes() {
        bookedAppointmentsRef.whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<String> availableTypes = new ArrayList<>();
                        availableTypes.add("Dental Examination");
                        availableTypes.add("Annual Physical");

                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            String bookedType = document.getString("consultationType");
                            availableTypes.remove(bookedType);
                        }

                        if (availableTypes.isEmpty()) {
                            Toast.makeText(StudentView.this, "You have already booked both appointment types.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(StudentView.this, StudentHomeView.class));
                            finish();
                        } else {
                            updateSpinner(availableTypes);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(StudentView.this, "Failed to fetch booked appointments.", Toast.LENGTH_SHORT).show());
    }
    private void updateSpinner(ArrayList<String> availableTypes) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(StudentView.this, android.R.layout.simple_spinner_item, availableTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentTypeSpinner.setAdapter(adapter);
    }

    private void fetchAvailableSlots() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(StudentView.this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedAppointmentType = (String) appointmentTypeSpinner.getSelectedItem();
        if (selectedAppointmentType == null) {
            Toast.makeText(StudentView.this, "Please select an appointment type.", Toast.LENGTH_SHORT).show();
            return;
        }

        appointmentsRef.whereEqualTo("consultationType", selectedAppointmentType)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnCompleteListener(task -> {
                    availableSlots.clear();
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            ArrayList<String> sortedSlots = new ArrayList<>();
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                sortedSlots.add(document.getString("time"));
                            }
                            sortedSlots.sort(String::compareTo);
                            for (String time : sortedSlots) {
                                availableSlots.add(convertTo12HourFormat(time));
                            }
                        } else {
                            Toast.makeText(StudentView.this, "No available slots for this date.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(StudentView.this, "Error fetching available slots.", Toast.LENGTH_SHORT).show();
                    }
                    slotsAdapter.notifyDataSetChanged();
                });
    }

    private void confirmBooking(String consultationType, String reason) {
        usersRef.document(studentId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String studentName = document.getString("name");

                            Appointment bookedAppointment = new Appointment(selectedDate, selectedTimeSlot, consultationType, reason, studentName);

                            // Add to bookedAppointments
                            bookedAppointmentsRef.add(bookedAppointment)
                                    .addOnSuccessListener(documentReference -> {
                                        // Now delete the appointment from Appointments collection
                                        deleteAppointmentFromAppointments(consultationType, selectedDate, selectedTimeSlot);

                                        Toast.makeText(StudentView.this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                        reasonInput.setText("");
                                        selectedTimeSlot = "";
                                        availableSlots.clear();
                                        slotsAdapter.notifyDataSetChanged();

                                        // Redirect to student home
                                        Intent intent = new Intent(StudentView.this, StudentHomeView.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(StudentView.this, "Failed to book appointment.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }

    private void deleteAppointmentFromAppointments(String consultationType, String date, String time) {
        appointmentsRef.whereEqualTo("consultationType", consultationType)
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
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

    private String convertTo12HourFormat(String time24) {
        try {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time24));
        } catch (Exception e) {
            return time24;
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

package com.example.schoolclinicappointment;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import android.widget.ArrayAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.ArrayList;
import java.util.Calendar;

public class NurseView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner consultationTypeSpinner;
    private Button addSlotButton;
    private ListView slotsListView;
    private EditText timeSlotInput;

    private String selectedDate = "";
    private ArrayList<String> slotsList = new ArrayList<>();
    private ArrayAdapter<String> slotsAdapter;
    private FirebaseFirestore db;
    private CollectionReference appointmentsRef;

    private int selectedHour = 8;  // Default hour to 8 AM
    private int selectedMinute = 0; // Default minute to 0

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_view);

        // Initialize Views
        calendarView = findViewById(R.id.calendarView);
        consultationTypeSpinner = findViewById(R.id.consultationTypeSpinner);
        timeSlotInput = findViewById(R.id.timeSlotInput); // Input field for time
        addSlotButton = findViewById(R.id.addSlotButton);
        slotsListView = findViewById(R.id.slotsListView);
        ImageView backButton = findViewById(R.id.backButton);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        appointmentsRef = db.collection("Appointments");

        // Set up the slots ListView
        slotsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, slotsList);
        slotsListView.setAdapter(slotsAdapter);

        // Back button functionality
        backButton.setOnClickListener(view -> finish());

        // Restrict CalendarView to only allow selecting the next day onwards
        calendarView.setMinDate(System.currentTimeMillis() + 86400000); // 86400000 ms = 1 day

        // Fetch existing slots from Firestore
        fetchExistingSlots();

        // TimePicker Dialog to choose time slot between 8 AM and 4 PM
        timeSlotInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(NurseView.this,
                    (view1, hourOfDay, minute) -> {
                        if (hourOfDay < 8 || hourOfDay > 16) {
                            Toast.makeText(NurseView.this, "Select a time between 8 AM and 4 PM", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        String time = selectedHour + ":" + (selectedMinute < 10 ? "0" + selectedMinute : selectedMinute);
                        timeSlotInput.setText(time); // Set the selected time in the input field
                    },
                    currentHour, currentMinute, false);
            timePickerDialog.show();
        });

        // Add Slot button click functionality
        addSlotButton.setOnClickListener(view -> {
            String consultationType = consultationTypeSpinner.getSelectedItem().toString();
            String timeSlot = timeSlotInput.getText().toString().trim();

            if (selectedDate.isEmpty()) {
                Toast.makeText(NurseView.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (timeSlot.isEmpty()) {
                Toast.makeText(NurseView.this, "Please select a time slot.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation dialog before adding the slot
            new AlertDialog.Builder(NurseView.this)
                    .setTitle("Confirm Slot")
                    .setMessage("Are you sure you want to add the slot for " + consultationType + " at " + timeSlot + " on " + selectedDate + "?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Add the new appointment slot to Firestore
                            addAppointmentToFirestore(selectedDate, timeSlot, consultationType);

                            // Add the new slot to the ListView
                            String slot = selectedDate + " | " + timeSlot + " | " + consultationType;
                            slotsList.add(slot);
                            slotsAdapter.notifyDataSetChanged();

                            Toast.makeText(NurseView.this, "Slot added successfully!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Listen for date selection from CalendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            fetchExistingSlots(); // Fetch slots after selecting the date
        });
    }

    // Method to fetch existing slots from Firebase
    private void fetchExistingSlots() {
        if (selectedDate.isEmpty()) {
            return;
        }

        String selectedConsultationType = consultationTypeSpinner.getSelectedItem().toString();

        appointmentsRef
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("consultationType", selectedConsultationType) // Filter by consultation type
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        slotsList.clear();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String time = document.getString("time");
                            String consultationType = document.getString("consultationType");
                            if (time != null && consultationType != null) {
                                String slot = selectedDate + " | " + time + " | " + consultationType;
                                slotsList.add(slot);
                            }
                        }
                        slotsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(NurseView.this, "Error fetching slots.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to insert the appointment into Firestore
    private void addAppointmentToFirestore(String date, String time, String consultationType) {
        Appointment newAppointment = new Appointment(date, time, consultationType);

        appointmentsRef.add(newAppointment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NurseView.this, "Appointment added!", Toast.LENGTH_SHORT).show();
                    String slot = date + " | " + time + " | " + consultationType;
                    slotsAdapter.notifyDataSetChanged();  // Update the ListView
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NurseView.this, "Failed to add appointment.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    // Appointment class to represent appointment data
    public static class Appointment {
        private String date;
        private String time;
        private String consultationType;

        public Appointment() {
            // Default constructor required for Firestore
        }

        public Appointment(String date, String time, String consultationType) {
            this.date = date;
            this.time = time;
            this.consultationType = consultationType;
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
    }
}

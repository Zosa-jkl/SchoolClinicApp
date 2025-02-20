package com.example.schoolclinicappointment;

import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Calendar;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;

public class NurseView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner consultationTypeSpinner;
    private EditText timeSlotInput;
    private Button addSlotButton;
    private ListView slotsListView;

    private String selectedDate = "";
    private int selectedHour = 8;  // Default hour to 8 AM
    private int selectedMinute = 0; // Default minute to 0
    private ArrayList<String> slotsList = new ArrayList<>();
    private ArrayAdapter<String> slotsAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_view);

        // Initialize Views
        calendarView = findViewById(R.id.calendarView);
        consultationTypeSpinner = findViewById(R.id.consultationTypeSpinner);
        timeSlotInput = findViewById(R.id.timeSlotInput);
        addSlotButton = findViewById(R.id.addSlotButton);
        slotsListView = findViewById(R.id.slotsListView);
        ImageView backButton = findViewById(R.id.backButton);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Back button functionality
        backButton.setOnClickListener(view -> finish());

        // Populate consultation types in the Spinner
        String[] consultationTypes = {"Annual Physical", "Dental Examination", "Immediate Health Care"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, consultationTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        consultationTypeSpinner.setAdapter(spinnerAdapter);

        // Listen for date selection from CalendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
        });

        // Set up the slots ListView
        slotsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, slotsList);
        slotsListView.setAdapter(slotsAdapter);

        // Fetch existing slots from the database
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

            // Add the new appointment slot into the database
            addAppointmentToDatabase(selectedDate, timeSlot, consultationType);

            // Add the new slot to the ListView
            String slot = selectedDate + " | " + timeSlot + " | " + consultationType;
            slotsList.add(slot);
            slotsAdapter.notifyDataSetChanged();

            Toast.makeText(NurseView.this, "Slot added successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to insert the appointment into the database
    private void addAppointmentToDatabase(String date, String time, String consultationType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Use ContentValues to insert the data into the appointments table
        ContentValues values = new ContentValues();
        values.put("appointment_date", date);
        values.put("appointment_time", time);
        values.put("consultation_type", consultationType);

        long newRowId = db.insert("appointments", null, values);
        if (newRowId == -1) {
            Toast.makeText(NurseView.this, "Failed to add appointment.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch all existing appointments from the database
    private void fetchExistingSlots() {
        ArrayList<String> existingSlots = dbHelper.getAllAppointments();
        slotsList.clear(); // Clear the current list
        slotsList.addAll(existingSlots); // Add all fetched slots to the list
        slotsAdapter.notifyDataSetChanged(); // Notify the adapter that data has changed
    }
}

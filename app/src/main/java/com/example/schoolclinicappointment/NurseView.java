package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class NurseView extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner consultationTypeSpinner;
    private EditText timeSlotInput;
    private Button addSlotButton;
    private ListView slotsListView;

    private String selectedDate = "";
    private ArrayList<String> slotsList = new ArrayList<>();
    private ArrayAdapter<String> slotsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_view);

        calendarView = findViewById(R.id.calendarView);
        consultationTypeSpinner = findViewById(R.id.consultationTypeSpinner);
        timeSlotInput = findViewById(R.id.timeSlotInput);
        addSlotButton = findViewById(R.id.addSlotButton);
        slotsListView = findViewById(R.id.slotsListView);

        // Populate consultation types
        String[] consultationTypes = {"Annual Physical", "Dental Examination", "Immediate Health Care"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, consultationTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        consultationTypeSpinner.setAdapter(spinnerAdapter);

        // Listen for date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
        });

        // Set up slots list adapter
        slotsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, slotsList);
        slotsListView.setAdapter(slotsAdapter);

        // Add slot button click listener
        addSlotButton.setOnClickListener(view -> {
            String timeSlot = timeSlotInput.getText().toString().trim();
            String consultationType = consultationTypeSpinner.getSelectedItem().toString();

            if (selectedDate.isEmpty()) {
                Toast.makeText(NurseView.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (timeSlot.isEmpty()) {
                Toast.makeText(NurseView.this, "Please enter a time slot.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add the slot to the list
            String slot = selectedDate + " | " + timeSlot + " | " + consultationType;
            slotsList.add(slot);
            slotsAdapter.notifyDataSetChanged();

            // Clear inputs
            timeSlotInput.setText("");
            Toast.makeText(NurseView.this, "Slot added successfully!", Toast.LENGTH_SHORT).show();
        });

        // Handle slot deletion (optional)
        slotsListView.setOnItemClickListener((adapterView, view, position, id) -> {
            slotsList.remove(position);
            slotsAdapter.notifyDataSetChanged();
            Toast.makeText(NurseView.this, "Slot removed.", Toast.LENGTH_SHORT).show();
        });
    }
}

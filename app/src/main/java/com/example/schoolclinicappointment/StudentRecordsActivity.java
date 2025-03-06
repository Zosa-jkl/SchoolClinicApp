package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

public class StudentRecordsActivity extends AppCompatActivity {

    private Spinner consultationTypeSpinner;
    private ListView studentRecordsListView;
    private Button backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_records);

        // Initialize views
        consultationTypeSpinner = findViewById(R.id.consultationTypeSpinner);
        studentRecordsListView = findViewById(R.id.studentRecordsListView);
        backButton = findViewById(R.id.backButton);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Populate the spinner with consultation types
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.consultationType, // Ensure this array exists in your strings.xml
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        consultationTypeSpinner.setAdapter(spinnerAdapter);

        // Set listener for consultation type spinner
        consultationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedView, int position, long id) {
                String selectedConsultationType = (String) parentView.getItemAtPosition(position);
                fetchStudentRecords(selectedConsultationType); // Fetch the available records based on selection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle this case if needed
            }
        });

        // Back button functionality
        backButton.setOnClickListener(view -> finish());

        // Default to show "Dental Examination" records
        fetchStudentRecords("Dental Examination");
    }

    // Fetch student records from Firestore based on consultation type
    private void fetchStudentRecords(String consultationType) {
        db.collection("studentRecords")
                .whereEqualTo("consultationType", consultationType) // Filter based on consultation type
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        ArrayList<String> recordsList = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String time = document.getString("time");  // Fetch time from Firestore
                            String date = document.getString("date");  // Fetch date from Firestore
                            String studentName = document.getString("studentName"); // Fetch studentName from Firestore
                            String reason = document.getString("reason"); // Fetch reason from Firestore

                            if (time != null && date != null && studentName != null && reason != null) {
                                String record = date + " | " + time + " | " + consultationType + " | " + studentName + " | " + reason;
                                recordsList.add(record);
                            }
                        }

                        // Set up ListView with available records
                        if (recordsList.isEmpty()) {
                            Toast.makeText(StudentRecordsActivity.this, "No records available for this type.", Toast.LENGTH_SHORT).show();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(StudentRecordsActivity.this, android.R.layout.simple_list_item_1, recordsList);
                        studentRecordsListView.setAdapter(adapter);
                    } else {
                        Toast.makeText(StudentRecordsActivity.this, "Error fetching records.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

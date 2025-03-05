package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private FirebaseAuth mAuth;  // Firebase Authentication instance
    private FirebaseFirestore db;  // Firebase Firestore instance
    private EditText emailField, passwordField, nameField; // Added nameField
    private Spinner roleSpinner;  // Spinner to select the role (nurse/student)
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize FirebaseAuth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailField = findViewById(R.id.editTextTextEmailAddress);
        passwordField = findViewById(R.id.editTextTextPassword);
        nameField = findViewById(R.id.editTextName); // Added reference for name input
        roleSpinner = findViewById(R.id.roleSpinner);  // Spinner for role (nurse/student)
        registerButton = findViewById(R.id.registerButton);

        // Populate the Spinner with the role options ("nurse" or "student")
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.role_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Register Button Click Listener
        registerButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String name = nameField.getText().toString().trim();  // Get name from the nameField
            String role = roleSpinner.getSelectedItem().toString();  // Get role from spinner (nurse/student)

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(Register.this, "Please enter email, password, and name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user with Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Get the current Firebase user
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Save the user's role and name in Firestore
                                saveUserDataInFirestore(user, name, role);
                            }
                        } else {
                            // Registration failed
                            Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void saveUserDataInFirestore(FirebaseUser user, String name, String role) {
        // Create a Map to store the user's data (name and role)
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);  // Save the name
        userMap.put("role", role);  // Save the role ("nurse" or "student")

        // Save the user document in Firestore under the "users" collection
        db.collection("users").document(user.getUid())  // Use Firebase UID as the document ID
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    // Redirect to Login or other activity after successful registration
                    startActivity(new Intent(Register.this, Login.class));
                    finish();  // Close current activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();  // Log the error to help debug
                });
    }
}

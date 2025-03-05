package com.example.schoolclinicappointment;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;  // Firebase Authentication instance
    private FirebaseFirestore db;  // Firebase Firestore instance
    private EditText emailField, passwordField;
    private Button loginButton;
    private TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailField = findViewById(R.id.editTextTextEmailAddress);
        passwordField = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.login);
        registerLink = findViewById(R.id.register);

        // Register link redirects to Register activity
        registerLink.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
            finish();
        });

        // Login button logic
        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Check if email and password fields are filled
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase Authentication: sign in with email and password
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Check if login is successful
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Fetch user role from Firestore
                                db.collection("users").document(user.getUid())
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                DocumentSnapshot document = task1.getResult();
                                                if (document.exists()) {
                                                    // Get the role from Firestore
                                                    String role = document.getString("role");

                                                    // Redirect based on role
                                                    if (role != null) {
                                                        if (role.equals("nurse")) {
                                                            // Redirect to NurseHomeActivity
                                                            startActivity(new Intent(Login.this, NurseHomeActivity.class));
                                                        } else if (role.equals("student")) {
                                                            // Redirect to StudentHomeView
                                                            startActivity(new Intent(Login.this, StudentHomeView.class));
                                                        }
                                                        finish();  // Close Login activity
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(Login.this, "Error fetching user role", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            // Authentication failed
                            Toast.makeText(Login.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}

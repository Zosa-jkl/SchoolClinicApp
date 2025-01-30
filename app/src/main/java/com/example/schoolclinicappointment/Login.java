    package com.example.schoolclinicappointment;

    import android.os.Bundle;

    import androidx.activity.EdgeToEdge;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import android.widget.TextView;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;
    import android.content.Intent;

    public class Login extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_login);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            EditText emailField = findViewById(R.id.editTextTextEmailAddress);
            EditText passwordField = findViewById(R.id.editTextTextPassword);
            TextView statusTextView = findViewById(R.id.status);
            Button loginButton = findViewById(R.id.login);

            String status = getIntent().getStringExtra("status");
            if (status != null) {
                statusTextView.setText(status);
            } else {
                // Handle the case where status is null
                Toast.makeText(this, "Status is missing. Please try again.", Toast.LENGTH_SHORT).show();
                finish(); // Close the Login activity if no status is provided
                return;
            }

            // Hardcoded credentials for demonstration
            String correctEmail = "user";
            String correctPassword = "123";

            loginButton.setOnClickListener(view -> {
                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Local validation
                if (email.equals(correctEmail) && password.equals(correctPassword)) {
                    // Successful login
                    Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                    // Navigate to the appropriate activity based on status
                    Intent intent;
                    switch (status.toLowerCase()) {
                        case "student/faculty":
                            intent = new Intent(Login.this, StudentView.class);
                            break;
                        case "nurse":
                            intent = new Intent(Login.this, NurseView.class);
                            break;
                        default:
                            Toast.makeText(Login.this, "Invalid status", Toast.LENGTH_SHORT).show();
                            return;
                    }
                    startActivity(intent);
                    finish(); // Close the Login activity
                } else {
                    // Login failed
                    Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

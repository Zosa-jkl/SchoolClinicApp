package com.example.schoolclinicappointment;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailField = findViewById(R.id.editTextTextEmailAddress);
        EditText passwordField = findViewById(R.id.editTextTextPassword);
        Button loginButton = findViewById(R.id.login);
        ImageView backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
        TextView register = findViewById(R.id.register);
        register.setOnClickListener(view ->{
            Intent intent = new Intent (Login.this, Register.class);
            startActivity(intent);
            finish();
        });

        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hardcoded credentials
            if (email.equals("nurse") && password.equals("123")) {
                Toast.makeText(Login.this, "Login Successful (Nurse)", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Login.this, NurseView.class);
                startActivity(intent);
                finish();
            } else if (email.equals("student") && password.equals("321")) {
                Toast.makeText(Login.this, "Login Successful (Student/Faculty)", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Login.this, StudentView.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

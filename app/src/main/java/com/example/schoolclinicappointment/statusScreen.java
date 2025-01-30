package com.example.schoolclinicappointment;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class statusScreen extends AppCompatActivity {

    private RadioGroup radioGroup;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_status_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        radioGroup = findViewById(R.id.radioGroup);
        next = findViewById(R.id.next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                int selectedId = radioGroup.getCheckedRadioButtonId();

                if(selectedId != -1){
                    RadioButton selectedRadioButton = findViewById(selectedId);
                    String selectedStatus = selectedRadioButton.getText().toString();

                    Intent intent = new Intent(statusScreen.this, Login.class);
                    intent.putExtra("status", selectedStatus);
                    startActivity(intent);
                } else{
                    Toast.makeText(statusScreen.this, "Please select a status.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
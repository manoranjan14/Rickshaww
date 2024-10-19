package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ChooseRoleActivity extends AppCompatActivity {

    private Button userButton;
    private Button driverButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

        // UI elements
        userButton = findViewById(R.id.user_button);
        driverButton = findViewById(R.id.driver_button);

        // Set onClickListeners for buttons
        userButton.setOnClickListener(view -> {
            // Navigate to User Login (MainActivity)
            Intent intent = new Intent(ChooseRoleActivity.this, MainActivity.class);
            startActivity(intent);
        });

        driverButton.setOnClickListener(view -> {
            // Navigate to Driver Login (DriverLoginActivity)
            Intent intent = new Intent(ChooseRoleActivity.this, DriverLoginActivity.class);
            startActivity(intent);
        });
    }
}

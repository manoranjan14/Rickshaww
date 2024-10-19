package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverRegisterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private FirebaseAuth auth;
    private DatabaseReference driverDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_register);

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance();
        driverDatabaseRef = FirebaseDatabase.getInstance().getReference("drivers"); // Reference to "drivers" node

        // UI elements
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        registerButton = findViewById(R.id.register_button);

        // Register button click
        registerButton.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(DriverRegisterActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registration successful
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveDriverData(user);
                        }
                        Toast.makeText(DriverRegisterActivity.this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show();
                        finish(); // Close DriverRegisterActivity
                    } else {
                        // Registration failed
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed. Please try again.";
                        Toast.makeText(DriverRegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Save driver data to Firebase
    // Save driver data to Firebase
    private void saveDriverData(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        // Create a driver object with relevant details
        Driver driver = new Driver(userId, email);

        // Save driver under "drivers" node using userId as the key
        driverDatabaseRef.child(userId).setValue(driver)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(DriverRegisterActivity.this, "Driver data saved successfully.", Toast.LENGTH_SHORT).show();
                        // Save user type in the database
                        driverDatabaseRef.child(userId).child("userType").setValue("driver");
                        Intent intent = new Intent(DriverRegisterActivity.this, DriverLoginActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(DriverRegisterActivity.this, "Failed to save driver data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}

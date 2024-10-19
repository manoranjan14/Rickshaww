package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private FirebaseAuth auth;
    private DatabaseReference driverDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        driverDatabaseRef = FirebaseDatabase.getInstance().getReference("drivers");

        // UI elements
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);

        // Login button click
        loginButton.setOnClickListener(view -> loginUser());

        // Register button click
        registerButton.setOnClickListener(view -> {
            Intent intent = new Intent(DriverLoginActivity.this, DriverRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Input validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(DriverLoginActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(DriverLoginActivity.this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Login successful
                        FirebaseUser user = auth.getCurrentUser();

                        // Check if user is a driver
                        driverDatabaseRef.child(user.getUid()).child("userType").get().addOnCompleteListener(typeTask -> {
                            if (typeTask.isSuccessful()) {
                                String userType = typeTask.getResult().getValue(String.class);
                                if ("driver".equals(userType)) {
                                    Toast.makeText(DriverLoginActivity.this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();

                                    // Save driver ID
                                    String driverId = user.getUid();
                                    SharedPreferences sharedPreferences = getSharedPreferences("DriverSession", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("driverId", driverId);
                                    editor.putBoolean("isLoggedIn", true);
                                    editor.apply();

                                    // Navigate to MapActivity
                                    Intent intent = new Intent(DriverLoginActivity.this, MapActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(DriverLoginActivity.this, "This account is not a driver.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(DriverLoginActivity.this, "Error retrieving user type.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed. Please try again.";
                        Toast.makeText(DriverLoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

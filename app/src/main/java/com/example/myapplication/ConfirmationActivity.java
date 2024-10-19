package com.example.myapplication;

import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmationActivity extends AppCompatActivity {

    // Firebase database reference
    private DatabaseReference databaseRef;
    private FirebaseAuth auth;
    private ExecutorService executorService;
    private Handler handler;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("ride_requests");
        auth = FirebaseAuth.getInstance();

        // Initialize thread handling
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        // Get the current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // If user is not logged in, handle the case (redirect to login or show error)
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity
            return;
        }

        // Get the userId (UID) of the logged-in user
        String userId = currentUser.getUid();

        // Get the current and drop location from the intent
        double currentLat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        double currentLon = getIntent().getDoubleExtra("CURRENT_LON", 0);
        double dropLat = getIntent().getDoubleExtra("DROP_LAT", 0);
        double dropLon = getIntent().getDoubleExtra("DROP_LON", 0);

        // Log the values for debugging
        Log.d("ConfirmationActivity", "Current Lat: " + currentLat);
        Log.d("ConfirmationActivity", "Current Lon: " + currentLon);
        Log.d("ConfirmationActivity", "Drop Lat: " + dropLat);
        Log.d("ConfirmationActivity", "Drop Lon: " + dropLon);

        // Check if latitude and longitude are valid
        if (currentLat == 0 || currentLon == 0 || dropLat == 0 || dropLon == 0) {
            Toast.makeText(this, "Invalid location data. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Initialize the loading indicator
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar

        // Run Geocoding in background thread
        executorService.execute(() -> {
            String currentPlaceName = getPlaceName(geocoder, currentLat, currentLon);
            String dropPlaceName = getPlaceName(geocoder, dropLat, dropLon);

            // Update the UI on the main thread
            handler.post(() -> {
                EditText currentLocationEditText = findViewById(R.id.current_location);
                EditText dropLocationEditText = findViewById(R.id.drop_location);
                currentLocationEditText.setText(currentPlaceName);
                dropLocationEditText.setText(dropPlaceName);
                progressBar.setVisibility(View.GONE); // Hide the progress bar after fetching place names
            });
        });

        // Set up the button to confirm the ride
        Button confirmRideButton = findViewById(R.id.confirm_ride_button);
        confirmRideButton.setOnClickListener(view -> {
            // Disable the button to prevent multiple submissions
            confirmRideButton.setEnabled(false);

            // Create a RideRequest object with the current user's ID
            RideRequest rideRequest = new RideRequest(currentLat, currentLon, dropLat, dropLon, userId);
            rideRequest.setStatus("Pending"); // Set status as "Pending"

            // Save the RideRequest object to Firebase
            String rideId = databaseRef.push().getKey();
            if (rideId != null) {
                rideRequest.setRideId(rideId);  // Set the ride ID
                databaseRef.child(rideId).setValue(rideRequest)
                        .addOnSuccessListener(aVoid -> {
                            // Ride request saved successfully
                            Log.d("ConfirmationActivity", "Ride Request ID: " + rideId);
                            Toast.makeText(ConfirmationActivity.this, "Ride Confirmed by User: " + currentUser.getEmail(), Toast.LENGTH_LONG).show();

                            // Pass the ride ID and coordinates to WaitingActivity
                            Intent intent = new Intent(ConfirmationActivity.this, WaitingActivity.class);
                            intent.putExtra("RIDE_ID", rideId);
                            intent.putExtra("USER_LATITUDE", currentLat);  // Pass user latitude
                            intent.putExtra("USER_LONGITUDE", currentLon);  // Pass user longitude
                            intent.putExtra("DROP_LATITUDE", dropLat);      // Pass drop latitude
                            intent.putExtra("DROP_LONGITUDE", dropLon);     // Pass drop longitude
                            startActivity(intent);
                            finish(); // Close ConfirmationActivity
                        })
                        .addOnFailureListener(e -> {
                            // Failed to save ride request
                            Toast.makeText(ConfirmationActivity.this, "Failed to confirm ride. Try again.", Toast.LENGTH_SHORT).show();
                            confirmRideButton.setEnabled(true); // Re-enable the button
                        });
            } else {
                // Ride ID generation failed
                Toast.makeText(ConfirmationActivity.this, "Error occurred. Try again.", Toast.LENGTH_SHORT).show();
                confirmRideButton.setEnabled(true); // Re-enable the button
            }
        });

        // Back button functionality
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(view -> {
            Toast.makeText(ConfirmationActivity.this, "Returning to previous screen", Toast.LENGTH_SHORT).show();
            finish(); // Close the current activity and return to the previous one
        });
    }

    // Method to get the place name from the latitude and longitude
    private String getPlaceName(Geocoder geocoder, double latitude, double longitude) {
        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0); // Get the first address line
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error retrieving location";
        }
        return "Unknown location"; // Fallback if no address found
    }
}

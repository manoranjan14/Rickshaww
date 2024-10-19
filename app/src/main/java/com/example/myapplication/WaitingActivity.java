package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaitingActivity extends AppCompatActivity {

    // Firebase database reference
    private DatabaseReference databaseRef;
    private String rideId; // Store the ride ID for cancellation
    private double userLatitude; // User's current latitude
    private double userLongitude; // User's current longitude
    private double dropLatitude; // Drop location latitude
    private double dropLongitude; // Drop location longitude

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference("ride_requests");

        // Get the ride ID from the Intent
        rideId = getIntent().getStringExtra("RIDE_ID");

        // Get user and drop location coordinates
        userLatitude = getIntent().getDoubleExtra("USER_LATITUDE", 0.0);
        userLongitude = getIntent().getDoubleExtra("USER_LONGITUDE", 0.0);
        dropLatitude = getIntent().getDoubleExtra("DROP_LATITUDE", 0.0);
        dropLongitude = getIntent().getDoubleExtra("DROP_LONGITUDE", 0.0);

        // Listen for ride acceptance
        listenForRideAcceptance();

        // Cancel Ride Button
        Button cancelRideButton = findViewById(R.id.cancel_ride_button);
        cancelRideButton.setOnClickListener(view -> {
            if (rideId != null) {
                // Remove the ride from Firebase
                databaseRef.child(rideId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            // Ride cancelled successfully
                            Toast.makeText(WaitingActivity.this, "Ride cancelled successfully.", Toast.LENGTH_SHORT).show();

                            // Redirect to MainActivity
                            Intent intent = new Intent(WaitingActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish(); // Close WaitingActivity
                        })
                        .addOnFailureListener(e -> {
                            // Failed to cancel ride
                            Toast.makeText(WaitingActivity.this, "Failed to cancel ride. Try again.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "No ride to cancel.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForRideAcceptance() {
        if (rideId != null) {
            databaseRef.child(rideId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Check if the ride status is accepted
                        String status = dataSnapshot.child("status").getValue(String.class);
                        if ("Accepted".equals(status)) {
                            // Ride accepted, proceed to ride details
                            Intent intent = new Intent(WaitingActivity.this, RideDetailsActivity.class);
                            intent.putExtra("RIDE_ID", rideId);
                            intent.putExtra("USER_LATITUDE", userLatitude); // Pickup latitude
                            intent.putExtra("USER_LONGITUDE", userLongitude); // Pickup longitude
                            intent.putExtra("DROP_LATITUDE", dropLatitude); // Drop latitude
                            intent.putExtra("DROP_LONGITUDE", dropLongitude); // Drop longitude
                            startActivity(intent);
                            finish(); // Close WaitingActivity
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                    Toast.makeText(WaitingActivity.this, "Error listening for ride status.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Invalid ride ID.", Toast.LENGTH_SHORT).show();
        }
    }
}

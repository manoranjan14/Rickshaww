package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import java.util.List;
import java.util.Locale;
import android.location.Geocoder;
import android.location.Address;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RideDetailsActivity extends AppCompatActivity {

    private static final double COST_PER_KM_IN_RUPEES = 50.0; // Example cost per kilometer in rupees
    private DatabaseReference rideReference; // Firebase reference

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);

        // Initialize Firebase Database reference
        rideReference = FirebaseDatabase.getInstance().getReference("ride_requests");

        // Get data from Intent
        String rideId = getIntent().getStringExtra("RIDE_ID"); // Pass ride ID through Intent
        double userLatitude = getIntent().getDoubleExtra("USER_LATITUDE", 0.0);
        double userLongitude = getIntent().getDoubleExtra("USER_LONGITUDE", 0.0);
        double dropLatitude = getIntent().getDoubleExtra("DROP_LATITUDE", 0.0);
        double dropLongitude = getIntent().getDoubleExtra("DROP_LONGITUDE", 0.0);

        // Calculate the distance
        double distance = calculateDistance(userLatitude, userLongitude, dropLatitude, dropLongitude);
        Log.d("RideDetailsActivity", "Distance: " + distance + " km"); // Log the distance

        // Get location names
        String pickupLocationName = getLocationName(userLatitude, userLongitude);
        String dropLocationName = getLocationName(dropLatitude, dropLongitude);

        double cost = calculateCost(distance);

        // Find TextViews
        TextView pickupLocationTextView = findViewById(R.id.pickup_location);
        TextView dropLocationTextView = findViewById(R.id.drop_location);
        TextView costTextView = findViewById(R.id.cost);

        // Set the pickup and drop location details
        pickupLocationTextView.setText("Pickup Location: " + pickupLocationName);
        dropLocationTextView.setText("Drop Location: " + dropLocationName);
        costTextView.setText("Cost: ₹" + String.format("%.2f", cost)); // Display cost in rupees

        // Check the ride status from Firebase
        listenForRideCompletion(rideId);
    }

    // Check the ride status from Firebase
    private void listenForRideCompletion(String rideId) {
        if (rideId != null) {
            DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_requests").child(rideId);
            rideRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Check if the ride status is completed
                        String status = dataSnapshot.child("status").getValue(String.class);
                        if ("Completed".equals(status)) {
                            // Show alert dialog to inform the user that the ride is completed
                            new AlertDialog.Builder(RideDetailsActivity.this)
                                    .setTitle("Ride Completed")
                                    .setMessage("Your ride has been completed.")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Redirect to Main Activity
                                            Intent intent = new Intent(RideDetailsActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish(); // Close RideDetailsActivity
                                        }
                                    })
                                    .show();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                    Log.e("RideDetailsActivity", "Error listening for ride status: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("RideDetailsActivity", "Invalid ride ID. Cannot listen for ride completion.");
        }
    }

    // Haversine formula to calculate distance between two points in kilometers
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Convert to kilometers
        return distance;
    }

    // Calculate cost based on distance in rupees
    private double calculateCost(double distance) {
        return distance * COST_PER_KM_IN_RUPEES; // Cost calculation in rupees
    }

    // Get location name from latitude and longitude
    private String getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String locationName = "Unknown Location"; // Default if not found
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                locationName = address.getAddressLine(0); // Get the complete address
            }
        } catch (Exception e) {
            Log.e("RideDetailsActivity", "Geocoder failed", e);
        }
        return locationName;
    }

    // Show alert when the ride is completed
    private void showRideCompletedAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ride Completed");
        builder.setMessage("Thank you for using our service! Your ride is completed.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Redirect to MainActivity
                Intent intent = new Intent(RideDetailsActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Finish the current activity
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}

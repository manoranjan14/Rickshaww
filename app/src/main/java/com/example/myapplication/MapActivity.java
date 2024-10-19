package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapActivity";

    private MapView mapView;
    private DatabaseReference databaseRef;
    private ArrayList<RideRequest> availableRides = new ArrayList<>();
    private LocationManager locationManager;
    private String driverId;
    private RideRequest selectedRide;
    private boolean rideInProgress = false;
    private FirebaseAuth auth;
    private Button completeRideButton;
    private FirebaseAuth.AuthStateListener authStateListener;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // Initialize AuthStateListener
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                driverId = user.getUid(); // Get the driver ID from Firebase
                saveDriverSession(driverId); // Save driver ID in SharedPreferences
            } else {
                redirectToLogin(); // No user is signed in
            }
        };

        setContentView(R.layout.activity_map);
        Log.d(TAG, "MapActivity created");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP); // Change overflow icon color to black
        }

        // osmdroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Initialize map view
        mapView = findViewById(R.id.mapview);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(37.7749, -122.4194));

        // Firebase reference for available rides
        databaseRef = FirebaseDatabase.getInstance().getReference("ride_requests");

        // Initialize LocationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Get driver ID from session
        driverId = getDriverSession();

        if (driverId == null) {
            Log.e(TAG, "Driver ID is null. Redirecting to login screen.");
            redirectToLogin(); // If driverId is null, redirect to login screen
            return;
        }

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation(); // Get current location if permission is granted
        }

        fetchAvailableRides(); // Fetch available rides from Firebase

        completeRideButton = findViewById(R.id.completeRideButton);
        completeRideButton.setVisibility(View.GONE);

        completeRideButton.setOnClickListener(v -> completeRide()); // Set click listener for completing a ride
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Close the app or redirect to login if necessary
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private String getDriverSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("DriverSession", MODE_PRIVATE);
        return sharedPreferences.getString("driverId", null);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MapActivity.this, DriverLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void saveDriverSession(String driverId) {
        SharedPreferences sharedPreferences = getSharedPreferences("DriverSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("driverId", driverId);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private void fetchAvailableRides() {
        if (rideInProgress) { return; }

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                availableRides.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    RideRequest rideRequest = snapshot.getValue(RideRequest.class);
                    if (rideRequest != null && "Pending".equals(rideRequest.getStatus())) {
                        availableRides.add(rideRequest);
                    }
                }

                if (!rideInProgress) { displayAvailableRidesOnMap(); }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load rides: " + databaseError.getMessage());
                Toast.makeText(MapActivity.this, "Failed to load rides.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAvailableRidesOnMap() {
        mapView.getOverlays().clear();

        for (RideRequest ride : availableRides) {
            String pickupAddress = getFullAddress(ride.getCurrentLat(), ride.getCurrentLon());

            Marker pickupMarker = new Marker(mapView);
            GeoPoint pickupPoint = new GeoPoint(ride.getCurrentLat(), ride.getCurrentLon());
            pickupMarker.setPosition(pickupPoint);
            pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pickupMarker.setTitle("Pickup Location: " + pickupAddress);
            pickupMarker.setSnippet("Booked by User ID: " + ride.getUserId());

            pickupMarker.setOnMarkerClickListener((marker, mapView) -> {
                selectedRide = ride;
                showRideDetailsDialog();
                return true;
            });

            mapView.getOverlays().add(pickupMarker);
        }

        mapView.invalidate();
    }

    private void showRideDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle("Ride Details");
        builder.setMessage("Pickup Location: " + getFullAddress(selectedRide.getCurrentLat(), selectedRide.getCurrentLon()) + "\n" +
                "Drop Location: " + getFullAddress(selectedRide.getDropLat(), selectedRide.getDropLon()));
        builder.setPositiveButton("Accept Ride", (dialog, which) -> acceptRide());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void acceptRide() {
        rideInProgress = true;
        completeRideButton.setVisibility(View.VISIBLE);

        if (selectedRide != null) {
            selectedRide.setStatus("Accepted");
            databaseRef.child(selectedRide.getRideId()).setValue(selectedRide)
                    .addOnSuccessListener(aVoid -> Toast.makeText(MapActivity.this, "Ride Accepted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update ride status: " + e.getMessage());
                        Toast.makeText(MapActivity.this, "Failed to update ride status.", Toast.LENGTH_SHORT).show();
                    });
        }

        displayDropLocationOnMap();
    }

    private void displayDropLocationOnMap() {
        mapView.getOverlays().clear();

        Marker pickupMarker = new Marker(mapView);
        GeoPoint pickupPoint = new GeoPoint(selectedRide.getCurrentLat(), selectedRide.getCurrentLon());
        pickupMarker.setPosition(pickupPoint);
        pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pickupMarker.setTitle("Pickup Location");
        mapView.getOverlays().add(pickupMarker);

        Marker dropMarker = new Marker(mapView);
        GeoPoint dropPoint = new GeoPoint(selectedRide.getDropLat(), selectedRide.getDropLon());
        dropMarker.setPosition(dropPoint);
        dropMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        dropMarker.setTitle("Drop Location");
        mapView.getOverlays().add(dropMarker);

        mapView.invalidate();
    }

    private void completeRide() {
        rideInProgress = false;
        completeRideButton.setVisibility(View.GONE);

        if (selectedRide != null) {
            selectedRide.setStatus("Completed");
            databaseRef.child(selectedRide.getRideId()).setValue(selectedRide)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MapActivity.this, "Ride Completed", Toast.LENGTH_SHORT).show();
                        selectedRide = null;
                        fetchAvailableRides();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update ride status: " + e.getMessage());
                        Toast.makeText(MapActivity.this, "Failed to update ride status.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) { updateLocationOnMap(location); }
    }

    private void updateLocationOnMap(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        GeoPoint currentPoint = new GeoPoint(lat, lon);

        mapView.getController().setCenter(currentPoint);
        mapView.getController().setZoom(17.0);

        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker && "Driver Location".equals(((Marker) overlay).getTitle()));

        Marker currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setPosition(currentPoint);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentLocationMarker.setTitle("Driver Location");

        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_driver_location));
        currentLocationMarker.setInfoWindow(null);

        mapView.getOverlays().add(currentLocationMarker);
        mapView.invalidate();
    }


    private String getFullAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        String address = "";

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                address = addr.getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get address: " + e.getMessage());
        }

        return address;
    }

    @Override public void onLocationChanged(@NonNull Location location) { updateLocationOnMap(location); }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override public void onProviderEnabled(@NonNull String provider) {}

    @Override public void onProviderDisabled(@NonNull String provider) {}

    @Override protected void onStart() { super.onStart(); auth.addAuthStateListener(authStateListener); }

    @Override protected void onStop() { super.onStop(); if (authStateListener != null) { auth.removeAuthStateListener(authStateListener); } }

    @Override protected void onDestroy() { super.onDestroy(); if (locationManager != null) { locationManager.removeUpdates(this); } }

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.menu_main, menu); return true; }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) { int id = item.getItemId();

        if (id == R.id.action_previous_rides) { Toast.makeText(this, "Previous Rides clicked", Toast.LENGTH_SHORT).show(); Intent intent = new Intent(MapActivity.this, DriverPreviousRidesActivity.class); startActivity(intent); return true; } else if (id == R.id.action_logout) { FirebaseAuth.getInstance().signOut();

            SharedPreferences sharedPreferences = getSharedPreferences("DriverSession", MODE_PRIVATE); SharedPreferences.Editor editor = sharedPreferences.edit(); editor.putBoolean("isLoggedIn", false); editor.apply();

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show(); startActivity(new Intent(MapActivity.this, DriverLoginActivity.class)); finish(); return true; } return super.onOptionsItemSelected(item); }
}
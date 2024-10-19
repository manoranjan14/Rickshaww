package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker dropMarker;
    private FloatingActionButton fabCurrentLocation;
    private GeoPoint currentLocation;
    private Geocoder geocoder;
    private FirebaseAuth auth;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // If no user is logged in, redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Prevent returning to MainActivity without login
        }

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        }


        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

        // Set up the map
        mapView = findViewById(R.id.map);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0); // Set default zoom level

        // Add the compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        mapView.getOverlays().add(compassOverlay);
        compassOverlay.enableCompass();

        // Set up MyLocation overlay
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
            centerMapOnCurrentLocation(); // Center map on current location
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        mapView.getOverlays().add(myLocationOverlay);

        // Set up the floating button for current location
        fabCurrentLocation = findViewById(R.id.fab_current_location);
        fabCurrentLocation.setOnClickListener(view -> {
            if (myLocationOverlay.getMyLocation() != null) {
                currentLocation = myLocationOverlay.getMyLocation();
                mapView.getController().animateTo(currentLocation);
                mapView.getController().setZoom(18.0);
                Toast.makeText(MainActivity.this, "Centered on current location", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize GestureDetector for double-tap detection
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                showDropPin(geoPoint); // Handle drop pin placement
                return true;
            }
        });

        // Set the touch listener for the map to handle double-tap events
        mapView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));


    }
    private void centerMapOnCurrentLocation() {
        GeoPoint currentLocation = myLocationOverlay.getMyLocation();
        if (currentLocation != null) {
            mapView.getController().setCenter(currentLocation);
            mapView.getController().setZoom(18.0); // Adjust zoom level if necessary
            Toast.makeText(this, "Map centered on current location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to add a marker for the drop location
    private void showDropPin(GeoPoint geoPoint) {
        // Remove existing drop marker if present
        if (dropMarker != null) {
            mapView.getOverlays().remove(dropMarker);
        }

        // Get the place name using Geocoder
        String placeName = getPlaceName(geoPoint);

        // Create a new drop marker
        dropMarker = new Marker(mapView);
        dropMarker.setPosition(geoPoint);
        dropMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        dropMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.marker_default)); // Use your custom marker icon
        dropMarker.setTitle(placeName != null ? placeName : "Drop Location");
        dropMarker.setSubDescription("Lat: " + geoPoint.getLatitude() + ", Lon: " + geoPoint.getLongitude());

        // Add the drop marker to the map
        mapView.getOverlays().add(dropMarker);
        mapView.invalidate(); // Refresh the map to show the marker

        // Update the TextView with the drop location's latitude and longitude
        TextView locationInfo = findViewById(R.id.location_info);
        locationInfo.setText("Drop Location: " + (placeName != null ? placeName : "Lat " + geoPoint.getLatitude() + ", Lon " + geoPoint.getLongitude()));

        // Draw a line to connect current location to drop location
        if (currentLocation != null) {
            drawLine(currentLocation, geoPoint);
        } else {
            Toast.makeText(MainActivity.this, "Current location not available for line drawing", Toast.LENGTH_SHORT).show();
        }

        // Navigate to ConfirmationActivity
        navigateToConfirmation(currentLocation, geoPoint);
    }

    // Method to get the place name from coordinates
    private String getPlaceName(GeoPoint geoPoint) {
        try {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getFeatureName(); // Get the name of the place
            }
        } catch (IOException e) {
            Log.e("GeocoderError", "Geocoder service not available", e);
        }
        return null; // Return null if no place name is found
    }

    // Method to draw a line between two GeoPoints
    private void drawLine(GeoPoint start, GeoPoint end) {
        Polyline line = new Polyline();
        line.setPoints(Arrays.asList(start, end));
        line.setColor(Color.BLUE);
        line.setWidth(5f);
        mapView.getOverlays().add(line);
        mapView.invalidate();
    }

    // Method to navigate to the confirmation screen
    private void navigateToConfirmation(GeoPoint current, GeoPoint drop) {
        if (current != null && drop != null) {
            Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra("CURRENT_LAT", current.getLatitude());
            intent.putExtra("CURRENT_LON", current.getLongitude());
            intent.putExtra("DROP_LAT", drop.getLatitude());
            intent.putExtra("DROP_LON", drop.getLongitude());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Cannot navigate: Current or drop location is null", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume(); // Refresh the map on resume
        currentLocation = myLocationOverlay.getMyLocation(); // Store current location
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause(); // Pause the map on pause
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myLocationOverlay.disableMyLocation(); // Disable location when activity is destroyed
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_previous_rides) {
            // Handle Previous Rides action
            Toast.makeText(this, "Previous Rides clicked", Toast.LENGTH_SHORT).show();
            // Navigate to Previous Rides Activity
            Intent intent = new Intent(MainActivity.this, PreviousRidesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            // Handle Logout action
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            // Redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Close MainActivity
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                myLocationOverlay.enableMyLocation(); // Enable location if permission is granted
                centerMapOnCurrentLocation(); // Center map on current location after permission is granted
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

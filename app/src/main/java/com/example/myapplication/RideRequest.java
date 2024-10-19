package com.example.myapplication;

public class RideRequest {
    private double currentLat;
    private double currentLon;
    private double dropLat;
    private double dropLon;
    private String userId;
    private String rideId;
    private String status;

    // No-argument constructor (required for Firebase)
    public RideRequest() {
        // This is needed for Firebase deserialization
    }

    // Parameterized constructor
    public RideRequest(double currentLat, double currentLon, double dropLat, double dropLon, String userId) {
        this.currentLat = currentLat;
        this.currentLon = currentLon;
        this.dropLat = dropLat;
        this.dropLon = dropLon;
        this.userId = userId;
        this.status = "Pending"; // Default status
    }

    // Getters and setters
    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(double currentLon) {
        this.currentLon = currentLon;
    }

    public double getDropLat() {
        return dropLat;
    }

    public void setDropLat(double dropLat) {
        this.dropLat = dropLat;
    }

    public double getDropLon() {
        return dropLon;
    }

    public void setDropLon(double dropLon) {
        this.dropLon = dropLon;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RideRequest{" +
                "currentLat=" + currentLat +
                ", currentLon=" + currentLon +
                ", dropLat=" + dropLat +
                ", dropLon=" + dropLon +
                ", userId='" + userId + '\'' +
                ", rideId='" + rideId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

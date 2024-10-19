package com.example.myapplication;

public class Driver {
    public String userId;
    public String email;

    public Driver() {
        // Default constructor required for calls to DataSnapshot.getValue(Driver.class)
    }

    public Driver(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    // Getters and Setters (optional if needed)
}

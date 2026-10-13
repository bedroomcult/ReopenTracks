package com.reopentracks.data.models;

public record HeartRate(float value) {

    public static HeartRate of(float value) {
        return new HeartRate(value);
    }

    public float getBPM() {
        return value;
    }
}

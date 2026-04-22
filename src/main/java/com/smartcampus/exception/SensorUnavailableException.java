package com.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String currentStatus;

    public SensorUnavailableException(String sensorId, String currentStatus) {
        super("Sensor " + sensorId + " cannot accept readings while status is " + currentStatus);
        this.sensorId = sensorId;
        this.currentStatus = currentStatus;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}

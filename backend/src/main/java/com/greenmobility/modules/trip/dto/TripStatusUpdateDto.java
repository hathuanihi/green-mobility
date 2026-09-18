package com.greenmobility.modules.trip.dto;

import com.greenmobility.modules.trip.entity.TripStatus;

import java.time.Instant;
import java.util.UUID;

public class TripStatusUpdateDto {

    private UUID tripId;
    private TripStatus status;
    private String message;
    private DriverSummaryDto driver;
    private String cancelledBy;
    private Instant timestamp = Instant.now();

    public TripStatusUpdateDto() {}

    public TripStatusUpdateDto(UUID tripId, TripStatus status, String message, DriverSummaryDto driver) {
        this.tripId = tripId;
        this.status = status;
        this.message = message;
        this.driver = driver;
        this.timestamp = Instant.now();
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DriverSummaryDto getDriver() {
        return driver;
    }

    public void setDriver(DriverSummaryDto driver) {
        this.driver = driver;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

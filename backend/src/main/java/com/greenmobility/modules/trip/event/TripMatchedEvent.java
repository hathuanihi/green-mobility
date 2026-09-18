package com.greenmobility.modules.trip.event;

import java.io.Serializable;
import java.util.UUID;

public class TripMatchedEvent implements Serializable {

    private UUID tripId;
    private String tripCode;
    private UUID customerId;
    private UUID driverId;
    private UUID vehicleId;
    private long matchedAt;

    public TripMatchedEvent() {}

    public TripMatchedEvent(UUID tripId, String tripCode, UUID customerId, UUID driverId, UUID vehicleId, long matchedAt) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.customerId = customerId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.matchedAt = matchedAt;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public long getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(long matchedAt) {
        this.matchedAt = matchedAt;
    }
}

package com.greenmobility.modules.trip.event;

import java.io.Serializable;
import java.util.UUID;

public class TripCancelledEvent implements Serializable {

    private UUID tripId;
    private String tripCode;
    private String cancelledBy;
    private String cancelReason;
    private UUID driverId;
    private long cancelledAt;

    public TripCancelledEvent() {}

    public TripCancelledEvent(UUID tripId, String tripCode, String cancelledBy, String cancelReason, UUID driverId, long cancelledAt) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.driverId = driverId;
        this.cancelledAt = cancelledAt;
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

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public long getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(long cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}

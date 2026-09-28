package com.greenmobility.modules.trip.event;

import java.io.Serializable;
import java.util.UUID;

public class TripDispatchedEvent implements Serializable {

    private UUID tripId;
    private UUID driverId;
    private int tier;
    private long dispatchedAt;

    public TripDispatchedEvent() {}

    public TripDispatchedEvent(UUID tripId, UUID driverId, int tier, long dispatchedAt) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.tier = tier;
        this.dispatchedAt = dispatchedAt;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public long getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(long dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}

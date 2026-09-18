package com.greenmobility.modules.trip.event;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public class TripRequestedEvent implements Serializable {

    private UUID tripId;
    private String tripCode;
    private UUID customerId;
    private VehicleType vehicleType;
    private double pickupLat;
    private double pickupLng;
    private String pickupAddress;
    private double dropoffLat;
    private double dropoffLng;
    private String dropoffAddress;
    private int estimatedDistanceMeters;
    private BigDecimal fareAmount;

    public TripRequestedEvent() {}

    public TripRequestedEvent(UUID tripId, String tripCode, UUID customerId, VehicleType vehicleType,
                              double pickupLat, double pickupLng, String pickupAddress,
                              double dropoffLat, double dropoffLng, String dropoffAddress,
                              int estimatedDistanceMeters, BigDecimal fareAmount) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.customerId = customerId;
        this.vehicleType = vehicleType;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.pickupAddress = pickupAddress;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.dropoffAddress = dropoffAddress;
        this.estimatedDistanceMeters = estimatedDistanceMeters;
        this.fareAmount = fareAmount;
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

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public double getDropoffLat() {
        return dropoffLat;
    }

    public void setDropoffLat(double dropoffLat) {
        this.dropoffLat = dropoffLat;
    }

    public double getDropoffLng() {
        return dropoffLng;
    }

    public void setDropoffLng(double dropoffLng) {
        this.dropoffLng = dropoffLng;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public int getEstimatedDistanceMeters() {
        return estimatedDistanceMeters;
    }

    public void setEstimatedDistanceMeters(int estimatedDistanceMeters) {
        this.estimatedDistanceMeters = estimatedDistanceMeters;
    }

    public BigDecimal getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(BigDecimal fareAmount) {
        this.fareAmount = fareAmount;
    }
}

package com.greenmobility.modules.trip.dto;

import com.greenmobility.modules.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DriverTripResponseDto {

    private UUID tripId;
    private String tripCode;
    private TripStatus status;
    private String customerName;
    private String customerPhone;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;
    private Double estimatedDistanceKm;
    private BigDecimal netIncomeVnd;
    private BigDecimal co2SavedGrams;
    private Instant matchedAt;

    public DriverTripResponseDto() {}

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

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(Double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public Double getDropoffLat() {
        return dropoffLat;
    }

    public void setDropoffLat(Double dropoffLat) {
        this.dropoffLat = dropoffLat;
    }

    public Double getDropoffLng() {
        return dropoffLng;
    }

    public void setDropoffLng(Double dropoffLng) {
        this.dropoffLng = dropoffLng;
    }

    public Double getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(Double estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public BigDecimal getNetIncomeVnd() {
        return netIncomeVnd;
    }

    public void setNetIncomeVnd(BigDecimal netIncomeVnd) {
        this.netIncomeVnd = netIncomeVnd;
    }

    public BigDecimal getCo2SavedGrams() {
        return co2SavedGrams;
    }

    public void setCo2SavedGrams(BigDecimal co2SavedGrams) {
        this.co2SavedGrams = co2SavedGrams;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Instant matchedAt) {
        this.matchedAt = matchedAt;
    }
}

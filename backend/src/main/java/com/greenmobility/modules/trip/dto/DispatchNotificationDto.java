package com.greenmobility.modules.trip.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DispatchNotificationDto {

    private UUID tripId;
    private String tripCode;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private Double distanceToPickupKm;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;
    private Double tripDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal estimatedEarningsVnd;
    private BigDecimal co2SavedGrams;
    private Integer countdownSeconds = 15;

    public DispatchNotificationDto() {}

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

    public Double getDistanceToPickupKm() {
        return distanceToPickupKm;
    }

    public void setDistanceToPickupKm(Double distanceToPickupKm) {
        this.distanceToPickupKm = distanceToPickupKm;
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

    public Double getTripDistanceKm() {
        return tripDistanceKm;
    }

    public void setTripDistanceKm(Double tripDistanceKm) {
        this.tripDistanceKm = tripDistanceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public BigDecimal getEstimatedEarningsVnd() {
        return estimatedEarningsVnd;
    }

    public void setEstimatedEarningsVnd(BigDecimal estimatedEarningsVnd) {
        this.estimatedEarningsVnd = estimatedEarningsVnd;
    }

    public BigDecimal getCo2SavedGrams() {
        return co2SavedGrams;
    }

    public void setCo2SavedGrams(BigDecimal co2SavedGrams) {
        this.co2SavedGrams = co2SavedGrams;
    }

    public Integer getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(Integer countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }
}

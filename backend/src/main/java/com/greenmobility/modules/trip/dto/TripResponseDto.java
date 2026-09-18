package com.greenmobility.modules.trip.dto;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.entity.PaymentMethod;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TripResponseDto {

    private UUID tripId;
    private String tripCode;
    private TripStatus status;
    private VehicleType vehicleType;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;
    private BigDecimal fareAmountVnd;
    private BigDecimal discountAmountVnd;
    private BigDecimal finalAmountVnd;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal co2SavedGrams;
    private DriverSummaryDto driver;
    private String cancelReason;
    private String cancelledBy;
    private Instant requestedAt;
    private Instant matchedAt;

    public TripResponseDto() {}

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

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
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

    public BigDecimal getFareAmountVnd() {
        return fareAmountVnd;
    }

    public void setFareAmountVnd(BigDecimal fareAmountVnd) {
        this.fareAmountVnd = fareAmountVnd;
    }

    public BigDecimal getDiscountAmountVnd() {
        return discountAmountVnd;
    }

    public void setDiscountAmountVnd(BigDecimal discountAmountVnd) {
        this.discountAmountVnd = discountAmountVnd;
    }

    public BigDecimal getFinalAmountVnd() {
        return finalAmountVnd;
    }

    public void setFinalAmountVnd(BigDecimal finalAmountVnd) {
        this.finalAmountVnd = finalAmountVnd;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Double getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(Double estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public BigDecimal getCo2SavedGrams() {
        return co2SavedGrams;
    }

    public void setCo2SavedGrams(BigDecimal co2SavedGrams) {
        this.co2SavedGrams = co2SavedGrams;
    }

    public DriverSummaryDto getDriver() {
        return driver;
    }

    public void setDriver(DriverSummaryDto driver) {
        this.driver = driver;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Instant matchedAt) {
        this.matchedAt = matchedAt;
    }
}

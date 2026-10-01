package com.greenmobility.modules.trip.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for trip completion summary.
 * Returned when a driver completes a trip and broadcast via WebSocket STOMP.
 */
public class TripCompleteSummaryDto {

    private UUID tripId;
    private String tripCode;
    private String status;
    private Instant completedAt;

    // Flat fields requested in task
    private Integer actualDistanceM;
    private Integer actualDurationS;
    private BigDecimal co2SavedGrams;

    private TripSummaryDetail tripSummary;
    private DriverEarningsDetail driverEarnings;

    public TripCompleteSummaryDto() {}

    public TripCompleteSummaryDto(UUID tripId, String tripCode, String status, Instant completedAt,
                                  TripSummaryDetail tripSummary, DriverEarningsDetail driverEarnings) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.status = status;
        this.completedAt = completedAt;
        this.tripSummary = tripSummary;
        this.driverEarnings = driverEarnings;
        if (tripSummary != null) {
            this.actualDistanceM = tripSummary.getActualDistanceM();
            this.actualDurationS = tripSummary.getActualDurationS();
            this.co2SavedGrams = tripSummary.getCo2SavedGrams();
        }
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Integer getActualDistanceM() { return actualDistanceM; }
    public void setActualDistanceM(Integer actualDistanceM) { this.actualDistanceM = actualDistanceM; }

    public Integer getActualDurationS() { return actualDurationS; }
    public void setActualDurationS(Integer actualDurationS) { this.actualDurationS = actualDurationS; }

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public TripSummaryDetail getTripSummary() { return tripSummary; }
    public void setTripSummary(TripSummaryDetail tripSummary) { this.tripSummary = tripSummary; }

    public DriverEarningsDetail getDriverEarnings() { return driverEarnings; }
    public void setDriverEarnings(DriverEarningsDetail driverEarnings) { this.driverEarnings = driverEarnings; }

    public static class TripSummaryDetail {
        private String tripCode;
        private String pickupAddress;
        private String dropoffAddress;
        private Integer estimatedDistanceM;
        private Integer actualDistanceM;
        private Integer estimatedDurationS;
        private Integer actualDurationS;
        private BigDecimal fareAmountVnd;
        private BigDecimal finalAmountVnd;
        private String paymentMethod;
        private String paymentStatus;
        private BigDecimal co2SavedGrams;
        private String customerName;
        private String driverName;

        public TripSummaryDetail() {}

        public String getTripCode() { return tripCode; }
        public void setTripCode(String tripCode) { this.tripCode = tripCode; }

        public String getPickupAddress() { return pickupAddress; }
        public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

        public String getDropoffAddress() { return dropoffAddress; }
        public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

        public Integer getEstimatedDistanceM() { return estimatedDistanceM; }
        public void setEstimatedDistanceM(Integer estimatedDistanceM) { this.estimatedDistanceM = estimatedDistanceM; }

        public Integer getActualDistanceM() { return actualDistanceM; }
        public void setActualDistanceM(Integer actualDistanceM) { this.actualDistanceM = actualDistanceM; }

        public Integer getEstimatedDurationS() { return estimatedDurationS; }
        public void setEstimatedDurationS(Integer estimatedDurationS) { this.estimatedDurationS = estimatedDurationS; }

        public Integer getActualDurationS() { return actualDurationS; }
        public void setActualDurationS(Integer actualDurationS) { this.actualDurationS = actualDurationS; }

        public BigDecimal getFareAmountVnd() { return fareAmountVnd; }
        public void setFareAmountVnd(BigDecimal fareAmountVnd) { this.fareAmountVnd = fareAmountVnd; }

        public BigDecimal getFinalAmountVnd() { return finalAmountVnd; }
        public void setFinalAmountVnd(BigDecimal finalAmountVnd) { this.finalAmountVnd = finalAmountVnd; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

        public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
        public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getDriverName() { return driverName; }
        public void setDriverName(String driverName) { this.driverName = driverName; }
    }

    public static class DriverEarningsDetail {
        private BigDecimal grossAmountVnd;
        private BigDecimal platformFeeVnd;
        private BigDecimal netEarningsVnd;

        public DriverEarningsDetail() {}

        public DriverEarningsDetail(BigDecimal grossAmountVnd, BigDecimal platformFeeVnd, BigDecimal netEarningsVnd) {
            this.grossAmountVnd = grossAmountVnd;
            this.platformFeeVnd = platformFeeVnd;
            this.netEarningsVnd = netEarningsVnd;
        }

        public BigDecimal getGrossAmountVnd() { return grossAmountVnd; }
        public void setGrossAmountVnd(BigDecimal grossAmountVnd) { this.grossAmountVnd = grossAmountVnd; }

        public BigDecimal getPlatformFeeVnd() { return platformFeeVnd; }
        public void setPlatformFeeVnd(BigDecimal platformFeeVnd) { this.platformFeeVnd = platformFeeVnd; }

        public BigDecimal getNetEarningsVnd() { return netEarningsVnd; }
        public void setNetEarningsVnd(BigDecimal netEarningsVnd) { this.netEarningsVnd = netEarningsVnd; }
    }
}

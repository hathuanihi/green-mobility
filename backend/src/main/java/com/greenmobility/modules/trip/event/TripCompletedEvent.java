package com.greenmobility.modules.trip.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ event published when a trip is completed.
 * Consumed by Sprint 4 (Carbon Engine) and Sprint 5 (Payment) modules.
 * Routing key: trip.event.completed
 */
public class TripCompletedEvent implements Serializable {

    private UUID tripId;
    private String tripCode;
    private UUID customerId;
    private UUID driverId;
    private UUID vehicleId;
    private String vehicleType;
    private Integer estimatedDistanceM;
    private Integer actualDistanceM;
    private Integer estimatedDurationS;
    private Integer actualDurationS;
    private BigDecimal fareAmountVnd;
    private BigDecimal finalAmountVnd;
    private String paymentMethod;
    private BigDecimal co2SavedGrams;
    private Instant startedAt;
    private Instant completedAt;

    public TripCompletedEvent() {}

    public TripCompletedEvent(UUID tripId, String tripCode, UUID customerId, UUID driverId,
                              UUID vehicleId, String vehicleType,
                              Integer estimatedDistanceM, Integer actualDistanceM,
                              Integer estimatedDurationS, Integer actualDurationS,
                              BigDecimal fareAmountVnd, BigDecimal finalAmountVnd,
                              String paymentMethod, BigDecimal co2SavedGrams,
                              Instant startedAt, Instant completedAt) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.customerId = customerId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.estimatedDistanceM = estimatedDistanceM;
        this.actualDistanceM = actualDistanceM;
        this.estimatedDurationS = estimatedDurationS;
        this.actualDurationS = actualDurationS;
        this.fareAmountVnd = fareAmountVnd;
        this.finalAmountVnd = finalAmountVnd;
        this.paymentMethod = paymentMethod;
        this.co2SavedGrams = co2SavedGrams;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

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

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

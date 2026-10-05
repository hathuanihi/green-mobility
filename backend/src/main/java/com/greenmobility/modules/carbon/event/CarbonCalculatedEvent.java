package com.greenmobility.modules.carbon.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when carbon emission reductions are calculated for a completed trip.
 * Consumed by Sprint 6 (Double-entry Ledger & Incentive Module).
 * Routing key: carbon.event.calculated
 */
public class CarbonCalculatedEvent implements Serializable {

    private UUID tripId;
    private String tripCode;
    private UUID customerId;
    private UUID driverId;
    private BigDecimal co2SavedGrams;
    private BigDecimal baselineGasolineCo2Grams;
    private BigDecimal evEmittedCo2Grams;
    private BigDecimal carbonCreditsEarned;
    private Integer loyaltyPointsEarned;
    private String shareableSlug;
    private Instant calculatedAt;

    public CarbonCalculatedEvent() {}

    public CarbonCalculatedEvent(UUID tripId, String tripCode, UUID customerId, UUID driverId,
                                 BigDecimal co2SavedGrams, BigDecimal baselineGasolineCo2Grams,
                                 BigDecimal evEmittedCo2Grams, BigDecimal carbonCreditsEarned,
                                 Integer loyaltyPointsEarned, String shareableSlug, Instant calculatedAt) {
        this.tripId = tripId;
        this.tripCode = tripCode;
        this.customerId = customerId;
        this.driverId = driverId;
        this.co2SavedGrams = co2SavedGrams;
        this.baselineGasolineCo2Grams = baselineGasolineCo2Grams;
        this.evEmittedCo2Grams = evEmittedCo2Grams;
        this.carbonCreditsEarned = carbonCreditsEarned;
        this.loyaltyPointsEarned = loyaltyPointsEarned;
        this.shareableSlug = shareableSlug;
        this.calculatedAt = calculatedAt;
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

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public BigDecimal getBaselineGasolineCo2Grams() { return baselineGasolineCo2Grams; }
    public void setBaselineGasolineCo2Grams(BigDecimal baselineGasolineCo2Grams) { this.baselineGasolineCo2Grams = baselineGasolineCo2Grams; }

    public BigDecimal getEvEmittedCo2Grams() { return evEmittedCo2Grams; }
    public void setEvEmittedCo2Grams(BigDecimal evEmittedCo2Grams) { this.evEmittedCo2Grams = evEmittedCo2Grams; }

    public BigDecimal getCarbonCreditsEarned() { return carbonCreditsEarned; }
    public void setCarbonCreditsEarned(BigDecimal carbonCreditsEarned) { this.carbonCreditsEarned = carbonCreditsEarned; }

    public Integer getLoyaltyPointsEarned() { return loyaltyPointsEarned; }
    public void setLoyaltyPointsEarned(Integer loyaltyPointsEarned) { this.loyaltyPointsEarned = loyaltyPointsEarned; }

    public String getShareableSlug() { return shareableSlug; }
    public void setShareableSlug(String shareableSlug) { this.shareableSlug = shareableSlug; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}

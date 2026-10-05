package com.greenmobility.modules.carbon.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_impact_receipts")
public class TripImpactReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id", unique = true, nullable = false)
    private UUID tripId;

    @Column(name = "co2_saved_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal co2SavedGrams;

    @Column(name = "baseline_gasoline_co2_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal baselineGasolineCo2Grams;

    @Column(name = "ev_emitted_co2_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal evEmittedCo2Grams;

    @Column(name = "tree_absorption_days_equiv", nullable = false, precision = 6, scale = 2)
    private BigDecimal treeAbsorptionDaysEquiv;

    @Column(name = "led_bulb_hours_equiv", nullable = false, precision = 8, scale = 2)
    private BigDecimal ledBulbHoursEquiv;

    @Column(name = "shareable_slug", unique = true, nullable = false, length = 64)
    private String shareableSlug;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public TripImpactReceipt() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public BigDecimal getBaselineGasolineCo2Grams() { return baselineGasolineCo2Grams; }
    public void setBaselineGasolineCo2Grams(BigDecimal baselineGasolineCo2Grams) { this.baselineGasolineCo2Grams = baselineGasolineCo2Grams; }

    public BigDecimal getEvEmittedCo2Grams() { return evEmittedCo2Grams; }
    public void setEvEmittedCo2Grams(BigDecimal evEmittedCo2Grams) { this.evEmittedCo2Grams = evEmittedCo2Grams; }

    public BigDecimal getTreeAbsorptionDaysEquiv() { return treeAbsorptionDaysEquiv; }
    public void setTreeAbsorptionDaysEquiv(BigDecimal treeAbsorptionDaysEquiv) { this.treeAbsorptionDaysEquiv = treeAbsorptionDaysEquiv; }

    public BigDecimal getLedBulbHoursEquiv() { return ledBulbHoursEquiv; }
    public void setLedBulbHoursEquiv(BigDecimal ledBulbHoursEquiv) { this.ledBulbHoursEquiv = ledBulbHoursEquiv; }

    public String getShareableSlug() { return shareableSlug; }
    public void setShareableSlug(String shareableSlug) { this.shareableSlug = shareableSlug; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

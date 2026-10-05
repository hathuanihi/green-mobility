package com.greenmobility.modules.carbon.dto;

import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TripImpactReceiptDto {

    private UUID id;
    private UUID tripId;
    private String tripCode;
    private String vehicleType;
    private Integer distanceM;
    private BigDecimal co2SavedGrams;
    private BigDecimal co2SavedKg;
    private BigDecimal baselineGasolineCo2Grams;
    private BigDecimal evEmittedCo2Grams;
    private BigDecimal treeAbsorptionDaysEquiv;
    private BigDecimal ledBulbHoursEquiv;
    private BigDecimal smartphoneChargesEquiv;
    private BigDecimal carbonCreditsEarned;
    private Integer loyaltyPointsEarned;
    private String shareableSlug;
    private String shareUrl;
    private Instant createdAt;

    public static TripImpactReceiptDto fromEntity(TripImpactReceipt entity, String tripCode, String vehicleType, Integer distanceM, BigDecimal credits, Integer points) {
        if (entity == null) return null;
        TripImpactReceiptDto dto = new TripImpactReceiptDto();
        dto.setId(entity.getId());
        dto.setTripId(entity.getTripId());
        dto.setTripCode(tripCode);
        dto.setVehicleType(vehicleType);
        dto.setDistanceM(distanceM);
        dto.setCo2SavedGrams(entity.getCo2SavedGrams());
        dto.setCo2SavedKg(entity.getCo2SavedGrams() != null ? entity.getCo2SavedGrams().divide(BigDecimal.valueOf(1000), 3, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        dto.setBaselineGasolineCo2Grams(entity.getBaselineGasolineCo2Grams());
        dto.setEvEmittedCo2Grams(entity.getEvEmittedCo2Grams());
        dto.setTreeAbsorptionDaysEquiv(entity.getTreeAbsorptionDaysEquiv());
        dto.setLedBulbHoursEquiv(entity.getLedBulbHoursEquiv());
        // Smartphone: ~8.22g CO2 per charge
        if (entity.getCo2SavedGrams() != null) {
            dto.setSmartphoneChargesEquiv(entity.getCo2SavedGrams().divide(BigDecimal.valueOf(8.22), 1, java.math.RoundingMode.HALF_UP));
        }
        dto.setCarbonCreditsEarned(credits);
        dto.setLoyaltyPointsEarned(points);
        dto.setShareableSlug(entity.getShareableSlug());
        dto.setShareUrl("/eco/certificate/" + entity.getShareableSlug());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public BigDecimal getCo2SavedKg() { return co2SavedKg; }
    public void setCo2SavedKg(BigDecimal co2SavedKg) { this.co2SavedKg = co2SavedKg; }

    public BigDecimal getBaselineGasolineCo2Grams() { return baselineGasolineCo2Grams; }
    public void setBaselineGasolineCo2Grams(BigDecimal baselineGasolineCo2Grams) { this.baselineGasolineCo2Grams = baselineGasolineCo2Grams; }

    public BigDecimal getEvEmittedCo2Grams() { return evEmittedCo2Grams; }
    public void setEvEmittedCo2Grams(BigDecimal evEmittedCo2Grams) { this.evEmittedCo2Grams = evEmittedCo2Grams; }

    public BigDecimal getTreeAbsorptionDaysEquiv() { return treeAbsorptionDaysEquiv; }
    public void setTreeAbsorptionDaysEquiv(BigDecimal treeAbsorptionDaysEquiv) { this.treeAbsorptionDaysEquiv = treeAbsorptionDaysEquiv; }

    public BigDecimal getLedBulbHoursEquiv() { return ledBulbHoursEquiv; }
    public void setLedBulbHoursEquiv(BigDecimal ledBulbHoursEquiv) { this.ledBulbHoursEquiv = ledBulbHoursEquiv; }

    public BigDecimal getSmartphoneChargesEquiv() { return smartphoneChargesEquiv; }
    public void setSmartphoneChargesEquiv(BigDecimal smartphoneChargesEquiv) { this.smartphoneChargesEquiv = smartphoneChargesEquiv; }

    public BigDecimal getCarbonCreditsEarned() { return carbonCreditsEarned; }
    public void setCarbonCreditsEarned(BigDecimal carbonCreditsEarned) { this.carbonCreditsEarned = carbonCreditsEarned; }

    public Integer getLoyaltyPointsEarned() { return loyaltyPointsEarned; }
    public void setLoyaltyPointsEarned(Integer loyaltyPointsEarned) { this.loyaltyPointsEarned = loyaltyPointsEarned; }

    public String getShareableSlug() { return shareableSlug; }
    public void setShareableSlug(String shareableSlug) { this.shareableSlug = shareableSlug; }

    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

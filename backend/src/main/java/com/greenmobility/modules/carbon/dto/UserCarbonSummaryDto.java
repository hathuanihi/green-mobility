package com.greenmobility.modules.carbon.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class UserCarbonSummaryDto {

    private UUID userId;
    private BigDecimal totalCo2SavedGrams;
    private BigDecimal totalCo2SavedKg;
    private Long totalGreenTrips;
    private BigDecimal equivalentTreeDays;
    private BigDecimal equivalentLedHours;
    private BigDecimal equivalentSmartphoneCharges;
    private BigDecimal personalCarbonCredits;
    private Integer totalEcoPoints;

    public static UserCarbonSummaryDto of(UUID userId, BigDecimal totalGrams, Long totalTrips) {
        UserCarbonSummaryDto dto = new UserCarbonSummaryDto();
        dto.setUserId(userId);
        BigDecimal safeGrams = totalGrams != null ? totalGrams : BigDecimal.ZERO;
        dto.setTotalCo2SavedGrams(safeGrams);
        dto.setTotalCo2SavedKg(safeGrams.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        dto.setTotalGreenTrips(totalTrips != null ? totalTrips : 0L);
        dto.setEquivalentTreeDays(safeGrams.divide(BigDecimal.valueOf(60.0), 1, RoundingMode.HALF_UP));
        dto.setEquivalentLedHours(safeGrams.divide(BigDecimal.valueOf(7.221), 1, RoundingMode.HALF_UP));
        dto.setEquivalentSmartphoneCharges(safeGrams.divide(BigDecimal.valueOf(8.22), 0, RoundingMode.HALF_UP));
        dto.setPersonalCarbonCredits(safeGrams.divide(BigDecimal.valueOf(1000000.0), 4, RoundingMode.HALF_UP));
        dto.setTotalEcoPoints(safeGrams.divide(BigDecimal.valueOf(100.0), 0, RoundingMode.FLOOR).intValue());
        return dto;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getTotalCo2SavedGrams() { return totalCo2SavedGrams; }
    public void setTotalCo2SavedGrams(BigDecimal totalCo2SavedGrams) { this.totalCo2SavedGrams = totalCo2SavedGrams; }

    public BigDecimal getTotalCo2SavedKg() { return totalCo2SavedKg; }
    public void setTotalCo2SavedKg(BigDecimal totalCo2SavedKg) { this.totalCo2SavedKg = totalCo2SavedKg; }

    public Long getTotalGreenTrips() { return totalGreenTrips; }
    public void setTotalGreenTrips(Long totalGreenTrips) { this.totalGreenTrips = totalGreenTrips; }

    public BigDecimal getEquivalentTreeDays() { return equivalentTreeDays; }
    public void setEquivalentTreeDays(BigDecimal equivalentTreeDays) { this.equivalentTreeDays = equivalentTreeDays; }

    public BigDecimal getEquivalentLedHours() { return equivalentLedHours; }
    public void setEquivalentLedHours(BigDecimal equivalentLedHours) { this.equivalentLedHours = equivalentLedHours; }

    public BigDecimal getEquivalentSmartphoneCharges() { return equivalentSmartphoneCharges; }
    public void setEquivalentSmartphoneCharges(BigDecimal equivalentSmartphoneCharges) { this.equivalentSmartphoneCharges = equivalentSmartphoneCharges; }

    public BigDecimal getPersonalCarbonCredits() { return personalCarbonCredits; }
    public void setPersonalCarbonCredits(BigDecimal personalCarbonCredits) { this.personalCarbonCredits = personalCarbonCredits; }

    public Integer getTotalEcoPoints() { return totalEcoPoints; }
    public void setTotalEcoPoints(Integer totalEcoPoints) { this.totalEcoPoints = totalEcoPoints; }
}

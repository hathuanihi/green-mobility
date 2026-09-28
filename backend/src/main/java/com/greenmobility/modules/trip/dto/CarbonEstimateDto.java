package com.greenmobility.modules.trip.dto;

import java.math.BigDecimal;

public class CarbonEstimateDto {

    private BigDecimal co2SavedGrams;
    private Double treeAbsorptionDays;
    private Double ledBulbHours;
    private BigDecimal baselineGasolineGrams;
    private BigDecimal evEmittedGrams;

    public CarbonEstimateDto() {}

    public CarbonEstimateDto(BigDecimal co2SavedGrams, Double treeAbsorptionDays, Double ledBulbHours,
                             BigDecimal baselineGasolineGrams, BigDecimal evEmittedGrams) {
        this.co2SavedGrams = co2SavedGrams;
        this.treeAbsorptionDays = treeAbsorptionDays;
        this.ledBulbHours = ledBulbHours;
        this.baselineGasolineGrams = baselineGasolineGrams;
        this.evEmittedGrams = evEmittedGrams;
    }

    public BigDecimal getCo2SavedGrams() {
        return co2SavedGrams;
    }

    public void setCo2SavedGrams(BigDecimal co2SavedGrams) {
        this.co2SavedGrams = co2SavedGrams;
    }

    public Double getTreeAbsorptionDays() {
        return treeAbsorptionDays;
    }

    public void setTreeAbsorptionDays(Double treeAbsorptionDays) {
        this.treeAbsorptionDays = treeAbsorptionDays;
    }

    public Double getLedBulbHours() {
        return ledBulbHours;
    }

    public void setLedBulbHours(Double ledBulbHours) {
        this.ledBulbHours = ledBulbHours;
    }

    public BigDecimal getBaselineGasolineGrams() {
        return baselineGasolineGrams;
    }

    public void setBaselineGasolineGrams(BigDecimal baselineGasolineGrams) {
        this.baselineGasolineGrams = baselineGasolineGrams;
    }

    public BigDecimal getEvEmittedGrams() {
        return evEmittedGrams;
    }

    public void setEvEmittedGrams(BigDecimal evEmittedGrams) {
        this.evEmittedGrams = evEmittedGrams;
    }
}

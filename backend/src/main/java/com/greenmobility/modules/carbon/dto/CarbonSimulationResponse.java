package com.greenmobility.modules.carbon.dto;

import java.math.BigDecimal;

public class CarbonSimulationResponse {

    private String vehicleCategory;
    private BigDecimal distanceKm;
    private BigDecimal baselineGasolineFactorGco2Km;
    private BigDecimal evEnergyConsumptionKwhKm;
    private BigDecimal gridEmissionFactorGco2Kwh;
    private BigDecimal calculatedEvFactorGco2Km;
    private BigDecimal netCo2SavingPerKm;
    private BigDecimal baselineGasolineCo2Grams;
    private BigDecimal evEmittedCo2Grams;
    private BigDecimal netCo2SavedGrams;
    private BigDecimal netCo2SavedKg;
    private BigDecimal treeAbsorptionDaysEquiv;
    private BigDecimal ledBulbHoursEquiv;
    private BigDecimal smartphoneChargesEquiv;
    private BigDecimal carbonCreditsEarned;
    private Integer ecoPoints;

    // Getters and Setters
    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public BigDecimal getBaselineGasolineFactorGco2Km() { return baselineGasolineFactorGco2Km; }
    public void setBaselineGasolineFactorGco2Km(BigDecimal baselineGasolineFactorGco2Km) { this.baselineGasolineFactorGco2Km = baselineGasolineFactorGco2Km; }

    public BigDecimal getEvEnergyConsumptionKwhKm() { return evEnergyConsumptionKwhKm; }
    public void setEvEnergyConsumptionKwhKm(BigDecimal evEnergyConsumptionKwhKm) { this.evEnergyConsumptionKwhKm = evEnergyConsumptionKwhKm; }

    public BigDecimal getGridEmissionFactorGco2Kwh() { return gridEmissionFactorGco2Kwh; }
    public void setGridEmissionFactorGco2Kwh(BigDecimal gridEmissionFactorGco2Kwh) { this.gridEmissionFactorGco2Kwh = gridEmissionFactorGco2Kwh; }

    public BigDecimal getCalculatedEvFactorGco2Km() { return calculatedEvFactorGco2Km; }
    public void setCalculatedEvFactorGco2Km(BigDecimal calculatedEvFactorGco2Km) { this.calculatedEvFactorGco2Km = calculatedEvFactorGco2Km; }

    public BigDecimal getNetCo2SavingPerKm() { return netCo2SavingPerKm; }
    public void setNetCo2SavingPerKm(BigDecimal netCo2SavingPerKm) { this.netCo2SavingPerKm = netCo2SavingPerKm; }

    public BigDecimal getBaselineGasolineCo2Grams() { return baselineGasolineCo2Grams; }
    public void setBaselineGasolineCo2Grams(BigDecimal baselineGasolineCo2Grams) { this.baselineGasolineCo2Grams = baselineGasolineCo2Grams; }

    public BigDecimal getEvEmittedCo2Grams() { return evEmittedCo2Grams; }
    public void setEvEmittedCo2Grams(BigDecimal evEmittedCo2Grams) { this.evEmittedCo2Grams = evEmittedCo2Grams; }

    public BigDecimal getNetCo2SavedGrams() { return netCo2SavedGrams; }
    public void setNetCo2SavedGrams(BigDecimal netCo2SavedGrams) { this.netCo2SavedGrams = netCo2SavedGrams; }

    public BigDecimal getNetCo2SavedKg() { return netCo2SavedKg; }
    public void setNetCo2SavedKg(BigDecimal netCo2SavedKg) { this.netCo2SavedKg = netCo2SavedKg; }

    public BigDecimal getTreeAbsorptionDaysEquiv() { return treeAbsorptionDaysEquiv; }
    public void setTreeAbsorptionDaysEquiv(BigDecimal treeAbsorptionDaysEquiv) { this.treeAbsorptionDaysEquiv = treeAbsorptionDaysEquiv; }

    public BigDecimal getLedBulbHoursEquiv() { return ledBulbHoursEquiv; }
    public void setLedBulbHoursEquiv(BigDecimal ledBulbHoursEquiv) { this.ledBulbHoursEquiv = ledBulbHoursEquiv; }

    public BigDecimal getSmartphoneChargesEquiv() { return smartphoneChargesEquiv; }
    public void setSmartphoneChargesEquiv(BigDecimal smartphoneChargesEquiv) { this.smartphoneChargesEquiv = smartphoneChargesEquiv; }

    public BigDecimal getCarbonCreditsEarned() { return carbonCreditsEarned; }
    public void setCarbonCreditsEarned(BigDecimal carbonCreditsEarned) { this.carbonCreditsEarned = carbonCreditsEarned; }

    public Integer getEcoPoints() { return ecoPoints; }
    public void setEcoPoints(Integer ecoPoints) { this.ecoPoints = ecoPoints; }
}

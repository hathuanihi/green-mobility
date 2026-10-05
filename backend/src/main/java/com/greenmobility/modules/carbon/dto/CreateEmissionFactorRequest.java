package com.greenmobility.modules.carbon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateEmissionFactorRequest {

    @NotBlank(message = "Vehicle category is required")
    private String vehicleCategory;

    @NotNull(message = "Baseline gasoline factor is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Baseline factor must be positive")
    private BigDecimal baselineGasolineFactorGco2Km;

    @NotNull(message = "EV energy consumption is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "EV energy consumption must be positive")
    private BigDecimal evEnergyConsumptionKwhKm;

    @NotNull(message = "Grid emission factor is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Grid emission factor must be positive")
    private BigDecimal gridEmissionFactorGco2Kwh;

    private String region = "VIETNAM_NATIONAL";

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean isActive = true;

    // Getters and Setters
    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public BigDecimal getBaselineGasolineFactorGco2Km() { return baselineGasolineFactorGco2Km; }
    public void setBaselineGasolineFactorGco2Km(BigDecimal baselineGasolineFactorGco2Km) { this.baselineGasolineFactorGco2Km = baselineGasolineFactorGco2Km; }

    public BigDecimal getEvEnergyConsumptionKwhKm() { return evEnergyConsumptionKwhKm; }
    public void setEvEnergyConsumptionKwhKm(BigDecimal evEnergyConsumptionKwhKm) { this.evEnergyConsumptionKwhKm = evEnergyConsumptionKwhKm; }

    public BigDecimal getGridEmissionFactorGco2Kwh() { return gridEmissionFactorGco2Kwh; }
    public void setGridEmissionFactorGco2Kwh(BigDecimal gridEmissionFactorGco2Kwh) { this.gridEmissionFactorGco2Kwh = gridEmissionFactorGco2Kwh; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

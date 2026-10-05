package com.greenmobility.modules.carbon.dto;

import com.greenmobility.modules.carbon.entity.EmissionFactor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EmissionFactorDto {

    private UUID id;
    private String vehicleCategory;
    private BigDecimal baselineGasolineFactorGco2Km;
    private BigDecimal evEnergyConsumptionKwhKm;
    private BigDecimal gridEmissionFactorGco2Kwh;
    private BigDecimal calculatedEvFactorGco2Km;
    private BigDecimal netCo2SavingPerKm;
    private String region;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private UUID createdBy;
    private Instant createdAt;

    public static EmissionFactorDto fromEntity(EmissionFactor entity) {
        if (entity == null) return null;
        EmissionFactorDto dto = new EmissionFactorDto();
        dto.setId(entity.getId());
        dto.setVehicleCategory(entity.getVehicleCategory());
        dto.setBaselineGasolineFactorGco2Km(entity.getBaselineGasolineFactorGco2Km());
        dto.setEvEnergyConsumptionKwhKm(entity.getEvEnergyConsumptionKwhKm());
        dto.setGridEmissionFactorGco2Kwh(entity.getGridEmissionFactorGco2Kwh());
        dto.setCalculatedEvFactorGco2Km(entity.getCalculatedEvFactorGco2Km());
        dto.setNetCo2SavingPerKm(entity.getNetCo2SavingPerKm());
        dto.setRegion(entity.getRegion());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

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

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

package com.greenmobility.modules.carbon.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "emission_factors")
public class EmissionFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_category", nullable = false, length = 50)
    private String vehicleCategory;

    @Column(name = "baseline_gasoline_factor_gco2_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal baselineGasolineFactorGco2Km;

    @Column(name = "ev_energy_consumption_kwh_km", nullable = false, precision = 6, scale = 4)
    private BigDecimal evEnergyConsumptionKwhKm;

    @Column(name = "grid_emission_factor_gco2_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal gridEmissionFactorGco2Kwh;

    @Column(name = "calculated_ev_factor_gco2_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal calculatedEvFactorGco2Km;

    @Column(name = "net_co2_saving_per_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal netCo2SavingPerKm;

    @Column(name = "region", length = 50)
    private String region = "VIETNAM_NATIONAL";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public EmissionFactor() {}

    /**
     * Recalculates calculatedEvFactorGco2Km and netCo2SavingPerKm
     * calculatedEvFactor = evEnergyConsumptionKwhKm * gridEmissionFactorGco2Kwh
     * netCo2SavingPerKm = baselineGasolineFactorGco2Km - calculatedEvFactorGco2Km
     */
    public void recalculate() {
        if (evEnergyConsumptionKwhKm != null && gridEmissionFactorGco2Kwh != null) {
            this.calculatedEvFactorGco2Km = evEnergyConsumptionKwhKm
                    .multiply(gridEmissionFactorGco2Kwh)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (baselineGasolineFactorGco2Km != null && calculatedEvFactorGco2Km != null) {
            this.netCo2SavingPerKm = baselineGasolineFactorGco2Km
                    .subtract(calculatedEvFactorGco2Km)
                    .setScale(2, RoundingMode.HALF_UP);
        }
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

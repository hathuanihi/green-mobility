package com.greenmobility.modules.carbon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CarbonSimulationRequest {

    @NotBlank(message = "Vehicle category is required")
    private String vehicleCategory;

    @NotNull(message = "Distance in kilometers is required")
    @DecimalMin(value = "0.01", message = "Distance must be greater than zero")
    private BigDecimal distanceKm;

    public CarbonSimulationRequest() {}

    public CarbonSimulationRequest(String vehicleCategory, BigDecimal distanceKm) {
        this.vehicleCategory = vehicleCategory;
        this.distanceKm = distanceKm;
    }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
}

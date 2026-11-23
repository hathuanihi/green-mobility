package com.greenmobility.modules.carbon.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CarbonModuleApi {

    EmissionFactorSnapshot getActiveFactor(String vehicleType, LocalDate targetDate);

    record EmissionFactorSnapshot(
            String vehicleCategory,
            BigDecimal baselineGasolineGco2Km,
            BigDecimal evEnergyConsumptionKwhKm,
            BigDecimal gridEmissionFactorGco2Kwh,
            BigDecimal calculatedEvFactorGco2Km,
            BigDecimal netCo2SavingPerKm
    ) {}
}

package com.greenmobility.modules.carbon.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CarbonModuleApi {

    EmissionFactorSnapshot getActiveFactor(String vehicleType, LocalDate targetDate);

    Optional<BigDecimal> getReceiptCo2SavedGrams(UUID tripId);

    BigDecimal getCustomerLifetimeCo2SavedGrams(UUID customerId);

    record EmissionFactorSnapshot(
            String vehicleCategory,
            BigDecimal baselineGasolineGco2Km,
            BigDecimal evEnergyConsumptionKwhKm,
            BigDecimal gridEmissionFactorGco2Kwh,
            BigDecimal calculatedEvFactorGco2Km,
            BigDecimal netCo2SavingPerKm
    ) {}
}

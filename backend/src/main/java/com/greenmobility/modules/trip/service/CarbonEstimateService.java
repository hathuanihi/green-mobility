package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.dto.CarbonEstimateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CarbonEstimateService {

    @Value("${green-mobility.carbon.default-grid-emission-factor-gco2-kwh:722.1}")
    private double gridEmissionFactorGco2Kwh;

    @Value("${green-mobility.carbon.tree-absorption-daily-kg:0.06}")
    private double treeAbsorptionDailyKg;

    @Value("${green-mobility.carbon.led-bulb-consumption-hourly-gco2:7.221}")
    private double ledBulbHourlyGco2;

    public CarbonEstimateDto estimateCarbon(VehicleType vehicleType, double distanceKm) {
        double baselineGasolineFactor;
        double secKwhPerKm;

        switch (vehicleType) {
            case ELECTRIC_MOTORBIKE -> {
                baselineGasolineFactor = 68.5; // gCO2/km
                secKwhPerKm = 0.025; // kWh/km
            }
            case ELECTRIC_CAR_4SEAT -> {
                baselineGasolineFactor = 165.0; // gCO2/km
                secKwhPerKm = 0.145; // kWh/km
            }
            case ELECTRIC_CAR_7SEAT -> {
                baselineGasolineFactor = 210.0; // gCO2/km
                secKwhPerKm = 0.175; // kWh/km
            }
            default -> {
                baselineGasolineFactor = 68.5;
                secKwhPerKm = 0.025;
            }
        }

        double totalBaselineGrams = baselineGasolineFactor * distanceKm;
        double evEmittedGrams = secKwhPerKm * gridEmissionFactorGco2Kwh * distanceKm;
        double co2SavedGrams = Math.max(0.0, totalBaselineGrams - evEmittedGrams);

        // Equivalents
        double treeDailyGrams = treeAbsorptionDailyKg * 1000.0; // 60g
        double treeAbsorptionDays = treeDailyGrams > 0 ? (co2SavedGrams / treeDailyGrams) : 0;
        double ledBulbHours = ledBulbHourlyGco2 > 0 ? (co2SavedGrams / ledBulbHourlyGco2) : 0;

        return new CarbonEstimateDto(
                BigDecimal.valueOf(co2SavedGrams).setScale(2, RoundingMode.HALF_UP),
                roundDouble(treeAbsorptionDays, 2),
                roundDouble(ledBulbHours, 2),
                BigDecimal.valueOf(totalBaselineGrams).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(evEmittedGrams).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private double roundDouble(double val, int places) {
        return BigDecimal.valueOf(val).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}

package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.carbon.api.CarbonModuleApi;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.dto.CarbonEstimateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class CarbonEstimateService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${green-mobility.carbon.default-grid-emission-factor-gco2-kwh:618.7}")
    private double gridEmissionFactorGco2Kwh = 618.7;

    @Value("${green-mobility.carbon.tree-absorption-daily-kg:0.06}")
    private double treeAbsorptionDailyKg = 0.06;

    @Value("${green-mobility.carbon.led-bulb-consumption-hourly-gco2:6.187}")
    private double ledBulbHourlyGco2 = 6.187;

    private final CarbonModuleApi carbonModuleApi;

    public CarbonEstimateService() {
        this.carbonModuleApi = null;
    }

    @Autowired
    public CarbonEstimateService(@Autowired(required = false) CarbonModuleApi carbonModuleApi) {
        this.carbonModuleApi = carbonModuleApi;
    }

    public CarbonEstimateDto estimateCarbon(VehicleType vehicleType, double distanceKm) {
        String category = vehicleType != null ? vehicleType.name() : "ELECTRIC_MOTORBIKE";

        double baselineGasolineFactor;
        double evFactorPerKm;
        double activeGridEf = gridEmissionFactorGco2Kwh;

        if (carbonModuleApi != null) {
            CarbonModuleApi.EmissionFactorSnapshot snap = carbonModuleApi.getActiveFactor(category, LocalDate.now(VN_ZONE));
            baselineGasolineFactor = snap.baselineGasolineGco2Km().doubleValue();
            evFactorPerKm = snap.calculatedEvFactorGco2Km().doubleValue();
            activeGridEf = snap.gridEmissionFactorGco2Kwh().doubleValue();
        } else {
            double secKwhPerKm;
            switch (category) {
                case "ELECTRIC_CAR_4SEAT" -> {
                    baselineGasolineFactor = 152.0;
                    secKwhPerKm = 0.135;
                }
                case "ELECTRIC_CAR_7SEAT" -> {
                    baselineGasolineFactor = 210.0;
                    secKwhPerKm = 0.180;
                }
                default -> {
                    baselineGasolineFactor = 68.5;
                    secKwhPerKm = 0.032;
                }
            }
            evFactorPerKm = BigDecimal.valueOf(secKwhPerKm * activeGridEf)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        double totalBaselineGrams = baselineGasolineFactor * distanceKm;
        double evEmittedGrams = evFactorPerKm * distanceKm;
        double co2SavedGrams = Math.max(0.0, totalBaselineGrams - evEmittedGrams);

        double treeDailyGrams = treeAbsorptionDailyKg * 1000.0;
        double treeAbsorptionDays = treeDailyGrams > 0 ? (co2SavedGrams / treeDailyGrams) : 0;
        double effectiveLedHourly = activeGridEf * 0.01 > 0 ? activeGridEf * 0.01 : ledBulbHourlyGco2;
        double ledBulbHours = effectiveLedHourly > 0 ? (co2SavedGrams / effectiveLedHourly) : 0;

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

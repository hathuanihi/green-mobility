package com.greenmobility.trip;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.dto.CarbonEstimateDto;
import com.greenmobility.modules.trip.service.CarbonEstimateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarbonEstimateServiceTest {

    private CarbonEstimateService carbonService;

    @BeforeEach
    void setUp() {
        carbonService = new CarbonEstimateService();
        ReflectionTestUtils.setField(carbonService, "gridEmissionFactorGco2Kwh", 722.1);
        ReflectionTestUtils.setField(carbonService, "treeAbsorptionDailyKg", 0.06);
        ReflectionTestUtils.setField(carbonService, "ledBulbHourlyGco2", 7.221);
    }

    @Test
    @DisplayName("Ước tính phát thải xe máy điện 16.2km phải tính ra CO2 giảm và cây xanh tương đương")
    void testEBikeCarbonEstimate() {
        CarbonEstimateDto estimate = carbonService.estimateCarbon(VehicleType.ELECTRIC_MOTORBIKE, 16.2);

        assertNotNull(estimate);
        // Baseline ~ 1109.7g, EV ~ 292.4g, Saved ~ 817.3g
        assertTrue(estimate.getCo2SavedGrams().doubleValue() > 800.0);
        assertTrue(estimate.getTreeAbsorptionDays() > 13.0);
        assertTrue(estimate.getLedBulbHours() > 100.0);
        assertTrue(estimate.getBaselineGasolineGrams().doubleValue() > 1100.0);
    }

    @Test
    @DisplayName("Ước tính phát thải ô tô điện 4 chỗ 10km")
    void testECar4SCarbonEstimate() {
        CarbonEstimateDto estimate = carbonService.estimateCarbon(VehicleType.ELECTRIC_CAR_4SEAT, 10.0);

        assertNotNull(estimate);
        assertTrue(estimate.getCo2SavedGrams().doubleValue() > 500.0);
        assertTrue(estimate.getTreeAbsorptionDays() > 8.0);
    }
}

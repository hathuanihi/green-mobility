package com.greenmobility.trip;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.service.FareCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareCalculationServiceTest {

    private FareCalculationService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareCalculationService();
    }

    @Test
    @DisplayName("Cước xe máy điện dưới 2km phải bằng giá mở cửa 12.000 VNĐ")
    void testEBikeBaseFare() {
        BigDecimal fare = fareService.calculateFare(VehicleType.ELECTRIC_MOTORBIKE, 1.5);
        assertEquals(BigDecimal.valueOf(12000), fare);
    }

    @Test
    @DisplayName("Cước xe máy điện 16.2km phải tính đúng và làm tròn lên nghìn đồng gần nhất (76.000 VNĐ)")
    void testEBikeLongDistanceFare() {
        // 12000 + (16.2 - 2) * 4500 = 12000 + 63900 = 75900 -> Round up: 76000
        BigDecimal fare = fareService.calculateFare(VehicleType.ELECTRIC_MOTORBIKE, 16.2);
        assertEquals(BigDecimal.valueOf(76000), fare);

        // Driver income 80%
        BigDecimal income = fareService.calculateDriverIncome(fare);
        assertEquals(BigDecimal.valueOf(60800), income);
    }

    @Test
    @DisplayName("Cước ô tô điện 4 chỗ dưới 2km phải bằng 20.000 VNĐ")
    void testECar4SBaseFare() {
        BigDecimal fare = fareService.calculateFare(VehicleType.ELECTRIC_CAR_4SEAT, 1.0);
        assertEquals(BigDecimal.valueOf(20000), fare);
    }

    @Test
    @DisplayName("Cước ô tô điện 7 chỗ 10km phải tính đúng")
    void testECar7SFare() {
        // 25000 + (10 - 2) * 14500 = 25000 + 116000 = 141000
        BigDecimal fare = fareService.calculateFare(VehicleType.ELECTRIC_CAR_7SEAT, 10.0);
        assertEquals(BigDecimal.valueOf(141000), fare);
    }

    @Test
    @DisplayName("Phụ phí đêm (22h - 06h): E-Bike +5k, E-Car 4S +15k, E-Car 7S +20k")
    void testNightSurcharges() {
        // Night E-Bike 16.2km: 76.000 + 5.000 = 81.000
        BigDecimal fareBikeNight = fareService.calculateFare(VehicleType.ELECTRIC_MOTORBIKE, 16.2, true);
        assertEquals(BigDecimal.valueOf(81000), fareBikeNight);

        // Night E-Car 4S 1.0km: 20.000 + 15.000 = 35.000
        BigDecimal fareCar4Night = fareService.calculateFare(VehicleType.ELECTRIC_CAR_4SEAT, 1.0, true);
        assertEquals(BigDecimal.valueOf(35000), fareCar4Night);

        // Night E-Car 7S 10.0km: 141.000 + 20.000 = 161.000
        BigDecimal fareCar7Night = fareService.calculateFare(VehicleType.ELECTRIC_CAR_7SEAT, 10.0, true);
        assertEquals(BigDecimal.valueOf(161000), fareCar7Night);
    }

    @Test
    @DisplayName("Kiểm tra hàm isNightTime xác định đúng khung giờ đêm 22h - 06h Việt Nam")
    void testIsNightTimeDetection() {
        // 23:30 VN Time -> Night
        java.time.ZonedDateTime nightTime = java.time.ZonedDateTime.of(2026, 10, 5, 23, 30, 0, 0, FareCalculationService.VN_ZONE);
        org.junit.jupiter.api.Assertions.assertTrue(fareService.isNightTime(nightTime.toInstant()));

        // 03:00 VN Time -> Night
        java.time.ZonedDateTime earlyMorning = java.time.ZonedDateTime.of(2026, 10, 5, 3, 0, 0, 0, FareCalculationService.VN_ZONE);
        org.junit.jupiter.api.Assertions.assertTrue(fareService.isNightTime(earlyMorning.toInstant()));

        // 14:00 VN Time -> Day
        java.time.ZonedDateTime dayTime = java.time.ZonedDateTime.of(2026, 10, 5, 14, 0, 0, 0, FareCalculationService.VN_ZONE);
        org.junit.jupiter.api.Assertions.assertFalse(fareService.isNightTime(dayTime.toInstant()));
    }
}

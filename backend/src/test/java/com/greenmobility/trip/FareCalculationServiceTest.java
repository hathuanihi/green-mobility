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
}

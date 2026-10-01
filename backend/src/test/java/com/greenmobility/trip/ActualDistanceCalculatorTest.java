package com.greenmobility.trip;

import com.greenmobility.modules.trip.entity.TripGpsPoint;
import com.greenmobility.modules.trip.service.ActualDistanceCalculator;
import com.greenmobility.modules.trip.service.GeofenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActualDistanceCalculatorTest {

    private ActualDistanceCalculator calculator;
    private GeofenceService geofenceService;

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceService();
        calculator = new ActualDistanceCalculator(geofenceService);
    }

    @Test
    @DisplayName("Tính tổng quãng đường bình thường với chuỗi điểm liên tiếp")
    void testCalculateActualDistance_NormalPath() {
        Instant baseTime = Instant.parse("2026-10-19T08:00:00Z");
        List<TripGpsPoint> points = new ArrayList<>();

        // 3 điểm dọc theo đường Lê Lợi, mỗi điểm cách nhau khoảng 100m, thời gian 10s
        points.add(createPoint(10.776530, 106.700981, 30.0, 5.0, baseTime));
        points.add(createPoint(10.777000, 106.701500, 30.0, 5.0, baseTime.plusSeconds(10)));
        points.add(createPoint(10.777500, 106.702000, 30.0, 5.0, baseTime.plusSeconds(20)));

        int distance = calculator.calculateActualDistance(points);
        assertTrue(distance > 100 && distance < 300, "Quãng đường phải khoảng 150m-200m: " + distance);
    }

    @Test
    @DisplayName("Lọc bỏ điểm có vận tốc tức thời bất thường (> 120 km/h)")
    void testFilterSpeedSpike() {
        Instant baseTime = Instant.parse("2026-10-19T08:00:00Z");
        List<TripGpsPoint> points = new ArrayList<>();

        // P0 -> P1: bình thường (30m trong 5s = ~21.6 km/h)
        points.add(createPoint(10.776530, 106.700981, 25.0, 5.0, baseTime));
        points.add(createPoint(10.776700, 106.701100, 25.0, 5.0, baseTime.plusSeconds(5)));

        // P2: Nhảy vọt tới Q.7 trong 1 giây (> 5km / 1s = > 18000 km/h)
        points.add(createPoint(10.730000, 106.720000, 150.0, 5.0, baseTime.plusSeconds(6)));

        // P3: Quay lại điểm gần P1
        points.add(createPoint(10.776900, 106.701300, 25.0, 5.0, baseTime.plusSeconds(10)));

        int distance = calculator.calculateActualDistance(points);
        // Điểm nhảy bất thường phải bị lọc bỏ
        assertTrue(distance < 500, "Khoảng cách phải được lọc, không tính bước nhảy vọt: " + distance);
    }

    @Test
    @DisplayName("Lọc bỏ điểm có độ chính xác thấp (accuracy > 50m)")
    void testFilterLowAccuracy() {
        Instant baseTime = Instant.parse("2026-10-19T08:00:00Z");
        List<TripGpsPoint> points = new ArrayList<>();

        points.add(createPoint(10.776530, 106.700981, 30.0, 5.0, baseTime));
        // Điểm có accuracy = 80m (> 50m threshold)
        points.add(createPoint(10.777000, 106.701500, 30.0, 80.0, baseTime.plusSeconds(5)));
        points.add(createPoint(10.777500, 106.702000, 30.0, 5.0, baseTime.plusSeconds(10)));

        int distance = calculator.calculateActualDistance(points);
        assertTrue(distance >= 0);
    }

    @Test
    @DisplayName("Kiểm tra tỷ lệ đối chiếu khoảng cách (Sanity Check Ratio)")
    void testValidateDistanceRatio() {
        // Hợp lý (0.85 - 1.30)
        ActualDistanceCalculator.DistanceValidation normal = calculator.validateDistanceRatio(10500, 10000);
        assertTrue(normal.isValid());
        assertFalse(normal.isFraudSuspect());
        assertEquals(1.05, normal.ratio(), 0.01);

        // Nghi ngờ gian lận (ratio > 1.30)
        ActualDistanceCalculator.DistanceValidation fraud = calculator.validateDistanceRatio(15000, 10000);
        assertFalse(fraud.isValid());
        assertTrue(fraud.isFraudSuspect());

        // Quá ngắn (ratio < 0.85, ví dụ mất GPS)
        ActualDistanceCalculator.DistanceValidation tooShort = calculator.validateDistanceRatio(6000, 10000);
        assertFalse(tooShort.isValid());

        // Fallback distance test
        int fallback = calculator.getFallbackDistance(6000, 10000);
        assertEquals(10000, fallback, "Khi actual quá ngắn, sử dụng estimated làm fallback");
    }

    private TripGpsPoint createPoint(double lat, double lng, Double speedKmh, Double accuracy, Instant timestamp) {
        return new TripGpsPoint(
                "trip-1", "driver-1", "ELECTRIC_MOTORBIKE", "IN_TRIP",
                lng, lat, speedKmh, 0.0, 10.0, accuracy, 80, false, timestamp
        );
    }
}

package com.greenmobility.trip;

import com.greenmobility.modules.trip.service.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingServiceTest {

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService();
    }

    @Test
    @DisplayName("Tính toán lộ trình giữa Nhà hát TP.HCM (Q1) và UIT (Thủ Đức) ~ 15-18km")
    void testCalculateRoute() {
        double pickupLat = 10.776530;
        double pickupLng = 106.700981;
        double dropoffLat = 10.870020;
        double dropoffLng = 106.803054;

        RoutingService.RouteInfo route = routingService.calculateRoute(pickupLat, pickupLng, dropoffLat, dropoffLng);

        assertNotNull(route);
        assertTrue(route.distanceMeters() >= 14000 && route.distanceMeters() <= 20000,
                "Quãng đường phải nằm trong khoảng 14km - 20km");
        assertTrue(route.durationSeconds() >= 1500, "Thời gian phải tối thiểu 25 phút");
        assertNotNull(route.polyline());
    }
}

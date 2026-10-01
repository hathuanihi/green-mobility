package com.greenmobility.trip;

import com.greenmobility.modules.trip.service.GeofenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeofenceServiceTest {

    private GeofenceService geofenceService;

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceService();
    }

    @Test
    @DisplayName("Độ chính xác công thức Haversine giữa 2 điểm Nhà hát TP (10.776530, 106.700981) và Phố đi bộ Nguyễn Huệ (10.773820, 106.704250)")
    void testHaversineDistanceAccuracy() {
        double lat1 = 10.776530;
        double lng1 = 106.700981;
        double lat2 = 10.773820;
        double lng2 = 106.704250;

        double distance = geofenceService.calculateHaversineDistance(lat1, lng1, lat2, lng2);
        // Khoảng cách thực tế ~ 460m - 500m
        assertTrue(distance >= 440 && distance <= 520, "Khoảng cách phải ~ 470m (thực tế: " + distance + "m)");
    }

    @Test
    @DisplayName("Kiểm tra Geofence tự động đến điểm đón (threshold <= 50m)")
    void testPickupGeofence() {
        double pickupLat = 10.776530;
        double pickupLng = 106.700981;

        // Vị trí cách 30m
        double nearLat = 10.776700;
        double nearLng = 106.701100;
        assertTrue(geofenceService.isWithinPickupGeofence(nearLat, nearLng, pickupLat, pickupLng));

        // Vị trí cách 150m
        double farLat = 10.777800;
        double farLng = 106.701800;
        assertFalse(geofenceService.isWithinPickupGeofence(farLat, farLng, pickupLat, pickupLng));
    }

    @Test
    @DisplayName("Kiểm tra Geofence xác nhận thủ công (threshold <= 200m)")
    void testManualArriveDistance() {
        double pickupLat = 10.776530;
        double pickupLng = 106.700981;

        // Vị trí cách 150m (cho phép xác nhận thủ công)
        double nearLat = 10.777800;
        double nearLng = 106.701800;
        assertTrue(geofenceService.isWithinManualArriveDistance(nearLat, nearLng, pickupLat, pickupLng));

        // Vị trí cách 500m (quá xa, từ chối)
        double farLat = 10.773820;
        double farLng = 106.704250;
        assertFalse(geofenceService.isWithinManualArriveDistance(farLat, farLng, pickupLat, pickupLng));
    }

    @Test
    @DisplayName("Kiểm tra checkGeofence với bán kính tùy biến")
    void testCheckGeofence() {
        double lat1 = 10.776530;
        double lng1 = 106.700981;
        double lat2 = 10.776700;
        double lng2 = 106.701100;

        assertTrue(geofenceService.checkGeofence(lat1, lng1, lat2, lng2, 100.0));
        assertFalse(geofenceService.checkGeofence(lat1, lng1, lat2, lng2, 10.0));
    }
}

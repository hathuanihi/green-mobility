package com.greenmobility.trip;

import com.greenmobility.common.util.PolylineUtil;
import com.greenmobility.modules.trip.dto.RoutingResultDto;
import com.greenmobility.modules.trip.dto.RoutingStepDto;
import com.greenmobility.modules.trip.service.GeofenceService;
import com.greenmobility.modules.trip.service.RoutingService;
import com.greenmobility.modules.trip.service.TripEtaService;
import com.greenmobility.modules.trip.service.TripTrackingRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripEtaServiceTest {

    @Mock private RoutingService routingService;
    @Mock private TripTrackingRedisService tripTrackingRedisService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private GeofenceService geofenceService;
    private TripEtaService tripEtaService;

    private UUID tripId;
    private String testPolyline;

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceService();
        tripEtaService = new TripEtaService(
                routingService,
                geofenceService,
                tripTrackingRedisService,
                redisTemplate
        );

        tripId = UUID.randomUUID();

        // Tạo polyline mẫu từ Nhà hát TP (10.776530, 106.700981) đến Nhà thờ Đức Bà (10.779780, 106.699020)
        List<PolylineUtil.LatLngPoint> points = List.of(
                new PolylineUtil.LatLngPoint(10.776530, 106.700981),
                new PolylineUtil.LatLngPoint(10.778000, 106.700000),
                new PolylineUtil.LatLngPoint(10.779780, 106.699020)
        );
        testPolyline = PolylineUtil.encode(points);
    }

    @Test
    @DisplayName("Tài xế di chuyển trên lộ trình (deviation <= 100m) -> Ước tính ETA nhanh không gọi re-route")
    void testFastEtaCalculation_OnRoute() {
        when(tripTrackingRedisService.getRoutePolyline(tripId)).thenReturn(testPolyline);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Lần tính trước cách 5 giây (< 30s)
        when(valueOperations.get("trip:eta:last_calc:" + tripId))
                .thenReturn(String.valueOf(System.currentTimeMillis() - 5000));

        // Vị trí tài xế rất gần điểm 2 trên polyline (~20m)
        double currentLat = 10.778100;
        double currentLng = 106.700100;
        double targetLat = 10.779780;
        double targetLng = 106.699020;

        TripEtaService.EtaResult result = tripEtaService.calculateEta(
                tripId, currentLat, currentLng, targetLat, targetLng, "ELECTRIC_MOTORBIKE", 36.0 // 36 km/h = 10 m/s
        );

        assertNotNull(result);
        assertFalse(result.isRerouted(), "Không được kích hoạt re-route khi đang đi đúng đường");
        assertTrue(result.etaSeconds() > 0);
        assertTrue(result.distanceRemainingM() > 0);
        assertEquals(testPolyline, result.polyline());

        verify(routingService, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("Tài xế lệch lộ trình > 100m -> Kích hoạt OSRM/Goong Re-route ngay lập tức")
    void testReRoute_WhenDeviated() {
        when(tripTrackingRedisService.getRoutePolyline(tripId)).thenReturn(testPolyline);
        when(routingService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString()))
                .thenReturn(new RoutingResultDto(1200, 150, "new_polyline_rerouted", List.of()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Vị trí tài xế ở xa đường (> 500m)
        double farLat = 10.790000;
        double farLng = 106.715000;
        double targetLat = 10.779780;
        double targetLng = 106.699020;

        TripEtaService.EtaResult result = tripEtaService.calculateEta(
                tripId, farLat, farLng, targetLat, targetLng, "ELECTRIC_MOTORBIKE", 30.0
        );

        assertNotNull(result);
        assertTrue(result.isRerouted(), "Phải kích hoạt re-route khi lệch đường > 100m");
        assertEquals("new_polyline_rerouted", result.polyline());
        assertEquals(150, result.etaSeconds());
        assertEquals(1200, result.distanceRemainingM());

        verify(routingService, times(1)).getRoute(eq(farLat), eq(farLng), eq(targetLat), eq(targetLng), eq("ELECTRIC_MOTORBIKE"));
        verify(tripTrackingRedisService, times(1)).saveRoutePolyline(eq(tripId), eq("new_polyline_rerouted"));
    }

    @Test
    @DisplayName("Quá 30 giây kể từ lần route trước -> Định kỳ gọi lại RoutingService để đồng bộ ETA")
    void testReRoute_PeriodicInterval() {
        when(tripTrackingRedisService.getRoutePolyline(tripId)).thenReturn(testPolyline);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Cách lần trước 35 giây (> 30s)
        when(valueOperations.get("trip:eta:last_calc:" + tripId))
                .thenReturn(String.valueOf(System.currentTimeMillis() - 35000));

        when(routingService.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString()))
                .thenReturn(new RoutingResultDto(500, 60, testPolyline, List.of()));

        double currentLat = 10.778000;
        double currentLng = 106.700000;
        double targetLat = 10.779780;
        double targetLng = 106.699020;

        TripEtaService.EtaResult result = tripEtaService.calculateEta(
                tripId, currentLat, currentLng, targetLat, targetLng, "ELECTRIC_MOTORBIKE", 20.0
        );

        assertNotNull(result);
        assertTrue(result.isRerouted());
        verify(routingService, times(1)).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString());
    }
}

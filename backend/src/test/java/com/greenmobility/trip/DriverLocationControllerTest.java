package com.greenmobility.trip;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.controller.DriverLocationController;
import com.greenmobility.modules.trip.dto.DriverLocationBroadcastDto;
import com.greenmobility.modules.trip.dto.DriverLocationUpdateMessage;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.repository.TripRepository;
import com.greenmobility.modules.trip.service.GeofenceService;
import com.greenmobility.modules.trip.service.GpsTelemetryService;
import com.greenmobility.modules.trip.service.TripEtaService;
import com.greenmobility.modules.trip.service.TripService;
import com.greenmobility.modules.trip.service.TripTrackingRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLocationControllerTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripService tripService;
    @Mock private GpsTelemetryService gpsTelemetryService;
    @Mock private TripTrackingRedisService tripTrackingRedisService;
    @Mock private GeofenceService geofenceService;
    @Mock private TripEtaService tripEtaService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SimpMessageHeaderAccessor headerAccessor;
    @Mock private Principal principal;

    private DriverLocationController locationController;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private UUID tripId;
    private UUID driverId;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        locationController = new DriverLocationController(
                tripRepository,
                tripService,
                gpsTelemetryService,
                tripTrackingRedisService,
                geofenceService,
                tripEtaService,
                messagingTemplate
        );

        tripId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        mockTrip = new Trip();
        mockTrip.setId(tripId);
        mockTrip.setDriverId(driverId);
        mockTrip.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        mockTrip.setStatus(TripStatus.DRIVER_ARRIVING);
        mockTrip.setPickupGeom(geometryFactory.createPoint(new Coordinate(106.700981, 10.776530)));
        mockTrip.setDropoffGeom(geometryFactory.createPoint(new Coordinate(106.803054, 10.870020)));
    }

    @Test
    @DisplayName("AC-02: Nhận location update từ tài xế -> Lưu MongoDB, cập nhật Redis, broadcast cho khách")
    void testHandleLocationUpdate_Success() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(principal.getName()).thenReturn(driverId.toString());

        when(geofenceService.isWithinPickupGeofence(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(false);

        when(tripEtaService.calculateEta(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(new TripEtaService.EtaResult(180, 842, false, "polyline_abc"));

        DriverLocationUpdateMessage msg = new DriverLocationUpdateMessage(
                tripId, 10.777120, 106.701540, 24.5, 182.0, 12.0, 4.0, 85, false, Instant.now()
        );

        locationController.handleLocationUpdate(msg, headerAccessor, principal);

        // Verify MongoDB save
        verify(gpsTelemetryService, times(1)).saveGpsPoint(
                eq(tripId.toString()), eq(driverId.toString()), eq("ELECTRIC_MOTORBIKE"),
                eq("DRIVER_ARRIVING"), eq(10.777120), eq(106.701540), eq(24.5), eq(182.0),
                eq(12.0), eq(4.0), eq(85), eq(false), any(Instant.class)
        );

        // Verify Redis update
        verify(tripTrackingRedisService, times(1)).updateDriverLocation(
                eq(tripId), eq(driverId), eq(10.777120), eq(106.701540), eq(182.0),
                eq(24.5), eq(85), eq(180), eq(842)
        );

        // Verify WebSocket broadcast to customer topic
        ArgumentCaptor<DriverLocationBroadcastDto> broadcastCaptor = ArgumentCaptor.forClass(DriverLocationBroadcastDto.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/driver-location/" + driverId), broadcastCaptor.capture());

        DriverLocationBroadcastDto broadcast = broadcastCaptor.getValue();
        assertEquals(driverId, broadcast.getDriverId());
        assertEquals(10.777120, broadcast.getLat());
        assertEquals(106.701540, broadcast.getLng());
        assertEquals(182.0, broadcast.getBearing());
        assertEquals(180, broadcast.getEtaSeconds());
        assertEquals(842, broadcast.getDistanceRemainingM());
        assertEquals("DRIVER_ARRIVING", broadcast.getPhase());
    }

    @Test
    @DisplayName("AC-03: Geofence tự động kích hoạt chuyển ARRIVED khi cách điểm đón <= 50m")
    void testHandleLocationUpdate_AutoGeofenceArrival() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(principal.getName()).thenReturn(driverId.toString());

        // Driver arrives within 50m
        when(geofenceService.isWithinPickupGeofence(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(true);

        when(tripEtaService.calculateEta(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(new TripEtaService.EtaResult(0, 30, false, "polyline_abc"));

        DriverLocationUpdateMessage msg = new DriverLocationUpdateMessage(
                tripId, 10.776600, 106.701000, 10.0, 180.0, 10.0, 3.0, 80, false, Instant.now()
        );

        locationController.handleLocationUpdate(msg, headerAccessor, principal);

        // Verify auto arrival was called
        verify(tripService, times(1)).autoArriveAtPickup(tripId);

        // Broadcast phase should reflect ARRIVED
        ArgumentCaptor<DriverLocationBroadcastDto> captor = ArgumentCaptor.forClass(DriverLocationBroadcastDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/driver-location/" + driverId), captor.capture());
        assertEquals("ARRIVED", captor.getValue().getPhase());
    }

    @Test
    @DisplayName("Bảo mật: Từ chối cập nhật vị trí nếu principal không khớp với tài xế của cuốc xe")
    void testHandleLocationUpdate_RejectUnauthorizedDriver() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        // Principal is a different driver
        when(principal.getName()).thenReturn(UUID.randomUUID().toString());

        DriverLocationUpdateMessage msg = new DriverLocationUpdateMessage(
                tripId, 10.777120, 106.701540, 20.0, 180.0, 10.0, 5.0, 90, false, Instant.now()
        );

        locationController.handleLocationUpdate(msg, headerAccessor, principal);

        // Không lưu vào Mongo hay broadcast
        verify(gpsTelemetryService, never()).saveGpsPoint(any(), any(), any(), any(), anyDouble(), anyDouble(), any(), any(), any(), any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}

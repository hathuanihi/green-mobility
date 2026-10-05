package com.greenmobility.modules.trip.controller;

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
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * STOMP WebSocket Controller handling real-time GPS telemetry streaming from drivers.
 * Listens on /app/driver/location-update and broadcasts to /topic/driver-location/{driverId}.
 */
@Controller
public class DriverLocationController {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationController.class);

    private final TripRepository tripRepository;
    private final TripService tripService;
    private final GpsTelemetryService gpsTelemetryService;
    private final TripTrackingRedisService tripTrackingRedisService;
    private final GeofenceService geofenceService;
    private final TripEtaService tripEtaService;
    private final SimpMessagingTemplate messagingTemplate;

    public DriverLocationController(TripRepository tripRepository,
                                    TripService tripService,
                                    GpsTelemetryService gpsTelemetryService,
                                    TripTrackingRedisService tripTrackingRedisService,
                                    GeofenceService geofenceService,
                                    TripEtaService tripEtaService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.tripRepository = tripRepository;
        this.tripService = tripService;
        this.gpsTelemetryService = gpsTelemetryService;
        this.tripTrackingRedisService = tripTrackingRedisService;
        this.geofenceService = geofenceService;
        this.tripEtaService = tripEtaService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Process real-time GPS location update from driver (sent every 3-5 seconds).
     *
     * Flow:
     * 1. Validate driver ownership of the trip
     * 2. Check for mock/fake location flag
     * 3. Persist GPS point in MongoDB time-series
     * 4. Update Redis real-time tracking hash
     * 5. Check geofence (auto-arrive when <= 50m to pickup during DRIVER_ARRIVING)
     * 6. Dynamically recalculate ETA & distance remaining
     * 7. Broadcast to customer topic /topic/driver-location/{driverId}
     */
    @MessageMapping("/driver/location-update")
    public void handleLocationUpdate(@Valid @Payload DriverLocationUpdateMessage message,
                                     SimpMessageHeaderAccessor headerAccessor,
                                     Principal principal) {

        if (message == null || message.getTripId() == null) {
            log.warn("Received invalid location update: message or tripId is null");
            return;
        }

        UUID tripId = message.getTripId();
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            log.warn("Location update rejected: Trip {} not found", tripId);
            return;
        }

        Trip trip = tripOpt.get();
        UUID driverId = trip.getDriverId();

        // 1. Validate driver ownership via STOMP Principal (resolves to driverProfile.id)
        if (principal != null && driverId != null) {
            String principalDriverId = principal.getName();
            if (!driverId.toString().equalsIgnoreCase(principalDriverId)) {
                log.warn("Security warning: Principal {} attempted to send location for driver {} on trip {}",
                        principalDriverId, driverId, tripId);
                return;
            }
        }

        TripStatus status = trip.getStatus();
        if (status != TripStatus.DRIVER_ARRIVING && status != TripStatus.ARRIVED && status != TripStatus.IN_TRIP) {
            log.debug("Trip {} is in status {}, skipping real-time tracking broadcast", tripId, status);
            return;
        }

        // 2. Check mock location flag
        if (Boolean.TRUE.equals(message.getIsMockLocation())) {
            log.warn("SUSPICIOUS: Mock location detected from driver {} for trip {} at ({}, {})",
                    driverId, tripId, message.getLat(), message.getLng());
        }

        Instant timestamp = message.getTimestamp() != null ? message.getTimestamp() : Instant.now();
        String currentPhase = status.name();

        // 3. Save GPS point in MongoDB
        try {
            gpsTelemetryService.saveGpsPoint(
                    tripId.toString(),
                    driverId != null ? driverId.toString() : "",
                    trip.getVehicleType().name(),
                    currentPhase,
                    message.getLat(),
                    message.getLng(),
                    message.getSpeedKmh(),
                    message.getBearing(),
                    message.getAltitude(),
                    message.getAccuracy(),
                    message.getBatteryPercent(),
                    message.getIsMockLocation(),
                    timestamp
            );
        } catch (Exception e) {
            log.error("Failed to save GPS point to MongoDB for trip {}: {}", tripId, e.getMessage());
        }

        // 4. Auto Geofence Detection during DRIVER_ARRIVING (≤ 50m from pickup)
        if (status == TripStatus.DRIVER_ARRIVING && trip.getPickupLat() != null && trip.getPickupLng() != null) {
            boolean withinGeofence = geofenceService.isWithinPickupGeofence(
                    message.getLat(), message.getLng(),
                    trip.getPickupLat(), trip.getPickupLng()
            );

            if (withinGeofence) {
                log.info("Auto-geofence triggered for trip {}: driver arrived at pickup", tripId);
                tripService.autoArriveAtPickup(tripId);
                currentPhase = TripStatus.ARRIVED.name();
            }
        }

        // 5. Dynamic ETA recalculation
        double targetLat;
        double targetLng;
        if (currentPhase.equals(TripStatus.DRIVER_ARRIVING.name())) {
            targetLat = trip.getPickupLat();
            targetLng = trip.getPickupLng();
        } else {
            targetLat = trip.getDropoffLat();
            targetLng = trip.getDropoffLng();
        }

        TripEtaService.EtaResult etaResult = tripEtaService.calculateEta(
                tripId,
                message.getLat(),
                message.getLng(),
                targetLat,
                targetLng,
                trip.getVehicleType().name(),
                message.getSpeedKmh()
        );

        // 6. Update Redis tracking cache
        try {
            tripTrackingRedisService.updateDriverLocation(
                    tripId,
                    driverId,
                    message.getLat(),
                    message.getLng(),
                    message.getBearing(),
                    message.getSpeedKmh(),
                    message.getBatteryPercent(),
                    etaResult.etaSeconds(),
                    etaResult.distanceRemainingM()
            );
        } catch (Exception e) {
            log.error("Failed to update Redis tracking for trip {}: {}", tripId, e.getMessage());
        }

        // 7. Broadcast to customer channel /topic/driver-location/{driverId}
        if (driverId != null) {
            DriverLocationBroadcastDto broadcastDto = new DriverLocationBroadcastDto(
                    driverId,
                    message.getLat(),
                    message.getLng(),
                    message.getBearing(),
                    message.getSpeedKmh(),
                    message.getBatteryPercent(),
                    etaResult.etaSeconds(),
                    etaResult.distanceRemainingM(),
                    currentPhase,
                    timestamp
            );

            messagingTemplate.convertAndSend("/topic/driver-location/" + driverId, broadcastDto);
            log.trace("Broadcasted location for driver {} to /topic/driver-location/{}", driverId, driverId);
        }
    }
}

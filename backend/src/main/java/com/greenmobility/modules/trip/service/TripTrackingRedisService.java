package com.greenmobility.modules.trip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Redis Hash structures for real-time trip tracking state.
 * Provides sub-millisecond reads for WebSocket broadcasts and REST fallback queries.
 *
 * Redis keys:
 * - trip:tracking:{tripId}          → Hash with driver location, ETA, phase
 * - driver:location:latest:{driverId} → Hash with latest GPS coordinates
 * - trip:route:{tripId}             → String with encoded polyline
 */
@Service
public class TripTrackingRedisService {

    private static final Logger log = LoggerFactory.getLogger(TripTrackingRedisService.class);

    private static final String TRACKING_KEY_PREFIX = "trip:tracking:";
    private static final String DRIVER_LOCATION_KEY_PREFIX = "driver:location:latest:";
    private static final String ROUTE_KEY_PREFIX = "trip:route:";
    private static final Duration TRACKING_TTL = Duration.ofHours(2);
    private static final Duration DRIVER_LOCATION_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public TripTrackingRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create initial tracking state when trip moves to DRIVER_ARRIVING.
     */
    public void createTracking(UUID tripId, UUID driverId, double lat, double lng,
                               String phase, Integer etaSeconds, Integer distanceM) {
        String key = TRACKING_KEY_PREFIX + tripId;
        Map<String, String> fields = new HashMap<>();
        fields.put("driverId", driverId.toString());
        fields.put("driverLat", String.valueOf(lat));
        fields.put("driverLng", String.valueOf(lng));
        fields.put("bearing", "0");
        fields.put("speedKmh", "0");
        fields.put("batteryPercent", "100");
        fields.put("lastPingEpoch", String.valueOf(System.currentTimeMillis() / 1000));
        fields.put("etaSeconds", etaSeconds != null ? String.valueOf(etaSeconds) : "0");
        fields.put("distanceRemainingM", distanceM != null ? String.valueOf(distanceM) : "0");
        fields.put("phase", phase);

        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, TRACKING_TTL);
        log.debug("Created tracking: trip={}, driver={}, phase={}", tripId, driverId, phase);
    }

    /**
     * Update driver location from incoming GPS point.
     */
    public void updateDriverLocation(UUID tripId, UUID driverId, double lat, double lng,
                                     Double bearing, Double speedKmh, Integer batteryPercent,
                                     Integer etaSeconds, Integer distanceRemainingM) {
        // Update trip tracking hash
        String trackingKey = TRACKING_KEY_PREFIX + tripId;
        Map<String, String> updates = new HashMap<>();
        updates.put("driverLat", String.valueOf(lat));
        updates.put("driverLng", String.valueOf(lng));
        updates.put("lastPingEpoch", String.valueOf(System.currentTimeMillis() / 1000));
        if (bearing != null) updates.put("bearing", String.valueOf(bearing));
        if (speedKmh != null) updates.put("speedKmh", String.valueOf(speedKmh));
        if (batteryPercent != null) updates.put("batteryPercent", String.valueOf(batteryPercent));
        if (etaSeconds != null) updates.put("etaSeconds", String.valueOf(etaSeconds));
        if (distanceRemainingM != null) updates.put("distanceRemainingM", String.valueOf(distanceRemainingM));

        redisTemplate.opsForHash().putAll(trackingKey, updates);

        // Update driver latest location hash
        String driverKey = DRIVER_LOCATION_KEY_PREFIX + driverId;
        Map<String, String> driverFields = new HashMap<>();
        driverFields.put("lat", String.valueOf(lat));
        driverFields.put("lng", String.valueOf(lng));
        if (bearing != null) driverFields.put("bearing", String.valueOf(bearing));
        if (speedKmh != null) driverFields.put("speed", String.valueOf(speedKmh));
        driverFields.put("updatedAt", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(driverKey, driverFields);
        redisTemplate.expire(driverKey, DRIVER_LOCATION_TTL);
    }

    /**
     * Update the trip phase in tracking hash (e.g., DRIVER_ARRIVING → ARRIVED → IN_TRIP).
     */
    public void updatePhase(UUID tripId, String phase) {
        String key = TRACKING_KEY_PREFIX + tripId;
        redisTemplate.opsForHash().put(key, "phase", phase);
    }

    /**
     * Get current tracking state for a trip (used by REST fallback endpoint).
     */
    public Map<String, String> getTracking(UUID tripId) {
        String key = TRACKING_KEY_PREFIX + tripId;
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * Get the latest known location of a driver.
     */
    public Map<String, String> getDriverLatestLocation(UUID driverId) {
        String key = DRIVER_LOCATION_KEY_PREFIX + driverId;
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * Store the current route polyline for a trip.
     */
    public void saveRoutePolyline(UUID tripId, String polyline) {
        String key = ROUTE_KEY_PREFIX + tripId;
        redisTemplate.opsForValue().set(key, polyline, TRACKING_TTL);
    }

    /**
     * Get the current route polyline for a trip.
     */
    public String getRoutePolyline(UUID tripId) {
        String key = ROUTE_KEY_PREFIX + tripId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Clean up all tracking data when trip completes or is cancelled.
     */
    public void deleteTracking(UUID tripId, UUID driverId) {
        redisTemplate.delete(TRACKING_KEY_PREFIX + tripId);
        redisTemplate.delete(ROUTE_KEY_PREFIX + tripId);
        if (driverId != null) {
            redisTemplate.delete(DRIVER_LOCATION_KEY_PREFIX + driverId);
        }
        log.debug("Deleted tracking data: trip={}, driver={}", tripId, driverId);
    }

    /**
     * Check if tracking data exists for a trip.
     */
    public boolean hasTracking(UUID tripId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TRACKING_KEY_PREFIX + tripId));
    }
}

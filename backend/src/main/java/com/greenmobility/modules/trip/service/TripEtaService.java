package com.greenmobility.modules.trip.service;

import com.greenmobility.common.util.PolylineUtil;
import com.greenmobility.modules.trip.dto.RoutingResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service for dynamic ETA recalculation and route deviation detection.
 * Avoids calling OSRM/Goong on every 3s GPS ping by using fast linear estimation
 * when on-route, while automatically triggering re-routing when deviating > 100m
 * or periodically every 30 seconds.
 */
@Service
public class TripEtaService {

    private static final Logger log = LoggerFactory.getLogger(TripEtaService.class);
    private static final String LAST_ROUTE_CALC_KEY_PREFIX = "trip:eta:last_calc:";
    private static final Duration KEY_TTL = Duration.ofHours(2);
    private static final double URBAN_ROAD_FACTOR = 1.25;
    private static final double DEFAULT_SPEED_MS = 8.33; // ~30 km/h urban speed

    @Value("${green-mobility.tracking.reroute-deviation-m:100}")
    private double rerouteDeviationM = 100.0;

    @Value("${green-mobility.tracking.eta-refresh-interval-s:30}")
    private long etaRefreshIntervalS = 30;

    private final RoutingService routingService;
    private final GeofenceService geofenceService;
    private final TripTrackingRedisService tripTrackingRedisService;
    private final StringRedisTemplate redisTemplate;

    public record EtaResult(int etaSeconds, int distanceRemainingM, boolean isRerouted, String polyline) {}

    public TripEtaService(RoutingService routingService,
                          GeofenceService geofenceService,
                          TripTrackingRedisService tripTrackingRedisService,
                          StringRedisTemplate redisTemplate) {
        this.routingService = routingService;
        this.geofenceService = geofenceService;
        this.tripTrackingRedisService = tripTrackingRedisService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Calculate dynamic ETA and distance remaining for a moving driver.
     * Triggers re-routing if deviating > 100m or if 30s have elapsed since last re-route.
     */
    public EtaResult calculateEta(UUID tripId, double currentLat, double currentLng,
                                  double targetLat, double targetLng, String vehicleType,
                                  Double currentSpeedKmh) {

        String polyline = tripTrackingRedisService.getRoutePolyline(tripId);
        boolean isDeviated = isDriverDeviatedFromRoute(currentLat, currentLng, polyline);
        boolean isIntervalExpired = isRouteRefreshIntervalExpired(tripId);

        if (isDeviated || isIntervalExpired || polyline == null || polyline.isEmpty()) {
            // Full re-route via RoutingService
            log.info("Triggering re-route for trip {}: deviated={}, intervalExpired={}",
                    tripId, isDeviated, isIntervalExpired);

            RoutingResultDto route = routingService.getRoute(
                    currentLat, currentLng, targetLat, targetLng,
                    vehicleType != null ? vehicleType : "ELECTRIC_MOTORBIKE"
            );

            tripTrackingRedisService.saveRoutePolyline(tripId, route.getPolyline());
            recordRouteCalculationTime(tripId);

            int distanceM = route.getDistanceM() != null ? route.getDistanceM() : 0;
            int durationS = route.getDurationS() != null ? route.getDurationS() : 0;

            return new EtaResult(durationS, distanceM, true, route.getPolyline());
        }

        // Fast ETA estimation: distance remaining / effective speed
        double directDistanceM = geofenceService.calculateHaversineDistance(
                currentLat, currentLng, targetLat, targetLng
        );
        int distanceRemainingM = (int) Math.round(directDistanceM * URBAN_ROAD_FACTOR);

        double speedMs;
        if (currentSpeedKmh != null && currentSpeedKmh > 5.0) {
            speedMs = currentSpeedKmh / 3.6;
        } else {
            speedMs = DEFAULT_SPEED_MS; // Fallback to 30 km/h when stopped at traffic light
        }

        int etaSeconds = (int) Math.max(10, Math.round(distanceRemainingM / speedMs));

        return new EtaResult(etaSeconds, distanceRemainingM, false, polyline);
    }

    /**
     * Check if driver is more than 100m away from the nearest point on the current polyline.
     */
    public boolean isDriverDeviatedFromRoute(double currentLat, double currentLng, String polyline) {
        if (polyline == null || polyline.isEmpty()) {
            return true;
        }

        List<PolylineUtil.LatLngPoint> points = PolylineUtil.decode(polyline);
        if (points.isEmpty()) {
            return true;
        }

        double minDistance = Double.MAX_VALUE;
        for (PolylineUtil.LatLngPoint pt : points) {
            double d = geofenceService.calculateHaversineDistance(currentLat, currentLng, pt.lat(), pt.lng());
            if (d < minDistance) {
                minDistance = d;
            }
            if (minDistance <= rerouteDeviationM) {
                return false; // Found a point within threshold, not deviated
            }
        }

        return minDistance > rerouteDeviationM;
    }

    private boolean isRouteRefreshIntervalExpired(UUID tripId) {
        String key = LAST_ROUTE_CALC_KEY_PREFIX + tripId;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return true;
        }
        try {
            long lastCalcTime = Long.parseLong(val);
            return (System.currentTimeMillis() - lastCalcTime) >= (etaRefreshIntervalS * 1000);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void recordRouteCalculationTime(UUID tripId) {
        String key = LAST_ROUTE_CALC_KEY_PREFIX + tripId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), KEY_TTL);
    }
}

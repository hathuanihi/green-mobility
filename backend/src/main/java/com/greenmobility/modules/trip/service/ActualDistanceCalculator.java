package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.trip.entity.TripGpsPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Calculates actual trip distance from sequential GPS telemetry points.
 * Applies noise filtering to remove GPS anomalies (jumps, speed spikes, low accuracy).
 *
 * Algorithm: Sequential Haversine Summation with noise filtering.
 * D_actual = Σ Haversine(P_i, P_{i+1}) for all valid consecutive point pairs.
 */
@Service
public class ActualDistanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(ActualDistanceCalculator.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    // Noise filter thresholds
    private static final double MAX_SPEED_KMH = 120.0;         // Reject points with instantaneous speed > 120 km/h
    private static final double MAX_TELEPORT_DISTANCE_M = 500.0; // Reject jumps > 500m in ≤ 3 seconds
    private static final double MAX_TELEPORT_TIME_S = 3.0;       // Time window for teleportation detection
    private static final double MAX_ACCURACY_M = 50.0;           // Reject points with GPS accuracy > 50m

    private final GeofenceService geofenceService;

    public ActualDistanceCalculator(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /**
     * Calculate the actual distance traveled from a sequence of GPS points.
     * Applies noise filtering to exclude anomalous data.
     *
     * @param points GPS points ordered chronologically (ascending timestamp)
     * @return total distance in meters
     */
    public int calculateActualDistance(List<TripGpsPoint> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        double totalDistance = 0;
        int validSegments = 0;
        int filteredSegments = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            TripGpsPoint p1 = points.get(i);
            TripGpsPoint p2 = points.get(i + 1);

            // Skip points with low GPS accuracy
            if (isLowAccuracy(p2)) {
                filteredSegments++;
                continue;
            }

            double segmentDistance = geofenceService.calculateHaversineDistance(
                    p1.getLat(), p1.getLng(), p2.getLat(), p2.getLng());

            double timeDelta = getTimeDeltaSeconds(p1, p2);

            // Filter: Speed anomaly (> 120 km/h)
            if (timeDelta > 0 && isSpeedAnomaly(segmentDistance, timeDelta)) {
                filteredSegments++;
                log.debug("Filtered speed anomaly: segment {}→{}, distance={:.0f}m, time={:.1f}s, speed={:.1f}km/h",
                        i, i + 1, segmentDistance, timeDelta, (segmentDistance / timeDelta) * 3.6);
                continue;
            }

            // Filter: Teleportation (> 500m in ≤ 3s)
            if (isTeleportation(segmentDistance, timeDelta)) {
                filteredSegments++;
                log.debug("Filtered teleportation: segment {}→{}, distance={:.0f}m, time={:.1f}s",
                        i, i + 1, segmentDistance, timeDelta);
                continue;
            }

            totalDistance += segmentDistance;
            validSegments++;
        }

        int result = (int) Math.round(totalDistance);
        log.info("Distance calculation: {} valid segments, {} filtered, total={}m",
                validSegments, filteredSegments, result);
        return result;
    }

    /**
     * Validate the ratio between actual and estimated distance.
     * Used for sanity checking and fraud detection.
     */
    public DistanceValidation validateDistanceRatio(int actualDistanceM, int estimatedDistanceM) {
        if (estimatedDistanceM <= 0) {
            return new DistanceValidation(0, true, false);
        }

        double ratio = (double) actualDistanceM / estimatedDistanceM;
        boolean isValid = ratio >= 0.85 && ratio <= 1.30;
        boolean isFraudSuspect = ratio > 1.30 || ratio < 0.50;

        log.info("Distance ratio validation: actual={}m, estimated={}m, ratio={:.2f}, valid={}, fraudSuspect={}",
                actualDistanceM, estimatedDistanceM, ratio, isValid, isFraudSuspect);

        return new DistanceValidation(ratio, isValid, isFraudSuspect);
    }

    /**
     * Get a fallback distance when actual GPS data is insufficient.
     * Uses the estimated distance from OSRM/Goong.
     */
    public int getFallbackDistance(int actualDistanceM, int estimatedDistanceM) {
        DistanceValidation validation = validateDistanceRatio(actualDistanceM, estimatedDistanceM);
        if (validation.ratio() < 0.85 && actualDistanceM > 0) {
            // Actual too short (possibly missing GPS data) → use estimated
            log.warn("Actual distance too short ({:.0f}% of estimated), using estimated as fallback",
                    validation.ratio() * 100);
            return estimatedDistanceM;
        }
        return actualDistanceM;
    }

    // --- Private Helper Methods ---

    private boolean isLowAccuracy(TripGpsPoint point) {
        return point.getAccuracy() != null && point.getAccuracy() > MAX_ACCURACY_M;
    }

    private boolean isSpeedAnomaly(double distanceMeters, double timeDeltaSeconds) {
        double speedKmh = (distanceMeters / timeDeltaSeconds) * 3.6;
        return speedKmh > MAX_SPEED_KMH;
    }

    private boolean isTeleportation(double distanceMeters, double timeDeltaSeconds) {
        return distanceMeters > MAX_TELEPORT_DISTANCE_M && timeDeltaSeconds <= MAX_TELEPORT_TIME_S;
    }

    private double getTimeDeltaSeconds(TripGpsPoint p1, TripGpsPoint p2) {
        if (p1.getTimestamp() == null || p2.getTimestamp() == null) {
            return 5.0; // Default 5s interval
        }
        return Duration.between(p1.getTimestamp(), p2.getTimestamp()).toMillis() / 1000.0;
    }

    /**
     * Result record for distance ratio validation.
     */
    public record DistanceValidation(double ratio, boolean isValid, boolean isFraudSuspect) {}
}

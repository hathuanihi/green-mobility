package com.greenmobility.modules.trip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for detecting geofence proximity events during trip execution.
 * Uses Haversine formula to compute great-circle distance between two GPS coordinates.
 */
@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public static final double GEOFENCE_ARRIVED_RADIUS_M = 50.0;
    public static final double MANUAL_ARRIVE_MAX_DISTANCE_M = 200.0;
    public static final double REROUTE_DEVIATION_M = 100.0;

    @Value("${green-mobility.tracking.geofence-arrived-radius-m:50}")
    private double geofenceArrivedRadiusM = GEOFENCE_ARRIVED_RADIUS_M;

    @Value("${green-mobility.tracking.manual-arrive-max-distance-m:200}")
    private double manualArriveMaxDistanceM = MANUAL_ARRIVE_MAX_DISTANCE_M;

    @Value("${green-mobility.tracking.reroute-deviation-m:100}")
    private double rerouteDeviationM = REROUTE_DEVIATION_M;

    /**
     * Check if a location is within a given radius of a target location using Haversine formula.
     */
    public boolean checkGeofence(double currentLat, double currentLng, double targetLat, double targetLng, double radiusMeters) {
        return calculateHaversineDistance(currentLat, currentLng, targetLat, targetLng) <= radiusMeters;
    }

    /**
     * Check if the driver has entered the geofence around the pickup point (auto-arrival ≤ 50m).
     */
    public boolean isWithinPickupGeofence(double driverLat, double driverLng,
                                          double pickupLat, double pickupLng) {
        double distance = calculateHaversineDistance(driverLat, driverLng, pickupLat, pickupLng);
        boolean within = distance <= geofenceArrivedRadiusM;
        if (within) {
            log.info("Geofence triggered: driver at ({},{}) is {:.1f}m from pickup ({},{}) [threshold: {}m]",
                    driverLat, driverLng, distance, pickupLat, pickupLng, geofenceArrivedRadiusM);
        }
        return within;
    }

    /**
     * Check if the driver is close enough for manual arrival confirmation (≤ 200m).
     */
    public boolean isWithinManualArriveDistance(double driverLat, double driverLng,
                                                double pickupLat, double pickupLng) {
        double distance = calculateHaversineDistance(driverLat, driverLng, pickupLat, pickupLng);
        return distance <= manualArriveMaxDistanceM;
    }

    /**
     * Get the distance from current position to target for UI display.
     */
    public double getDistanceTo(double fromLat, double fromLng, double toLat, double toLng) {
        return calculateHaversineDistance(fromLat, fromLng, toLat, toLng);
    }

    /**
     * Check if driver has deviated from the planned route beyond threshold (for re-routing).
     */
    public boolean hasDeviatedFromRoute(double driverLat, double driverLng,
                                         double nearestRouteLat, double nearestRouteLng) {
        double distance = calculateHaversineDistance(driverLat, driverLng, nearestRouteLat, nearestRouteLng);
        return distance > rerouteDeviationM;
    }

    /**
     * Calculate the great-circle distance between two GPS coordinates using the Haversine formula.
     *
     * Formula: d = 2R * arcsin(sqrt(sin²(Δφ/2) + cos(φ1)*cos(φ2)*sin²(Δλ/2)))
     *
     * @return distance in meters
     */
    public double calculateHaversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    // Getters for configuration values
    public double getGeofenceArrivedRadiusM() { return geofenceArrivedRadiusM; }
    public double getManualArriveMaxDistanceM() { return manualArriveMaxDistanceM; }
    public double getRerouteDeviationM() { return rerouteDeviationM; }
}

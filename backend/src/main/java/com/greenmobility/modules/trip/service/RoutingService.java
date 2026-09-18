package com.greenmobility.modules.trip.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double URBAN_ROAD_FACTOR = 1.25; // Đường đô thị uốn khúc so với đường chim bay
    private static final double AVERAGE_URBAN_SPEED_MPS = 8.33; // ~30 km/h

    public record RouteInfo(int distanceMeters, int durationSeconds, String polyline) {}

    public RouteInfo calculateRoute(double startLat, double startLng, double endLat, double endLng) {
        // Haversine formula
        double dLat = Math.toRadians(endLat - startLat);
        double dLng = Math.toRadians(endLng - startLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightDistance = EARTH_RADIUS_METERS * c;

        int estimatedDistanceMeters = (int) Math.round(straightDistance * URBAN_ROAD_FACTOR);
        if (estimatedDistanceMeters < 500) {
            estimatedDistanceMeters = 500; // Minimum 500m
        }

        int estimatedDurationSeconds = (int) Math.round(estimatedDistanceMeters / AVERAGE_URBAN_SPEED_MPS) + 60;

        // Simple encoded polyline representation for client maps
        String polyline = encodeSimplePolyline(startLat, startLng, endLat, endLng);

        return new RouteInfo(estimatedDistanceMeters, estimatedDurationSeconds, polyline);
    }

    public double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private String encodeSimplePolyline(double lat1, double lng1, double lat2, double lng2) {
        return String.format("_lat1=%f_lng1=%f_lat2=%f_lng2=%f", lat1, lng1, lat2, lng2);
    }
}

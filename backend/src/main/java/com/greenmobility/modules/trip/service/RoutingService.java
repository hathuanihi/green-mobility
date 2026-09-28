package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.map.service.GoongMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final GoongMapService goongMapService;

    public record RouteInfo(int distanceMeters, int durationSeconds, String polyline) {}

    public RoutingService() {
        this.goongMapService = new GoongMapService();
    }

    public RoutingService(GoongMapService goongMapService) {
        this.goongMapService = goongMapService;
    }

    public RouteInfo calculateRoute(double startLat, double startLng, double endLat, double endLng) {
        return calculateRoute(startLat, startLng, endLat, endLng, "ELECTRIC_MOTORBIKE");
    }

    public RouteInfo calculateRoute(double startLat, double startLng, double endLat, double endLng, String vehicleType) {
        GoongMapService.RouteResult route = goongMapService.calculateRoute(startLat, startLng, endLat, endLng, vehicleType);
        return new RouteInfo(route.distanceMeters(), route.durationSeconds(), route.polyline());
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
}


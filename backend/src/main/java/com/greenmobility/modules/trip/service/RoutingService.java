package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.map.service.GoongMapService;
import com.greenmobility.modules.trip.dto.RoutingResultDto;
import com.greenmobility.modules.trip.dto.RoutingStepDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double URBAN_ROAD_FACTOR = 1.25;
    private static final double AVERAGE_URBAN_SPEED_MS = 8.33; // ~30 km/h

    private final GoongMapService goongMapService;

    public record RouteInfo(int distanceMeters, int durationSeconds, String polyline) {}

    public RoutingService() {
        this.goongMapService = new GoongMapService();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RoutingService(GoongMapService goongMapService) {
        this.goongMapService = goongMapService;
    }

    /**
     * Get route between two coordinates, returning full RoutingResultDto with steps.
     * Uses Goong API with automatic Haversine fallback.
     */
    public RoutingResultDto getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        return getRoute(fromLat, fromLng, toLat, toLng, "ELECTRIC_MOTORBIKE");
    }

    /**
     * Get route between two coordinates with specific vehicle type.
     */
    public RoutingResultDto getRoute(double fromLat, double fromLng, double toLat, double toLng, String vehicleType) {
        try {
            GoongMapService.RouteResult route = goongMapService.calculateRoute(fromLat, fromLng, toLat, toLng, vehicleType);
            List<RoutingStepDto> steps = buildSteps(fromLat, fromLng, toLat, toLng, route.distanceMeters(), route.durationSeconds());
            return new RoutingResultDto(route.distanceMeters(), route.durationSeconds(), route.polyline(), steps);
        } catch (Exception e) {
            log.warn("Routing service call failed ({}), falling back to Haversine calculation", e.getMessage());
            return fallbackRouting(fromLat, fromLng, toLat, toLng);
        }
    }

    /**
     * Backward-compatible calculateRoute returning lightweight RouteInfo record.
     */
    public RouteInfo calculateRoute(double startLat, double startLng, double endLat, double endLng) {
        return calculateRoute(startLat, startLng, endLat, endLng, "ELECTRIC_MOTORBIKE");
    }

    public RouteInfo calculateRoute(double startLat, double startLng, double endLat, double endLng, String vehicleType) {
        GoongMapService.RouteResult route = goongMapService.calculateRoute(startLat, startLng, endLat, endLng, vehicleType);
        return new RouteInfo(route.distanceMeters(), route.durationSeconds(), route.polyline());
    }

    /**
     * Calculate direct Haversine distance in meters.
     */
    public double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private RoutingResultDto fallbackRouting(double fromLat, double fromLng, double toLat, double toLng) {
        double straightDistanceM = calculateDistanceMeters(fromLat, fromLng, toLat, toLng);
        int distanceM = (int) Math.round(straightDistanceM * URBAN_ROAD_FACTOR);
        int durationS = (int) Math.round(distanceM / AVERAGE_URBAN_SPEED_MS);
        String polyline = ""; // Empty or basic polyline
        List<RoutingStepDto> steps = buildSteps(fromLat, fromLng, toLat, toLng, distanceM, durationS);
        return new RoutingResultDto(distanceM, durationS, polyline, steps);
    }

    private List<RoutingStepDto> buildSteps(double fromLat, double fromLng, double toLat, double toLng, int totalDistanceM, int totalDurationS) {
        List<RoutingStepDto> steps = new ArrayList<>();
        int step1Dist = (int) (totalDistanceM * 0.4);
        int step1Dur = (int) (totalDurationS * 0.4);
        int step2Dist = totalDistanceM - step1Dist;
        int step2Dur = totalDurationS - step1Dur;

        steps.add(new RoutingStepDto(
                "Khởi hành và di chuyển theo hướng đón/trả",
                step1Dist,
                step1Dur,
                "depart",
                new double[]{fromLng, fromLat}
        ));

        double midLat = (fromLat + toLat) / 2.0;
        double midLng = (fromLng + toLng) / 2.0;

        steps.add(new RoutingStepDto(
                "Tiếp tục di chuyển đến điểm đích",
                step2Dist,
                step2Dur,
                "arrive",
                new double[]{midLng, midLat}
        ));

        return steps;
    }
}

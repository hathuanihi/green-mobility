package com.greenmobility.modules.trip.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TripRouteResponseDto {

    private UUID tripId;
    private String phase;
    private EndpointInfo origin;
    private EndpointInfo destination;
    private String routePolyline;
    private Integer distanceM;
    private Integer durationS;
    private List<RoutingStepDto> steps;
    private Instant timestamp = Instant.now();

    public TripRouteResponseDto() {}

    public TripRouteResponseDto(UUID tripId, String phase, EndpointInfo origin, EndpointInfo destination,
                                String routePolyline, Integer distanceM, Integer durationS,
                                List<RoutingStepDto> steps) {
        this.tripId = tripId;
        this.phase = phase;
        this.origin = origin;
        this.destination = destination;
        this.routePolyline = routePolyline;
        this.distanceM = distanceM;
        this.durationS = durationS;
        this.steps = steps;
        this.timestamp = Instant.now();
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public EndpointInfo getOrigin() { return origin; }
    public void setOrigin(EndpointInfo origin) { this.origin = origin; }

    public EndpointInfo getDestination() { return destination; }
    public void setDestination(EndpointInfo destination) { this.destination = destination; }

    public String getRoutePolyline() { return routePolyline; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }

    public Integer getDurationS() { return durationS; }
    public void setDurationS(Integer durationS) { this.durationS = durationS; }

    public List<RoutingStepDto> getSteps() { return steps; }
    public void setSteps(List<RoutingStepDto> steps) { this.steps = steps; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static class EndpointInfo {
        private Double lat;
        private Double lng;
        private String label;

        public EndpointInfo() {}
        public EndpointInfo(Double lat, Double lng, String label) {
            this.lat = lat;
            this.lng = lng;
            this.label = label;
        }

        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }

        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}

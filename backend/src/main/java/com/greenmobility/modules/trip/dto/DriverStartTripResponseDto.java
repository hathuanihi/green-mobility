package com.greenmobility.modules.trip.dto;

import java.time.Instant;
import java.util.UUID;

public class DriverStartTripResponseDto {

    private UUID tripId;
    private String status;
    private Instant startedAt;
    private RoutingResultDto routing;
    private LocationInfo dropoff;

    public DriverStartTripResponseDto() {}

    public DriverStartTripResponseDto(UUID tripId, String status, Instant startedAt, RoutingResultDto routing, LocationInfo dropoff) {
        this.tripId = tripId;
        this.status = status;
        this.startedAt = startedAt;
        this.routing = routing;
        this.dropoff = dropoff;
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public RoutingResultDto getRouting() { return routing; }
    public void setRouting(RoutingResultDto routing) { this.routing = routing; }

    public LocationInfo getDropoff() { return dropoff; }
    public void setDropoff(LocationInfo dropoff) { this.dropoff = dropoff; }

    public static class LocationInfo {
        private String address;
        private Double lat;
        private Double lng;

        public LocationInfo() {}
        public LocationInfo(String address, Double lat, Double lng) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
    }
}

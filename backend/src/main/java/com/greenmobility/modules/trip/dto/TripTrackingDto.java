package com.greenmobility.modules.trip.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for trip tracking state (REST fallback when WebSocket is disconnected).
 * Used by GET /trips/{tripId}/tracking
 */
public class TripTrackingDto {

    private String tripId;
    private String tripCode;
    private String status;

    // Driver info
    private DriverSummaryDto driver;

    // Real-time tracking data
    private TrackingData tracking;

    // Locations
    private LocationDto pickup;
    private LocationDto dropoff;

    private Instant timestamp = Instant.now();

    public TripTrackingDto() {}

    // Getters and Setters
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public DriverSummaryDto getDriver() { return driver; }
    public void setDriver(DriverSummaryDto driver) { this.driver = driver; }

    public TrackingData getTracking() { return tracking; }
    public void setTracking(TrackingData tracking) { this.tracking = tracking; }

    public LocationDto getPickup() { return pickup; }
    public void setPickup(LocationDto pickup) { this.pickup = pickup; }

    public LocationDto getDropoff() { return dropoff; }
    public void setDropoff(LocationDto dropoff) { this.dropoff = dropoff; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    /**
     * Real-time tracking data from Redis.
     */
    public static class TrackingData {
        private Double driverLat;
        private Double driverLng;
        private Double bearing;
        private Double speedKmh;
        private Integer batteryPercent;
        private Integer etaSeconds;
        private Integer distanceRemainingM;
        private String routePolyline;
        private String phase;
        private Instant lastUpdatedAt;

        public TrackingData() {}

        public Double getDriverLat() { return driverLat; }
        public void setDriverLat(Double driverLat) { this.driverLat = driverLat; }

        public Double getDriverLng() { return driverLng; }
        public void setDriverLng(Double driverLng) { this.driverLng = driverLng; }

        public Double getBearing() { return bearing; }
        public void setBearing(Double bearing) { this.bearing = bearing; }

        public Double getSpeedKmh() { return speedKmh; }
        public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }

        public Integer getBatteryPercent() { return batteryPercent; }
        public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }

        public Integer getEtaSeconds() { return etaSeconds; }
        public void setEtaSeconds(Integer etaSeconds) { this.etaSeconds = etaSeconds; }

        public Integer getDistanceRemainingM() { return distanceRemainingM; }
        public void setDistanceRemainingM(Integer distanceRemainingM) { this.distanceRemainingM = distanceRemainingM; }

        public String getRoutePolyline() { return routePolyline; }
        public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }

        public Instant getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    }

    /**
     * Simple location DTO with address, lat, lng.
     */
    public static class LocationDto {
        private String address;
        private Double lat;
        private Double lng;

        public LocationDto() {}

        public LocationDto(String address, Double lat, Double lng) {
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

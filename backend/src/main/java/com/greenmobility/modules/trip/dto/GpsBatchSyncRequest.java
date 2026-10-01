package com.greenmobility.modules.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for batch GPS sync (offline sync when driver had no connectivity).
 * POST /driver/trips/{tripId}/sync-gps-batch
 */
public class GpsBatchSyncRequest {

    @NotEmpty(message = "GPS points list cannot be empty")
    @Valid
    private List<GpsPointPayload> points;

    public GpsBatchSyncRequest() {}

    public List<GpsPointPayload> getPoints() { return points; }
    public void setPoints(List<GpsPointPayload> points) { this.points = points; }

    /**
     * Individual GPS point in the batch payload.
     */
    public static class GpsPointPayload {

        @NotNull(message = "Latitude is required")
        private Double lat;

        @NotNull(message = "Longitude is required")
        private Double lng;

        private Double speedKmh;
        private Double bearing;
        private Double accuracy;
        private Integer batteryPercent;
        private Boolean isMockLocation;
        private Instant timestamp;

        public GpsPointPayload() {}

        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }

        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }

        public Double getSpeedKmh() { return speedKmh; }
        public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }

        public Double getBearing() { return bearing; }
        public void setBearing(Double bearing) { this.bearing = bearing; }

        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

        public Integer getBatteryPercent() { return batteryPercent; }
        public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }

        public Boolean getIsMockLocation() { return isMockLocation; }
        public void setIsMockLocation(Boolean mockLocation) { isMockLocation = mockLocation; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
}

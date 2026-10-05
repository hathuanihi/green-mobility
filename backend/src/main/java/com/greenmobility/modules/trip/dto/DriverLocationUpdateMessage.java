package com.greenmobility.modules.trip.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * STOMP payload sent by driver app on /app/driver/location-update every 3-5 seconds.
 */
public class DriverLocationUpdateMessage {

    @NotNull(message = "Trip ID is required")
    private UUID tripId;

    @NotNull(message = "Latitude is required")
    private Double lat;

    @NotNull(message = "Longitude is required")
    private Double lng;

    private Double speedKmh;
    private Double bearing;
    private Double altitude;
    private Double accuracy;
    private Integer batteryPercent;
    private Boolean isMockLocation = false;
    private Instant timestamp;

    public DriverLocationUpdateMessage() {}

    public DriverLocationUpdateMessage(UUID tripId, Double lat, Double lng, Double speedKmh,
                                       Double bearing, Double altitude, Double accuracy,
                                       Integer batteryPercent, Boolean isMockLocation,
                                       Instant timestamp) {
        this.tripId = tripId;
        this.lat = lat;
        this.lng = lng;
        this.speedKmh = speedKmh;
        this.bearing = bearing;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.batteryPercent = batteryPercent;
        this.isMockLocation = isMockLocation;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }

    public Double getBearing() { return bearing; }
    public void setBearing(Double bearing) { this.bearing = bearing; }

    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

    public Integer getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }

    public Boolean getIsMockLocation() { return isMockLocation; }
    public void setIsMockLocation(Boolean mockLocation) { isMockLocation = mockLocation; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

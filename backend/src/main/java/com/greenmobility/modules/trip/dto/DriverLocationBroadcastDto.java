package com.greenmobility.modules.trip.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Broadcast DTO pushed to customer on /topic/driver-location/{driverId} every 3-5s.
 */
public class DriverLocationBroadcastDto {

    private UUID driverId;
    private Double lat;
    private Double lng;
    private Double bearing;
    private Double speedKmh;
    private Integer batteryPercent;
    private Integer etaSeconds;
    private Integer distanceRemainingM;
    private String phase;
    private Instant timestamp;

    public DriverLocationBroadcastDto() {}

    public DriverLocationBroadcastDto(UUID driverId, Double lat, Double lng, Double bearing,
                                      Double speedKmh, Integer batteryPercent, Integer etaSeconds,
                                      Integer distanceRemainingM, String phase, Instant timestamp) {
        this.driverId = driverId;
        this.lat = lat;
        this.lng = lng;
        this.bearing = bearing;
        this.speedKmh = speedKmh;
        this.batteryPercent = batteryPercent;
        this.etaSeconds = etaSeconds;
        this.distanceRemainingM = distanceRemainingM;
        this.phase = phase;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

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

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

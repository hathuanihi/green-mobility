package com.greenmobility.modules.trip.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;

/**
 * MongoDB document for GPS telemetry data collected during trip execution.
 * Stored in time-series collection "trip_gps_points" in greenmobility_telemetry database.
 * Each document represents a single GPS ping from a driver during DRIVER_ARRIVING or IN_TRIP phase.
 */
@Document(collection = "trip_gps_points")
@CompoundIndex(name = "idx_trip_timeline", def = "{'tripId': 1, 'timestamp': 1}")
@CompoundIndex(name = "idx_driver_latest", def = "{'driverId': 1, 'timestamp': -1}")
public class TripGpsPoint {

    @Id
    private String id;

    @Field("tripId")
    private String tripId;

    @Field("driverId")
    private String driverId;

    @Field("vehicleType")
    private String vehicleType;

    /**
     * Trip phase when this GPS point was recorded.
     * Values: "DRIVER_ARRIVING" or "IN_TRIP"
     */
    @Field("phase")
    private String phase;

    /**
     * GeoJSON Point: [longitude, latitude] following MongoDB 2dsphere standard.
     */
    @GeoSpatialIndexed
    @Field("location")
    private GeoJsonPoint location;

    @Field("speedKmh")
    private Double speedKmh;

    @Field("bearing")
    private Double bearing;

    @Field("altitude")
    private Double altitude;

    @Field("accuracy")
    private Double accuracy;

    @Field("batteryPercent")
    private Integer batteryPercent;

    @Field("isMockLocation")
    private Boolean isMockLocation;

    @Field("timestamp")
    private Instant timestamp;

    public TripGpsPoint() {}

    public TripGpsPoint(String tripId, String driverId, String vehicleType, String phase,
                        double lng, double lat, Double speedKmh, Double bearing,
                        Double altitude, Double accuracy, Integer batteryPercent,
                        Boolean isMockLocation, Instant timestamp) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.vehicleType = vehicleType;
        this.phase = phase;
        this.location = new GeoJsonPoint(lng, lat);
        this.speedKmh = speedKmh;
        this.bearing = bearing;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.batteryPercent = batteryPercent;
        this.isMockLocation = isMockLocation;
        this.timestamp = timestamp;
    }

    // Convenience coordinate getters
    public double getLat() {
        return location != null ? location.getY() : 0;
    }

    public double getLng() {
        return location != null ? location.getX() : 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }

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

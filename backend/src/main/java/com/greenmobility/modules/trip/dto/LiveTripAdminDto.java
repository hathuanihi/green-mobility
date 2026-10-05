package com.greenmobility.modules.trip.dto;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Admin Live Tracking Dashboard.
 * Combines core trip metadata with real-time GPS tracking snapshot from Redis.
 */
public class LiveTripAdminDto {

    private UUID tripId;
    private String tripCode;
    private TripStatus status;
    private VehicleType vehicleType;

    // Pickup & Dropoff
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;

    // Customer info
    private String customerName;
    private String customerPhone;

    // Driver & Vehicle info
    private DriverSummaryDto driver;

    // Real-time tracking from Redis
    private Double driverLat;
    private Double driverLng;
    private Double bearing;
    private Double speedKmh;
    private Integer batteryPercent;
    private Integer etaSeconds;
    private Integer distanceRemainingM;
    private String phase;
    private String routePolyline;
    private Long lastPingEpoch;

    // Timestamps & Metrics
    private Instant requestedAt;
    private Instant matchedAt;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal fareAmountVnd;
    private BigDecimal co2SavedGrams;

    public LiveTripAdminDto() {}

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }

    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public Double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(Double dropoffLat) { this.dropoffLat = dropoffLat; }

    public Double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(Double dropoffLng) { this.dropoffLng = dropoffLng; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public DriverSummaryDto getDriver() { return driver; }
    public void setDriver(DriverSummaryDto driver) { this.driver = driver; }

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

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getRoutePolyline() { return routePolyline; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

    public Long getLastPingEpoch() { return lastPingEpoch; }
    public void setLastPingEpoch(Long lastPingEpoch) { this.lastPingEpoch = lastPingEpoch; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getMatchedAt() { return matchedAt; }
    public void setMatchedAt(Instant matchedAt) { this.matchedAt = matchedAt; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public BigDecimal getFareAmountVnd() { return fareAmountVnd; }
    public void setFareAmountVnd(BigDecimal fareAmountVnd) { this.fareAmountVnd = fareAmountVnd; }

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }
}

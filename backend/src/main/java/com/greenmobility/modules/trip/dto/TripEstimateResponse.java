package com.greenmobility.modules.trip.dto;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;

import java.math.BigDecimal;

public class TripEstimateResponse {

    private VehicleType vehicleType;
    private Integer distanceMeters;
    private Double distanceKm;
    private Integer durationSeconds;
    private Integer durationMinutes;
    private BigDecimal fareAmountVnd;
    private CarbonEstimateDto carbonEstimate;
    private String routePolyline;

    public TripEstimateResponse() {}

    public TripEstimateResponse(VehicleType vehicleType, Integer distanceMeters, Double distanceKm,
                                Integer durationSeconds, Integer durationMinutes, BigDecimal fareAmountVnd,
                                CarbonEstimateDto carbonEstimate, String routePolyline) {
        this.vehicleType = vehicleType;
        this.distanceMeters = distanceMeters;
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.durationMinutes = durationMinutes;
        this.fareAmountVnd = fareAmountVnd;
        this.carbonEstimate = carbonEstimate;
        this.routePolyline = routePolyline;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getFareAmountVnd() {
        return fareAmountVnd;
    }

    public void setFareAmountVnd(BigDecimal fareAmountVnd) {
        this.fareAmountVnd = fareAmountVnd;
    }

    public CarbonEstimateDto getCarbonEstimate() {
        return carbonEstimate;
    }

    public void setCarbonEstimate(CarbonEstimateDto carbonEstimate) {
        this.carbonEstimate = carbonEstimate;
    }

    public String getRoutePolyline() {
        return routePolyline;
    }

    public void setRoutePolyline(String routePolyline) {
        this.routePolyline = routePolyline;
    }
}

package com.greenmobility.modules.trip.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DriverSummaryDto {

    private UUID driverId;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private BigDecimal ratingAvg;
    private String vehicleModel;
    private String licensePlate;
    private Double currentLat;
    private Double currentLng;

    public DriverSummaryDto() {}

    public DriverSummaryDto(UUID driverId, String fullName, String phoneNumber, String avatarUrl,
                            BigDecimal ratingAvg, String vehicleModel, String licensePlate,
                            Double currentLat, Double currentLng) {
        this.driverId = driverId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.ratingAvg = ratingAvg;
        this.vehicleModel = vehicleModel;
        this.licensePlate = licensePlate;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(Double currentLng) {
        this.currentLng = currentLng;
    }
}

package com.greenmobility.modules.drivervehicle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AdminDriverResponse {

    private UUID driverId;
    private String fullName;
    private String phoneNumber;
    private String citizenId;
    private String licenseNumber;
    private String driverLicenseNumber;
    private String vehicleModel;
    private String licensePlate;
    private BigDecimal batteryCapacityKwh;
    private String kycStatus;
    private Instant submittedAt;

    public AdminDriverResponse() {}

    public AdminDriverResponse(UUID driverId, String fullName, String phoneNumber, String citizenId,
                               String driverLicenseNumber, String vehicleModel, String licensePlate,
                               BigDecimal batteryCapacityKwh, String kycStatus, Instant submittedAt) {
        this.driverId = driverId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.citizenId = citizenId;
        this.licenseNumber = driverLicenseNumber;
        this.driverLicenseNumber = driverLicenseNumber;
        this.vehicleModel = vehicleModel;
        this.licensePlate = licensePlate;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.kycStatus = kycStatus;
        this.submittedAt = submittedAt;
    }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getLicenseNumber() { return licenseNumber != null ? licenseNumber : driverLicenseNumber; }
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
        this.driverLicenseNumber = licenseNumber;
    }

    public String getDriverLicenseNumber() { return driverLicenseNumber != null ? driverLicenseNumber : licenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) {
        this.driverLicenseNumber = driverLicenseNumber;
        this.licenseNumber = driverLicenseNumber;
    }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public BigDecimal getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public void setBatteryCapacityKwh(BigDecimal batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}

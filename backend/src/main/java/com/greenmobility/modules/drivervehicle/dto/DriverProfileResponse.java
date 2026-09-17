package com.greenmobility.modules.drivervehicle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DriverProfileResponse {

    private UUID driverId;
    private UUID userId;
    private String fullName;
    private String phoneNumber;
    private String citizenId;
    private String driverLicenseNumber;
    private String licenseClass;
    private String kycStatus;
    private String kycRejectionReason;
    private String citizenCardFrontUrl;
    private String citizenCardBackUrl;
    private String driverLicenseImageUrl;
    private String facePortraitUrl;
    private Boolean isActiveShift;
    private BigDecimal ratingAvg;
    private Integer totalTripsCompleted;
    private BigDecimal totalCo2SavedKg;
    private VehicleResponse vehicle;
    private Instant createdAt;

    public DriverProfileResponse() {}

    public DriverProfileResponse(UUID driverId, UUID userId, String fullName, String phoneNumber,
                                 String citizenId, String driverLicenseNumber, String licenseClass,
                                 String kycStatus, String kycRejectionReason,
                                 String citizenCardFrontUrl, String citizenCardBackUrl,
                                 String driverLicenseImageUrl, String facePortraitUrl,
                                 Boolean isActiveShift, BigDecimal ratingAvg, Integer totalTripsCompleted,
                                 BigDecimal totalCo2SavedKg, VehicleResponse vehicle, Instant createdAt) {
        this.driverId = driverId;
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.citizenId = citizenId;
        this.driverLicenseNumber = driverLicenseNumber;
        this.licenseClass = licenseClass;
        this.kycStatus = kycStatus;
        this.kycRejectionReason = kycRejectionReason;
        this.citizenCardFrontUrl = citizenCardFrontUrl;
        this.citizenCardBackUrl = citizenCardBackUrl;
        this.driverLicenseImageUrl = driverLicenseImageUrl;
        this.facePortraitUrl = facePortraitUrl;
        this.isActiveShift = isActiveShift;
        this.ratingAvg = ratingAvg;
        this.totalTripsCompleted = totalTripsCompleted;
        this.totalCo2SavedKg = totalCo2SavedKg;
        this.vehicle = vehicle;
        this.createdAt = createdAt;
    }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public String getKycRejectionReason() { return kycRejectionReason; }
    public void setKycRejectionReason(String kycRejectionReason) { this.kycRejectionReason = kycRejectionReason; }

    public String getCitizenCardFrontUrl() { return citizenCardFrontUrl; }
    public void setCitizenCardFrontUrl(String citizenCardFrontUrl) { this.citizenCardFrontUrl = citizenCardFrontUrl; }

    public String getCitizenCardBackUrl() { return citizenCardBackUrl; }
    public void setCitizenCardBackUrl(String citizenCardBackUrl) { this.citizenCardBackUrl = citizenCardBackUrl; }

    public String getDriverLicenseImageUrl() { return driverLicenseImageUrl; }
    public void setDriverLicenseImageUrl(String driverLicenseImageUrl) { this.driverLicenseImageUrl = driverLicenseImageUrl; }

    public String getDriverLicenseUrl() { return driverLicenseImageUrl; }
    public void setDriverLicenseUrl(String driverLicenseUrl) { this.driverLicenseImageUrl = driverLicenseUrl; }

    public String getFacePortraitUrl() { return facePortraitUrl; }
    public void setFacePortraitUrl(String facePortraitUrl) { this.facePortraitUrl = facePortraitUrl; }

    public Boolean getIsActiveShift() { return isActiveShift; }
    public void setIsActiveShift(Boolean activeShift) { isActiveShift = activeShift; }

    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }

    public Integer getTotalTripsCompleted() { return totalTripsCompleted; }
    public void setTotalTripsCompleted(Integer totalTripsCompleted) { this.totalTripsCompleted = totalTripsCompleted; }

    public BigDecimal getTotalCo2SavedKg() { return totalCo2SavedKg; }
    public void setTotalCo2SavedKg(BigDecimal totalCo2SavedKg) { this.totalCo2SavedKg = totalCo2SavedKg; }

    public VehicleResponse getVehicle() { return vehicle; }
    public void setVehicle(VehicleResponse vehicle) { this.vehicle = vehicle; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

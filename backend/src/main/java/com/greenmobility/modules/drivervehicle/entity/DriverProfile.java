package com.greenmobility.modules.drivervehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "citizen_id", unique = true, nullable = false, length = 30)
    private String citizenId;

    @Column(name = "driver_license_number", unique = true, nullable = false, length = 30)
    private String driverLicenseNumber;

    @Column(name = "license_class", nullable = false, length = 10)
    private String licenseClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_rejection_reason", columnDefinition = "TEXT")
    private String kycRejectionReason;

    @Column(name = "face_encoding_vector", columnDefinition = "float8[]")
    private Double[] faceEncodingVector;

    @Column(name = "is_active_shift")
    private Boolean isActiveShift = false;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.valueOf(5.00);

    @Column(name = "total_trips_completed")
    private Integer totalTripsCompleted = 0;

    @Column(name = "total_co2_saved_kg", precision = 10, scale = 3)
    private BigDecimal totalCo2SavedKg = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public DriverProfile() {}

    public DriverProfile(UUID userId, String citizenId, String driverLicenseNumber, String licenseClass) {
        this.userId = userId;
        this.citizenId = citizenId;
        this.driverLicenseNumber = driverLicenseNumber;
        this.licenseClass = licenseClass;
        this.kycStatus = KycStatus.PENDING;
        this.isActiveShift = false;
        this.ratingAvg = BigDecimal.valueOf(5.00);
        this.totalTripsCompleted = 0;
        this.totalCo2SavedKg = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public String getKycRejectionReason() { return kycRejectionReason; }
    public void setKycRejectionReason(String kycRejectionReason) { this.kycRejectionReason = kycRejectionReason; }

    public Double[] getFaceEncodingVector() { return faceEncodingVector; }
    public void setFaceEncodingVector(Double[] faceEncodingVector) { this.faceEncodingVector = faceEncodingVector; }

    public Boolean getIsActiveShift() { return isActiveShift; }
    public void setIsActiveShift(Boolean activeShift) { isActiveShift = activeShift; }

    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }

    public Integer getTotalTripsCompleted() { return totalTripsCompleted; }
    public void setTotalTripsCompleted(Integer totalTripsCompleted) { this.totalTripsCompleted = totalTripsCompleted; }

    public BigDecimal getTotalCo2SavedKg() { return totalCo2SavedKg; }
    public void setTotalCo2SavedKg(BigDecimal totalCo2SavedKg) { this.totalCo2SavedKg = totalCo2SavedKg; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

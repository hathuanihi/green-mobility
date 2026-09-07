package com.greenmobility.modules.drivervehicle.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "face_verification_logs")
public class FaceVerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "selfie_image_url", nullable = false, length = 500)
    private String selfieImageUrl;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;

    @Column(name = "verified_at", updatable = false)
    private Instant verifiedAt = Instant.now();

    public FaceVerificationLog() {}

    public FaceVerificationLog(UUID driverId, String selfieImageUrl, BigDecimal similarityScore, Boolean isPassed) {
        this.driverId = driverId;
        this.selfieImageUrl = selfieImageUrl;
        this.similarityScore = similarityScore;
        this.isPassed = isPassed;
        this.verifiedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getSelfieImageUrl() { return selfieImageUrl; }
    public void setSelfieImageUrl(String selfieImageUrl) { this.selfieImageUrl = selfieImageUrl; }

    public BigDecimal getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(BigDecimal similarityScore) { this.similarityScore = similarityScore; }

    public Boolean getIsPassed() { return isPassed; }
    public void setIsPassed(Boolean passed) { isPassed = passed; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}

package com.greenmobility.modules.incentive.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reward_redemptions")
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reward_id", nullable = false)
    private UUID rewardId;

    @Column(name = "redemption_code", unique = true, nullable = false, length = 32)
    private String redemptionCode;

    @Column(name = "points_spent", nullable = false)
    private Integer pointsSpent = 0;

    @Column(name = "carbon_spent", nullable = false, precision = 18, scale = 4)
    private BigDecimal carbonSpent = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE";

    @Column(name = "pcc_ledger_tx_id")
    private UUID pccLedgerTxId;

    @Column(name = "points_ledger_tx_id")
    private UUID pointsLedgerTxId;

    @Column(name = "reversal_pcc_tx_id")
    private UUID reversalPccTxId;

    @Column(name = "reversal_points_tx_id")
    private UUID reversalPointsTxId;

    @Column(name = "applied_trip_id")
    private UUID appliedTripId;

    @Column(name = "discount_applied_vnd")
    private Long discountAppliedVnd;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certificate_metadata", columnDefinition = "jsonb")
    private String certificateMetadata;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getRewardId() { return rewardId; }
    public void setRewardId(UUID rewardId) { this.rewardId = rewardId; }

    public String getRedemptionCode() { return redemptionCode; }
    public void setRedemptionCode(String redemptionCode) { this.redemptionCode = redemptionCode; }

    public Integer getPointsSpent() { return pointsSpent; }
    public void setPointsSpent(Integer pointsSpent) { this.pointsSpent = pointsSpent; }

    public BigDecimal getCarbonSpent() { return carbonSpent; }
    public void setCarbonSpent(BigDecimal carbonSpent) { this.carbonSpent = carbonSpent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getPccLedgerTxId() { return pccLedgerTxId; }
    public void setPccLedgerTxId(UUID pccLedgerTxId) { this.pccLedgerTxId = pccLedgerTxId; }

    public UUID getPointsLedgerTxId() { return pointsLedgerTxId; }
    public void setPointsLedgerTxId(UUID pointsLedgerTxId) { this.pointsLedgerTxId = pointsLedgerTxId; }

    public UUID getReversalPccTxId() { return reversalPccTxId; }
    public void setReversalPccTxId(UUID reversalPccTxId) { this.reversalPccTxId = reversalPccTxId; }

    public UUID getReversalPointsTxId() { return reversalPointsTxId; }
    public void setReversalPointsTxId(UUID reversalPointsTxId) { this.reversalPointsTxId = reversalPointsTxId; }

    public UUID getAppliedTripId() { return appliedTripId; }
    public void setAppliedTripId(UUID appliedTripId) { this.appliedTripId = appliedTripId; }

    public Long getDiscountAppliedVnd() { return discountAppliedVnd; }
    public void setDiscountAppliedVnd(Long discountAppliedVnd) { this.discountAppliedVnd = discountAppliedVnd; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getCertificateMetadata() { return certificateMetadata; }
    public void setCertificateMetadata(String certificateMetadata) { this.certificateMetadata = certificateMetadata; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

package com.greenmobility.modules.incentive.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_incentive_awards")
public class TripIncentiveAward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id", unique = true, nullable = false)
    private UUID tripId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "driver_user_id")
    private UUID driverUserId;

    @Column(name = "co2_saved_grams", nullable = false, precision = 10, scale = 2)
    private BigDecimal co2SavedGrams = BigDecimal.ZERO;

    @Column(name = "total_pcc", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalPcc = BigDecimal.ZERO;

    @Column(name = "customer_pcc", nullable = false, precision = 18, scale = 4)
    private BigDecimal customerPcc = BigDecimal.ZERO;

    @Column(name = "driver_pcc", nullable = false, precision = 18, scale = 4)
    private BigDecimal driverPcc = BigDecimal.ZERO;

    @Column(name = "customer_points", nullable = false)
    private Integer customerPoints = 0;

    @Column(name = "driver_points", nullable = false)
    private Integer driverPoints = 0;

    @Column(name = "carbon_ready", nullable = false)
    private boolean carbonReady = false;

    @Column(name = "payment_ready", nullable = false)
    private boolean paymentReady = false;

    @Column(nullable = false, length = 35)
    private String status = "PENDING_CONDITIONS";

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name = "pcc_ledger_tx_id")
    private UUID pccLedgerTxId;

    @Column(name = "points_ledger_tx_id")
    private UUID pointsLedgerTxId;

    @Column(name = "clawback_pcc_tx_id")
    private UUID clawbackPccTxId;

    @Column(name = "clawback_points_tx_id")
    private UUID clawbackPointsTxId;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

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

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public BigDecimal getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(BigDecimal co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public BigDecimal getTotalPcc() { return totalPcc; }
    public void setTotalPcc(BigDecimal totalPcc) { this.totalPcc = totalPcc; }

    public BigDecimal getCustomerPcc() { return customerPcc; }
    public void setCustomerPcc(BigDecimal customerPcc) { this.customerPcc = customerPcc; }

    public BigDecimal getDriverPcc() { return driverPcc; }
    public void setDriverPcc(BigDecimal driverPcc) { this.driverPcc = driverPcc; }

    public Integer getCustomerPoints() { return customerPoints; }
    public void setCustomerPoints(Integer customerPoints) { this.customerPoints = customerPoints; }

    public Integer getDriverPoints() { return driverPoints; }
    public void setDriverPoints(Integer driverPoints) { this.driverPoints = driverPoints; }

    public boolean isCarbonReady() { return carbonReady; }
    public void setCarbonReady(boolean carbonReady) { this.carbonReady = carbonReady; }

    public boolean isPaymentReady() { return paymentReady; }
    public void setPaymentReady(boolean paymentReady) { this.paymentReady = paymentReady; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHoldReason() { return holdReason; }
    public void setHoldReason(String holdReason) { this.holdReason = holdReason; }

    public UUID getPccLedgerTxId() { return pccLedgerTxId; }
    public void setPccLedgerTxId(UUID pccLedgerTxId) { this.pccLedgerTxId = pccLedgerTxId; }

    public UUID getPointsLedgerTxId() { return pointsLedgerTxId; }
    public void setPointsLedgerTxId(UUID pointsLedgerTxId) { this.pointsLedgerTxId = pointsLedgerTxId; }

    public UUID getClawbackPccTxId() { return clawbackPccTxId; }
    public void setClawbackPccTxId(UUID clawbackPccTxId) { this.clawbackPccTxId = clawbackPccTxId; }

    public UUID getClawbackPointsTxId() { return clawbackPointsTxId; }
    public void setClawbackPointsTxId(UUID clawbackPointsTxId) { this.clawbackPointsTxId = clawbackPointsTxId; }

    public Instant getCreditedAt() { return creditedAt; }
    public void setCreditedAt(Instant creditedAt) { this.creditedAt = creditedAt; }

    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

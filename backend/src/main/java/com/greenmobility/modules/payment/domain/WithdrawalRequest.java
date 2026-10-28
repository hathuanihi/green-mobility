package com.greenmobility.modules.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_user_id", nullable = false)
    private UUID driverUserId;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "bank_code", nullable = false, length = 30)
    private String bankCode;

    @Column(name = "bank_account_no_encrypted", nullable = false, columnDefinition = "TEXT")
    private String bankAccountNoEncrypted;

    @Column(name = "bank_account_last4", nullable = false, length = 4)
    private String bankAccountLast4;

    @Column(name = "account_holder_name", nullable = false, length = 120)
    private String accountHolderName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED_PAID, REJECTED, CANCELLED

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "hold_ledger_tx_id")
    private UUID holdLedgerTxId;

    @Column(name = "final_ledger_tx_id")
    private UUID finalLedgerTxId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public WithdrawalRequest() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverUserId() { return driverUserId; }
    public void setDriverUserId(UUID driverUserId) { this.driverUserId = driverUserId; }

    public Long getAmountVnd() { return amountVnd; }
    public void setAmountVnd(Long amountVnd) { this.amountVnd = amountVnd; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBankAccountNoEncrypted() { return bankAccountNoEncrypted; }
    public void setBankAccountNoEncrypted(String bankAccountNoEncrypted) { this.bankAccountNoEncrypted = bankAccountNoEncrypted; }

    public String getBankAccountLast4() { return bankAccountLast4; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public UUID getHoldLedgerTxId() { return holdLedgerTxId; }
    public void setHoldLedgerTxId(UUID holdLedgerTxId) { this.holdLedgerTxId = holdLedgerTxId; }

    public UUID getFinalLedgerTxId() { return finalLedgerTxId; }
    public void setFinalLedgerTxId(UUID finalLedgerTxId) { this.finalLedgerTxId = finalLedgerTxId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}


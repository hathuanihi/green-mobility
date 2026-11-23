package com.greenmobility.modules.incentive.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carbon_conversions")
public class CarbonConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @Column(name = "target_account_type", nullable = false, length = 30)
    private String targetAccountType;

    @Column(name = "pcc_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal pccAmount;

    @Column(name = "rate_vnd_per_pcc", nullable = false)
    private Long rateVndPerPcc;

    @Column(name = "vnd_amount", nullable = false)
    private Long vndAmount;

    @Column(name = "pcc_ledger_tx_id", nullable = false)
    private UUID pccLedgerTxId;

    @Column(name = "vnd_ledger_tx_id", nullable = false)
    private UUID vndLedgerTxId;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getTargetAccountType() { return targetAccountType; }
    public void setTargetAccountType(String targetAccountType) { this.targetAccountType = targetAccountType; }

    public BigDecimal getPccAmount() { return pccAmount; }
    public void setPccAmount(BigDecimal pccAmount) { this.pccAmount = pccAmount; }

    public Long getRateVndPerPcc() { return rateVndPerPcc; }
    public void setRateVndPerPcc(Long rateVndPerPcc) { this.rateVndPerPcc = rateVndPerPcc; }

    public Long getVndAmount() { return vndAmount; }
    public void setVndAmount(Long vndAmount) { this.vndAmount = vndAmount; }

    public UUID getPccLedgerTxId() { return pccLedgerTxId; }
    public void setPccLedgerTxId(UUID pccLedgerTxId) { this.pccLedgerTxId = pccLedgerTxId; }

    public UUID getVndLedgerTxId() { return vndLedgerTxId; }
    public void setVndLedgerTxId(UUID vndLedgerTxId) { this.vndLedgerTxId = vndLedgerTxId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

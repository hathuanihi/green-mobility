package com.greenmobility.modules.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topup_requests")
public class TopupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "gateway", nullable = false, length = 20)
    private String gateway;

    @Column(name = "gateway_txn_ref", nullable = false, unique = true, length = 64)
    private String gatewayTxnRef;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ledger_transaction_id")
    private UUID ledgerTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public TopupRequest() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getAmountVnd() { return amountVnd; }
    public void setAmountVnd(Long amountVnd) { this.amountVnd = amountVnd; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getGatewayTxnRef() { return gatewayTxnRef; }
    public void setGatewayTxnRef(String gatewayTxnRef) { this.gatewayTxnRef = gatewayTxnRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public void setLedgerTransactionId(UUID ledgerTransactionId) { this.ledgerTransactionId = ledgerTransactionId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}


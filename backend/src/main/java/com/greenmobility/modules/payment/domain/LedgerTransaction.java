package com.greenmobility.modules.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_code", unique = true, nullable = false, length = 40)
    private String transactionCode;

    @Column(name = "trip_id")
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private LedgerTxType transactionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reversal_of")
    private UUID reversalOf;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LedgerTransaction() {}

    public LedgerTransaction(String transactionCode, UUID tripId, LedgerTxType transactionType,
                             String description, String idempotencyKey, String referenceType,
                             UUID referenceId, UUID reversalOf) {
        this.transactionCode = transactionCode;
        this.tripId = tripId;
        this.transactionType = transactionType;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reversalOf = reversalOf;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public LedgerTxType getTransactionType() { return transactionType; }
    public void setTransactionType(LedgerTxType transactionType) { this.transactionType = transactionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public UUID getReversalOf() { return reversalOf; }
    public void setReversalOf(UUID reversalOf) { this.reversalOf = reversalOf; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

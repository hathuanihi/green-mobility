package com.greenmobility.modules.payment.ledger;

import com.greenmobility.modules.payment.domain.LedgerTxType;

import java.util.List;
import java.util.UUID;

public record PostingRequest(
        String transactionCode,
        UUID tripId,
        LedgerTxType transactionType,
        String description,
        String idempotencyKey,
        String referenceType,
        UUID referenceId,
        UUID reversalOf,
        List<PostingEntry> entries
) {
    public PostingRequest {
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type must not be null");
        }
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Ledger transaction must have at least 2 entries");
        }
    }
}


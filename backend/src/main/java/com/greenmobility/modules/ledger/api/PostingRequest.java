package com.greenmobility.modules.ledger.api;

import com.greenmobility.modules.ledger.domain.LedgerTxType;

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
        List<PostingEntry> entries,
        boolean bypassFrozenCheck
) {
    public PostingRequest {
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction type must not be null");
        }
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Ledger transaction must have at least 2 entries");
        }
    }

    public PostingRequest(
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
        this(transactionCode, tripId, transactionType, description, idempotencyKey, referenceType, referenceId, reversalOf, entries, false);
    }
}

package com.greenmobility.modules.payment.ledger;

import com.greenmobility.modules.payment.domain.EntryType;

import java.math.BigDecimal;
import java.util.UUID;

public record PostingEntry(
        UUID accountId,
        EntryType entryType,
        BigDecimal amount
) {
    public PostingEntry {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID must not be null");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("Entry type must not be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be strictly positive");
        }
    }
}


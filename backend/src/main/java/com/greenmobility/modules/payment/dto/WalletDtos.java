package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.UUID;

public class WalletDtos {

    public record WalletSummaryResponse(
            UUID walletAccountId,
            long balanceVnd,
            Long earningsBalanceVnd,
            Long holdVnd,
            long cashDebtVnd,
            boolean cashTripsAllowed
    ) {}

    public record WalletTransactionResponse(
            UUID id,
            String direction, // IN, OUT
            long amountVnd,
            long balanceAfterVnd,
            String type,
            String description,
            String referenceType,
            UUID referenceId,
            Instant createdAt
    ) {}

    public record CursorPaginationResponse<T>(
            java.util.List<T> items,
            String nextCursor,
            boolean hasMore
    ) {}
}


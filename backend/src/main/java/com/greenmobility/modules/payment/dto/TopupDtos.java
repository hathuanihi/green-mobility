package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.UUID;

public class TopupDtos {

    public record CreateTopupRequest(
            long amountVnd,
            String gateway
    ) {}

    public record TopupResponse(
            UUID topupId,
            long amountVnd,
            String gateway,
            String status,
            String payUrl,
            Instant expiresAt,
            Instant createdAt
    ) {}
}


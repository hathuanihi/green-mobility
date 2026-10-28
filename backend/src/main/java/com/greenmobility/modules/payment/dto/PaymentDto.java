package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentDto(
        UUID paymentId,
        UUID tripId,
        String method,
        String status,
        long amountVnd,
        long commissionVnd,
        long driverAmountVnd,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        Instant expiresAt,
        Instant paidAt,
        Instant createdAt
) {}


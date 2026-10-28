package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckoutResponse(
        UUID paymentId,
        String status,
        String method,
        long amountVnd,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        Instant expiresAt
) {}


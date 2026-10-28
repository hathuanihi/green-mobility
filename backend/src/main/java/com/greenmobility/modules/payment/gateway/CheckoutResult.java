package com.greenmobility.modules.payment.gateway;

import java.time.Instant;

public record CheckoutResult(
        String gatewayTxnRef,
        String payUrl,
        String deeplink,
        String qrCodeUrl,
        Instant expiresAt
) {}


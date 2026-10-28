package com.greenmobility.modules.payment.gateway;

import java.time.Instant;

public record CheckoutRequest(
        String gatewayTxnRef,
        long amountVnd,
        String orderInfo,
        String clientIp,
        String returnUrl,
        Instant expiresAt
) {}


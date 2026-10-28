package com.greenmobility.modules.payment.gateway;

public record GatewayQueryResult(
        boolean isFound,
        boolean isSuccess,
        long amountVnd,
        String gatewayTransactionNo,
        String rawStatus
) {}


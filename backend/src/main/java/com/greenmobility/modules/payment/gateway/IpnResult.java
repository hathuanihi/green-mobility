package com.greenmobility.modules.payment.gateway;

public record IpnResult(
        boolean isValidSignature,
        boolean isSuccess,
        String gatewayTxnRef,
        long amountVnd,
        String gatewayTransactionNo,
        String responseCode,
        String message
) {}


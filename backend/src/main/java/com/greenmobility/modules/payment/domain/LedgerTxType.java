package com.greenmobility.modules.payment.domain;

public enum LedgerTxType {
    TOPUP,
    TRIP_PAYMENT_WALLET,
    TRIP_PAYMENT_GATEWAY,
    TRIP_PAYMENT_CASH,
    REFUND,
    WITHDRAWAL_HOLD,
    WITHDRAWAL_PAID,
    WITHDRAWAL_REVERSAL
}


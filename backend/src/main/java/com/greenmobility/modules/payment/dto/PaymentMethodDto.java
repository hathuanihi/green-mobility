package com.greenmobility.modules.payment.dto;

public record PaymentMethodDto(
        String code,
        String name,
        boolean enabled,
        long walletBalanceVnd,
        String icon
) {}


package com.greenmobility.modules.ledger.domain;

import java.util.Arrays;

public enum LedgerCurrency {
    VND(0),
    PCC(4),
    ECO_POINT(0);

    private final int scale;

    LedgerCurrency(int scale) {
        this.scale = scale;
    }

    public int scale() {
        return scale;
    }

    public static LedgerCurrency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code must not be blank");
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported ledger currency: " + code));
    }
}

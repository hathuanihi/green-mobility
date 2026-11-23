package com.greenmobility.modules.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LedgerAuditReportDto(
        boolean isBalanced,
        Map<String, BigDecimal> totalsByCurrency,
        int unbalancedTransactionsCount,
        int crossCurrencyTransactionsCount,
        int accountDiscrepanciesCount,
        List<String> discrepancies,
        boolean redemptionsFrozen,
        Instant checkedAt
) {}

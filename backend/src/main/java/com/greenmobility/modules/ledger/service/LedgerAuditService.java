package com.greenmobility.modules.ledger.service;

import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.ledger.api.LedgerAuditReportDto;
import com.greenmobility.modules.ledger.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class LedgerAuditService {

    private static final Logger log = LoggerFactory.getLogger(LedgerAuditService.class);

    private final LedgerEntryRepository entryRepository;
    private final PlatformSettingsService settingsService;
    private final AuditLogService auditLogService;

    public LedgerAuditService(LedgerEntryRepository entryRepository,
                              PlatformSettingsService settingsService,
                              AuditLogService auditLogService) {
        this.entryRepository = entryRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LedgerAuditReportDto runFullAudit(boolean autoCircuitBreak) {
        List<String> discrepancies = new ArrayList<>();

        // 1. Unbalanced transactions
        List<String> unbalancedTx = entryRepository.findUnbalancedTransactionCodes();
        for (String txCode : unbalancedTx) {
            discrepancies.add("Giao dịch mất cân bằng Debit != Credit: " + txCode);
        }

        // 2. Cross-currency transactions
        List<String> crossCcyTx = entryRepository.findCrossCurrencyTransactionCodes();
        for (String txCode : crossCcyTx) {
            discrepancies.add("Giao dịch trộn lẫn nhiều loại tiền tệ: " + txCode);
        }

        // 3. Account balance vs sum(entries) drifts
        List<Object[]> drifts = entryRepository.findAccountBalanceDrifts();
        for (Object[] row : drifts) {
            discrepancies.add(String.format("Tài khoản %s sai lệch số dư: balance=%s, sum(entries)=%s",
                    row[0], row[1], row[2]));
        }

        // 4. Per-currency zero-sum conservation
        Map<String, BigDecimal> totalsByCurrency = new LinkedHashMap<>();
        totalsByCurrency.put("VND", BigDecimal.ZERO.setScale(4));
        totalsByCurrency.put("PCC", BigDecimal.ZERO.setScale(4));
        totalsByCurrency.put("ECO_POINT", BigDecimal.ZERO.setScale(4));

        for (Object[] row : entryRepository.sumBalancesByCurrency()) {
            String ccy = String.valueOf(row[0]);
            BigDecimal sum = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(row[1]));
            totalsByCurrency.put(ccy, sum);
            if (sum.compareTo(BigDecimal.ZERO) != 0) {
                discrepancies.add(String.format("Tổng hệ thống tiền tệ %s khác 0: Δ = %s", ccy, sum));
            }
        }

        boolean isBalanced = unbalancedTx.isEmpty()
                && crossCcyTx.isEmpty()
                && drifts.isEmpty()
                && totalsByCurrency.values().stream().allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);

        if (!isBalanced && autoCircuitBreak && !settingsService.isRedemptionsFrozen()) {
            log.error("LEDGER INTEGRITY VIOLATION DETECTED! Triggering circuit-breaker redemptions_frozen=true. Discrepancies: {}",
                    discrepancies);
            settingsService.updateSetting("redemptions_frozen", "true", null);
            auditLogService.record(null, "SYSTEM_AUDIT", "AUTO_CIRCUIT_BREAKER_TRIPPED",
                    "PlatformSetting", "redemptions_frozen",
                    Map.of("redemptions_frozen", "false"),
                    Map.of("redemptions_frozen", "true", "discrepancies", discrepancies),
                    "127.0.0.1");
        }

        return new LedgerAuditReportDto(
                isBalanced,
                totalsByCurrency,
                unbalancedTx.size(),
                crossCcyTx.size(),
                drifts.size(),
                discrepancies,
                settingsService.isRedemptionsFrozen(),
                Instant.now()
        );
    }
}

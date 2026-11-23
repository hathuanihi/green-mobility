package com.greenmobility.modules.ledger.job;

import com.greenmobility.modules.ledger.api.LedgerAuditReportDto;
import com.greenmobility.modules.ledger.service.LedgerAuditService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LedgerIntegrityJob {

    private static final Logger log = LoggerFactory.getLogger(LedgerIntegrityJob.class);

    private final LedgerAuditService ledgerAuditService;

    public LedgerIntegrityJob(LedgerAuditService ledgerAuditService) {
        this.ledgerAuditService = ledgerAuditService;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "LedgerIntegrityJob", lockAtLeastFor = "10m", lockAtMostFor = "1h")
    public void verifyLedgerIntegrity() {
        log.info("Starting nightly multi-currency ledger integrity verification...");
        LedgerAuditReportDto report = ledgerAuditService.runFullAudit(true);

        if (!report.isBalanced()) {
            log.error("LEDGER INTEGRITY VIOLATION! Balanced={}, Totals={}, UnbalancedTx={}, CrossCcyTx={}, Drifts={}",
                    report.isBalanced(), report.totalsByCurrency(),
                    report.unbalancedTransactionsCount(), report.crossCurrencyTransactionsCount(),
                    report.accountDiscrepanciesCount());
        } else {
            log.info("Multi-currency ledger integrity verified cleanly: totals={}", report.totalsByCurrency());
        }
    }
}

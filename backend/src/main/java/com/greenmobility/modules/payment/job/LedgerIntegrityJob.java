package com.greenmobility.modules.payment.job;

import com.greenmobility.modules.payment.dto.AdminFinanceDtos;
import com.greenmobility.modules.payment.service.FinanceReportService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LedgerIntegrityJob {

    private static final Logger log = LoggerFactory.getLogger(LedgerIntegrityJob.class);

    private final FinanceReportService financeReportService;

    public LedgerIntegrityJob(FinanceReportService financeReportService) {
        this.financeReportService = financeReportService;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh") // 02:00 AM nightly GMT+7
    @SchedulerLock(name = "LedgerIntegrityJob", lockAtLeastFor = "10m", lockAtMostFor = "1h")
    public void verifyLedgerIntegrity() {
        log.info("Starting nightly ledger integrity verification...");
        AdminFinanceDtos.ReconciliationResponse report = financeReportService.getReconciliation();

        if (!report.isBalanced()) {
            log.error("LEDGER INTEGRITY VIOLATION DETECTED! Balanced={}, SystemTotal={}, UnbalancedTx={}, AccountDiscrepancies={}",
                    report.isBalanced(), report.systemTotalVnd(), report.unbalancedTransactionsCount(), report.accountDiscrepanciesCount());
            for (String disc : report.discrepancies()) {
                log.error("  Discrepancy: {}", disc);
            }
        } else {
            log.info("Ledger integrity verified successfully. System balanced at 0 VND with 0 anomalies.");
        }
    }
}


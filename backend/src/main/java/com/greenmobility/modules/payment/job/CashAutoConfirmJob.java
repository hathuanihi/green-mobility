package com.greenmobility.modules.payment.job;

import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.payment.service.PlatformSettingsService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class CashAutoConfirmJob {

    private static final Logger log = LoggerFactory.getLogger(CashAutoConfirmJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PlatformSettingsService settingsService;

    public CashAutoConfirmJob(PaymentRepository paymentRepository,
                              PaymentService paymentService,
                              PlatformSettingsService settingsService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000) // 1 minute
    @SchedulerLock(name = "CashAutoConfirmJob", lockAtLeastFor = "30s", lockAtMostFor = "3m")
    @Transactional
    public void autoConfirmOverdueCashPayments() {
        int minutes = settingsService.getCashAutoConfirmMinutes();
        Instant cutoff = Instant.now().minusSeconds(minutes * 60L);

        List<Payment> overdue = paymentRepository.findPendingCashPaymentsOlderThan(cutoff);
        for (Payment payment : overdue) {
            log.info("Auto-confirming overdue CASH payment {} (older than {}m)", payment.getId(), minutes);
            try {
                paymentService.settle(payment.getId(), "AUTO_CONFIRMED", null);
            } catch (Exception e) {
                log.error("Failed to auto-confirm cash payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}


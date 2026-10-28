package com.greenmobility.modules.payment.job;

import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.gateway.GatewayQueryResult;
import com.greenmobility.modules.payment.gateway.PaymentGateway;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class PaymentReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final TripModuleApi tripModuleApi;

    public PaymentReconciliationJob(PaymentRepository paymentRepository,
                                    PaymentService paymentService,
                                    TripModuleApi tripModuleApi) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.tripModuleApi = tripModuleApi;
    }

    @Scheduled(fixedDelay = 120_000, initialDelay = 30_000) // 2 minutes
    @SchedulerLock(name = "PaymentReconciliationJob", lockAtLeastFor = "1m", lockAtMostFor = "5m")
    @Transactional
    public void reconcilePendingPayments() {
        Instant cutoff = Instant.now().minusSeconds(180); // 3 minutes ago
        List<Payment> pending = paymentRepository.findPendingOnlinePaymentsOlderThan(cutoff);

        for (Payment payment : pending) {
            try {
                if (payment.getGatewayTxnRef() == null) continue;

                PaymentGateway gateway = paymentService.selectGateway(payment.getGateway());
                GatewayQueryResult queryResult = gateway.queryTransaction(payment.getGatewayTxnRef());

                if (queryResult.isSuccess()) {
                    log.info("Reconciliation found successful transaction at gateway for payment {}", payment.getId());
                    paymentService.settle(payment.getId(), "RECONCILIATION_JOB", queryResult.gatewayTransactionNo());
                } else if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(Instant.now())) {
                    log.info("Payment {} has expired, marking EXPIRED", payment.getId());
                    payment.setStatus("EXPIRED");
                    paymentRepository.save(payment);
                    tripModuleApi.markPaymentStatus(payment.getTripId(), PaymentStatus.FAILED);
                }
            } catch (Exception e) {
                log.warn("Error reconciling payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}


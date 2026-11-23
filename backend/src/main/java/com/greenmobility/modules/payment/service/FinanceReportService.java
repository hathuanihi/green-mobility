package com.greenmobility.modules.payment.service;

import com.greenmobility.modules.drivervehicle.entity.KycStatus;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.ledger.api.LedgerAuditReportDto;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.service.LedgerAuditService;
import com.greenmobility.modules.ledger.service.LedgerService;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.dto.AdminFinanceDtos;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FinanceReportService {

    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PaymentRepository paymentRepository;
    private final LedgerAccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final LedgerAuditService ledgerAuditService;
    private final DriverProfileRepository driverProfileRepository;

    @Autowired
    public FinanceReportService(PaymentRepository paymentRepository,
                                LedgerAccountRepository accountRepository,
                                LedgerService ledgerService,
                                LedgerAuditService ledgerAuditService,
                                @Autowired(required = false) DriverProfileRepository driverProfileRepository) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.ledgerAuditService = ledgerAuditService;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional(readOnly = true)
    public AdminFinanceDtos.FinanceOverviewResponse getOverview() {
        Long totalGmv = paymentRepository.calculateTotalGmv();
        Long totalComm = paymentRepository.calculateTotalCommission();

        Map<String, Long> byMethod = new LinkedHashMap<>();
        Map<String, Long> methodAmounts = new LinkedHashMap<>();
        for (Object[] row : paymentRepository.aggregateByMethod()) {
            String method = String.valueOf(row[0]);
            long cnt = ((Number) row[1]).longValue();
            long amt = ((Number) row[2]).longValue();
            byMethod.put(method, cnt);
            methodAmounts.put(method, amt);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long totalCount = 0;
        long paidCount = 0;
        for (Object[] row : paymentRepository.aggregateCountByStatus()) {
            String status = String.valueOf(row[0]);
            long cnt = ((Number) row[1]).longValue();
            byStatus.put(status, cnt);
            totalCount += cnt;
            if ("PAID".equals(status) || "PARTIALLY_REFUNDED".equals(status)) {
                paidCount += cnt;
            }
        }
        double successRate = totalCount > 0 ? ((double) paidCount / totalCount) * 100.0 : 0.0;

        long gatewayClearingBal = 0;
        long payoutClearingBal = 0;
        try {
            gatewayClearingBal = ledgerService.getSystemAccount(AccountType.GATEWAY_CLEARING).getBalance().longValue();
        } catch (Exception ignored) {}
        try {
            payoutClearingBal = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING).getBalance().longValue();
        } catch (Exception ignored) {}

        BigDecimal walletSum = accountRepository.sumBalanceByAccountTypeAndCurrency(AccountType.USER_GREEN_WALLET, "VND");
        long totalWalletBalanceVnd = walletSum != null ? walletSum.longValue() : 0L;

        long activeDriversCount = driverProfileRepository != null
                ? driverProfileRepository.countByKycStatus(KycStatus.APPROVED)
                : 0L;

        // Real 7-day revenue series (GMT+7)
        LocalDate today = LocalDate.now(GMT7_ZONE);
        LocalDate startDate = today.minusDays(6);
        Instant startInstant = startDate.atStartOfDay(GMT7_ZONE).toInstant();

        Map<String, AdminFinanceDtos.DailyRevenuePoint> dayMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            String key = startDate.plusDays(i).format(DAY_FMT);
            dayMap.put(key, new AdminFinanceDtos.DailyRevenuePoint(key, 0L, 0L, 0L));
        }

        for (Object[] row : paymentRepository.aggregateDailyRevenueSince(startInstant)) {
            String dayKey = String.valueOf(row[0]);
            long gmv = ((Number) row[1]).longValue();
            long comm = ((Number) row[2]).longValue();
            long drv = ((Number) row[3]).longValue();
            dayMap.put(dayKey, new AdminFinanceDtos.DailyRevenuePoint(dayKey, gmv, comm, drv));
        }

        return new AdminFinanceDtos.FinanceOverviewResponse(
                totalGmv != null ? totalGmv : 0L,
                totalComm != null ? totalComm : 0L,
                Math.round(successRate * 10.0) / 10.0,
                gatewayClearingBal,
                payoutClearingBal,
                totalWalletBalanceVnd,
                activeDriversCount,
                byMethod,
                methodAmounts,
                byStatus,
                new ArrayList<>(dayMap.values())
        );
    }

    @Transactional
    public AdminFinanceDtos.ReconciliationResponse getReconciliation() {
        LedgerAuditReportDto audit = ledgerAuditService.runFullAudit(false);
        List<String> discrepancies = new ArrayList<>(audit.discrepancies());

        Instant threeMinutesAgo = Instant.now().minusSeconds(180);
        List<Payment> latePayments = paymentRepository.findPendingOnlinePaymentsOlderThan(threeMinutesAgo);
        for (Payment p : latePayments) {
            discrepancies.add("Thanh toán online đang chờ quá 3 phút: ID=" + p.getId() + ", Ref=" + p.getGatewayTxnRef());
        }

        long systemTotalVnd = audit.totalsByCurrency()
                .getOrDefault("VND", BigDecimal.ZERO)
                .longValue();

        return new AdminFinanceDtos.ReconciliationResponse(
                audit.isBalanced(),
                systemTotalVnd,
                audit.unbalancedTransactionsCount(),
                audit.accountDiscrepanciesCount(),
                latePayments.size(),
                discrepancies,
                audit.checkedAt()
        );
    }
}

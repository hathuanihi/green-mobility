package com.greenmobility.modules.payment.service;

import com.greenmobility.modules.payment.domain.AccountType;
import com.greenmobility.modules.payment.domain.LedgerAccount;
import com.greenmobility.modules.payment.domain.LedgerEntry;
import com.greenmobility.modules.payment.domain.LedgerTransaction;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.dto.AdminFinanceDtos;
import com.greenmobility.modules.payment.ledger.LedgerAccountRepository;
import com.greenmobility.modules.payment.ledger.LedgerEntryRepository;
import com.greenmobility.modules.payment.ledger.LedgerService;
import com.greenmobility.modules.payment.ledger.LedgerTransactionRepository;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceReportService {

    private final PaymentRepository paymentRepository;
    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerService ledgerService;

    public FinanceReportService(PaymentRepository paymentRepository,
                                LedgerAccountRepository accountRepository,
                                LedgerTransactionRepository transactionRepository,
                                LedgerEntryRepository entryRepository,
                                LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional(readOnly = true)
    public AdminFinanceDtos.FinanceOverviewResponse getOverview() {
        Long totalGmv = paymentRepository.calculateTotalGmv();
        Long totalComm = paymentRepository.calculateTotalCommission();

        List<Payment> allPayments = paymentRepository.findAll();
        long totalCount = allPayments.size();
        long paidCount = allPayments.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        double successRate = totalCount > 0 ? ((double) paidCount / totalCount) * 100.0 : 0.0;

        Map<String, Long> byMethod = allPayments.stream()
                .collect(Collectors.groupingBy(Payment::getMethod, Collectors.counting()));

        Map<String, Long> byStatus = allPayments.stream()
                .collect(Collectors.groupingBy(Payment::getStatus, Collectors.counting()));

        long gatewayClearingBal = 0;
        long payoutClearingBal = 0;

        try {
            gatewayClearingBal = ledgerService.getSystemAccount(AccountType.GATEWAY_CLEARING).getBalance().longValue();
        } catch (Exception ignored) {}

        try {
            payoutClearingBal = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING).getBalance().longValue();
        } catch (Exception ignored) {}

        return new AdminFinanceDtos.FinanceOverviewResponse(
                totalGmv != null ? totalGmv : 0L,
                totalComm != null ? totalComm : 0L,
                Math.round(successRate * 10.0) / 10.0,
                gatewayClearingBal,
                payoutClearingBal,
                byMethod,
                byStatus
        );
    }

    @Transactional(readOnly = true)
    public AdminFinanceDtos.ReconciliationResponse getReconciliation() {
        List<String> discrepancies = new ArrayList<>();
        int unbalancedTxCount = 0;
        int accountDiscrepancies = 0;
        int latePendingCount = 0;

        // 1. Check all ledger transactions balance (Sum Debit == Sum Credit)
        List<LedgerTransaction> allTx = transactionRepository.findAll();
        for (LedgerTransaction tx : allTx) {
            List<LedgerEntry> entries = entryRepository.findByTransactionId(tx.getId());
            BigDecimal d = BigDecimal.ZERO;
            BigDecimal c = BigDecimal.ZERO;
            for (LedgerEntry e : entries) {
                if ("DEBIT".equalsIgnoreCase(e.getEntryType().name())) {
                    d = d.add(e.getAmount());
                } else {
                    c = c.add(e.getAmount());
                }
            }
            if (d.compareTo(c) != 0 || (d.compareTo(BigDecimal.ZERO) == 0 && !entries.isEmpty())) {
                unbalancedTxCount++;
                discrepancies.add("Giao dịch " + tx.getTransactionCode() + " không cân bằng: Nợ=" + d + ", Có=" + c);
            }
        }

        // 2. Check each account: balance == Sum(entries)
        List<LedgerAccount> accounts = accountRepository.findAll();
        BigDecimal totalSystemBalance = BigDecimal.ZERO;

        for (LedgerAccount acc : accounts) {
            if ("VND".equalsIgnoreCase(acc.getCurrency())) {
                totalSystemBalance = totalSystemBalance.add(acc.getBalance());
            }

            BigDecimal calculated = entryRepository.calculateBalanceForAccount(acc.getId());
            if (acc.getBalance().compareTo(calculated) != 0) {
                accountDiscrepancies++;
                discrepancies.add(String.format("Tài khoản %s sai lệch số dư: ghi nhận=%s, tính từ bút toán=%s",
                        acc.getAccountNumber(), acc.getBalance(), calculated));
            }
        }

        // 3. Late pending payments
        Instant threeMinutesAgo = Instant.now().minusSeconds(180);
        List<Payment> latePayments = paymentRepository.findPendingOnlinePaymentsOlderThan(threeMinutesAgo);
        latePendingCount = latePayments.size();
        for (Payment p : latePayments) {
            discrepancies.add("Thanh toán online đang chờ quá 3 phút: ID=" + p.getId() + ", Ref=" + p.getGatewayTxnRef());
        }

        boolean isBalanced = (unbalancedTxCount == 0) &&
                (accountDiscrepancies == 0) &&
                (totalSystemBalance.compareTo(BigDecimal.ZERO) == 0);

        return new AdminFinanceDtos.ReconciliationResponse(
                isBalanced,
                totalSystemBalance.longValue(),
                unbalancedTxCount,
                accountDiscrepancies,
                latePendingCount,
                discrepancies,
                Instant.now()
        );
    }
}


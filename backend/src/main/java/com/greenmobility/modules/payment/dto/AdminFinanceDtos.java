package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminFinanceDtos {

    public record FinanceOverviewResponse(
            long totalGmvVnd,
            long totalCommissionVnd,
            double successRate,
            long gatewayClearingVnd,
            long payoutClearingVnd,
            Map<String, Long> paymentsByMethod,
            Map<String, Long> paymentsByStatus
    ) {}

    public record ReconciliationResponse(
            boolean isBalanced,
            long systemTotalVnd,
            int unbalancedTransactionsCount,
            int accountDiscrepanciesCount,
            int latePendingPaymentsCount,
            List<String> discrepancies,
            Instant checkedAt
    ) {}

    public record RefundRequest(
            long amountVnd,
            String reason,
            boolean refundCommission
    ) {}

    public record AdminPaymentDetailResponse(
            PaymentDto payment,
            List<LedgerEntryDetail> ledgerEntries,
            List<WebhookEventDetail> webhookEvents
    ) {}

    public record LedgerEntryDetail(
            UUID entryId,
            String accountType,
            String accountNumber,
            String entryType,
            long amountVnd,
            long balanceAfterVnd,
            Instant createdAt
    ) {}

    public record WebhookEventDetail(
            UUID eventId,
            String gateway,
            String gatewayTxnRef,
            boolean signatureValid,
            String processingResult,
            String sourceIp,
            Instant receivedAt
    ) {}
}


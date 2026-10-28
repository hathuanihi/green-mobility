package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.AdminFinanceDtos;
import com.greenmobility.modules.payment.ledger.LedgerService;
import com.greenmobility.modules.payment.ledger.PostingEntry;
import com.greenmobility.modules.payment.ledger.PostingRequest;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final CommissionService commissionService;
    private final AuditLogService auditLogService;
    private final OutboxService outboxService;
    private final TripModuleApi tripModuleApi;

    public RefundService(PaymentRepository paymentRepository,
                         LedgerService ledgerService,
                         CommissionService commissionService,
                         AuditLogService auditLogService,
                         OutboxService outboxService,
                         TripModuleApi tripModuleApi) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
        this.commissionService = commissionService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.tripModuleApi = tripModuleApi;
    }

    @Transactional
    public void refund(UUID adminId, String adminEmail, UUID paymentId,
                       AdminFinanceDtos.RefundRequest request, String ipAddress) {
        if (request.reason() == null || request.reason().trim().length() < 10) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "REFUND_INVALID_REASON",
                    "Lý do hoàn tiền là bắt buộc và phải có ít nhất 10 ký tự");
        }

        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy giao dịch thanh toán"));

        if (!List.of("PAID", "PARTIALLY_REFUNDED").contains(payment.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_REFUNDABLE",
                    "Chỉ có thể hoàn tiền cho giao dịch đã thanh toán thành công");
        }

        long r = request.amountVnd();
        if (r <= 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "REFUND_INVALID_AMOUNT", "Số tiền hoàn phải lớn hơn 0");
        }

        long alreadyRefunded = payment.getRefundedVnd() != null ? payment.getRefundedVnd() : 0L;
        long remainingRefundable = payment.getAmountVnd() - alreadyRefunded;

        if (r > remainingRefundable) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REFUND_EXCEEDS_PAID",
                    String.format("Số tiền hoàn (%,dđ) vượt quá số tiền khả dụng còn lại của giao dịch (%,dđ)", r, remainingRefundable));
        }

        String method = payment.getMethod();
        UUID ledgerTxId = null;

        if (!"CASH".equalsIgnoreCase(method)) {
            // Proportional split (§2.2): C' = round(R * C / F), D' = R - C'
            CommissionService.RefundSplit split = commissionService.calculateRefundSplit(
                    r,
                    payment.getAmountVnd(),
                    payment.getCommissionVnd() != null ? payment.getCommissionVnd() : 0L
            );

            LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(payment.getCustomerId());
            LedgerAccount driverEarnings = payment.getDriverUserId() != null
                    ? ledgerService.getOrCreateDriverEarningsAccount(payment.getDriverUserId())
                    : null;
            LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);

            List<PostingEntry> entries = new ArrayList<>();
            if (driverEarnings != null && split.driverRefundVnd() > 0) {
                entries.add(new PostingEntry(driverEarnings.getId(), EntryType.DEBIT, BigDecimal.valueOf(split.driverRefundVnd())));
            }
            if (split.commissionRefundVnd() > 0) {
                entries.add(new PostingEntry(platformRev.getId(), EntryType.DEBIT, BigDecimal.valueOf(split.commissionRefundVnd())));
            }
            entries.add(new PostingEntry(userWallet.getId(), EntryType.CREDIT, BigDecimal.valueOf(r)));

            PostingRequest postReq = new PostingRequest(
                    null,
                    payment.getTripId(),
                    LedgerTxType.REFUND,
                    "Hoan tien ve Vi Xanh: " + request.reason(),
                    "REFUND-TX-" + payment.getId() + "-" + (alreadyRefunded + r),
                    "REFUND",
                    payment.getId(),
                    payment.getLedgerTransactionId(),
                    entries
            );
            LedgerTransaction tx = ledgerService.post(postReq);
            ledgerTxId = tx.getId();
        } else if (request.refundCommission()) {
            // For CASH, if refundCommission=true, reverse platform commission back to driver
            CommissionService.RefundSplit split = commissionService.calculateRefundSplit(
                    r,
                    payment.getAmountVnd(),
                    payment.getCommissionVnd() != null ? payment.getCommissionVnd() : 0L
            );
            if (split.commissionRefundVnd() > 0 && payment.getDriverUserId() != null) {
                LedgerAccount driverEarnings = ledgerService.getOrCreateDriverEarningsAccount(payment.getDriverUserId());
                LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);

                PostingRequest postReq = new PostingRequest(
                        null,
                        payment.getTripId(),
                        LedgerTxType.REFUND,
                        "Hoan lai phi hoa hong tien mat cho tai xe: " + request.reason(),
                        "REFUND-COMM-TX-" + payment.getId() + "-" + (alreadyRefunded + r),
                        "REFUND",
                        payment.getId(),
                        payment.getLedgerTransactionId(),
                        List.of(
                                new PostingEntry(platformRev.getId(), EntryType.DEBIT, BigDecimal.valueOf(split.commissionRefundVnd())),
                                new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(split.commissionRefundVnd()))
                        )
                );
                ledgerService.post(postReq);
            }
        }

        long newRefunded = alreadyRefunded + r;
        payment.setRefundedVnd(newRefunded);
        boolean isFullRefund = (newRefunded >= payment.getAmountVnd());
        String newStatus = isFullRefund ? "REFUNDED" : "PARTIALLY_REFUNDED";
        payment.setStatus(newStatus);
        paymentRepository.save(payment);

        tripModuleApi.markPaymentStatus(payment.getTripId(), isFullRefund ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);

        auditLogService.record(adminId, adminEmail, "REFUND_PAYMENT", "Payment",
                payment.getId().toString(),
                Map.of("refundedVnd", alreadyRefunded),
                Map.of("refundedVnd", newRefunded, "refundAmount", r, "reason", request.reason(), "status", newStatus),
                ipAddress);

        outboxService.recordEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_PAYMENT_REFUNDED, Map.of(
                        "paymentId", payment.getId(),
                        "tripId", payment.getTripId(),
                        "refundAmountVnd", r,
                        "totalRefundedVnd", newRefunded,
                        "status", newStatus,
                        "reason", request.reason()
                ));
    }
}


package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.WithdrawalDtos;
import com.greenmobility.modules.payment.ledger.LedgerService;
import com.greenmobility.modules.payment.ledger.PostingEntry;
import com.greenmobility.modules.payment.ledger.PostingRequest;
import com.greenmobility.modules.payment.repository.WithdrawalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WithdrawalService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalService.class);
    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WithdrawalRequestRepository withdrawalRepository;
    private final LedgerService ledgerService;
    private final PlatformSettingsService settingsService;
    private final EncryptionService encryptionService;
    private final AuditLogService auditLogService;
    private final OutboxService outboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepository,
                             LedgerService ledgerService,
                             PlatformSettingsService settingsService,
                             EncryptionService encryptionService,
                             AuditLogService auditLogService,
                             OutboxService outboxService,
                             SimpMessagingTemplate messagingTemplate) {
        this.withdrawalRepository = withdrawalRepository;
        this.ledgerService = ledgerService;
        this.settingsService = settingsService;
        this.encryptionService = encryptionService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public WithdrawalDtos.WithdrawalResponse requestWithdrawal(UUID driverUserId, long amountVnd,
                                                              String bankCode, String accountNo,
                                                              String accountHolder) {
        long min = settingsService.getWithdrawMinVnd();
        long max = settingsService.getWithdrawMaxVnd();
        long dailyLimit = settingsService.getWithdrawDailyLimitVnd();

        if (amountVnd < min || amountVnd > max) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "WALLET_INVALID_AMOUNT",
                    String.format("Số tiền rút phải từ %,dđ đến %,dđ", min, max));
        }

        // Daily limit check
        ZonedDateTime startOfDayGmt7 = ZonedDateTime.now(GMT7_ZONE).toLocalDate().atStartOfDay(GMT7_ZONE);
        Instant startOfDay = startOfDayGmt7.toInstant();
        Long usedToday = withdrawalRepository.calculateDailyWithdrawnAmount(driverUserId, startOfDay);
        if (usedToday + amountVnd > dailyLimit) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WITHDRAWAL_DAILY_LIMIT",
                    String.format("Vượt hạn mức rút tiền trong ngày (Đã dùng: %,dđ / Hạn mức: %,dđ)", usedToday, dailyLimit));
        }

        // Check available driver earnings balance
        LedgerAccount driverAcc = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);
        BigDecimal currentBal = driverAcc.getBalance();
        BigDecimal requested = BigDecimal.valueOf(amountVnd);

        if (currentBal.compareTo(requested) < 0) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT_BALANCE",
                    String.format("Số dư thu nhập khả dụng không đủ (Hiện có: %,dđ, Yêu cầu: %,dđ)", currentBal.longValue(), amountVnd));
        }

        // 1. Post WITHDRAWAL_HOLD immediately
        LedgerAccount payoutClearing = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING);
        String holdIdemKey = "WITHDRAW-HOLD-" + driverUserId + "-" + System.currentTimeMillis();

        PostingRequest holdReq = new PostingRequest(
                null,
                null,
                LedgerTxType.WITHDRAWAL_HOLD,
                "Giu tien yeu cau rut thu nhap tai xe",
                holdIdemKey,
                "WITHDRAWAL",
                null,
                null,
                List.of(
                        new PostingEntry(driverAcc.getId(), EntryType.DEBIT, requested),
                        new PostingEntry(payoutClearing.getId(), EntryType.CREDIT, requested)
                )
        );
        LedgerTransaction holdTx = ledgerService.post(holdReq);

        // 2. Encrypt account number
        String encryptedAccount = encryptionService.encrypt(accountNo);
        String last4 = accountNo.length() >= 4 ? accountNo.substring(accountNo.length() - 4) : accountNo;

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setDriverUserId(driverUserId);
        wr.setAmountVnd(amountVnd);
        wr.setBankCode(bankCode);
        wr.setBankAccountNoEncrypted(encryptedAccount);
        wr.setBankAccountLast4(last4);
        wr.setAccountHolderName(accountHolder);
        wr.setStatus("PENDING");
        wr.setHoldLedgerTxId(holdTx.getId());

        WithdrawalRequest saved = withdrawalRepository.save(wr);

        // Record outbox event
        outboxService.recordEvent("Withdrawal", saved.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_WITHDRAWAL_UPDATED, Map.of(
                        "withdrawalId", saved.getId(),
                        "driverUserId", driverUserId,
                        "status", "PENDING",
                        "amountVnd", amountVnd
                ));

        return toDto(saved);
    }

    @Transactional
    public WithdrawalDtos.WithdrawalResponse approveWithdrawal(UUID adminId, String adminEmail,
                                                              UUID withdrawalId, String bankReference,
                                                              String ipAddress) {
        WithdrawalRequest wr = withdrawalRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "Không tìm thấy yêu cầu rút tiền"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WITHDRAWAL_ALREADY_PROCESSED",
                    "Yêu cầu rút tiền này đã được xử lý (Trạng thái hiện tại: " + wr.getStatus() + ")");
        }

        LedgerAccount payoutClearing = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING);
        LedgerAccount externalBank = ledgerService.getSystemAccount(AccountType.EXTERNAL_BANK);

        BigDecimal amount = BigDecimal.valueOf(wr.getAmountVnd());

        // Posting template WITHDRAWAL_PAID: Dr PAYOUT_CLEARING / Cr EXTERNAL_BANK
        PostingRequest paidReq = new PostingRequest(
                null,
                null,
                LedgerTxType.WITHDRAWAL_PAID,
                "Duyet chi rut tien tai xe (Ref: " + (bankReference != null ? bankReference : "N/A") + ")",
                "WITHDRAW-PAID-" + wr.getId(),
                "WITHDRAWAL",
                wr.getId(),
                null,
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(externalBank.getId(), EntryType.CREDIT, amount)
                )
        );
        LedgerTransaction paidTx = ledgerService.post(paidReq);

        wr.setStatus("APPROVED_PAID");
        wr.setReviewedBy(adminId);
        wr.setReviewedAt(Instant.now());
        wr.setFinalLedgerTxId(paidTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        auditLogService.record(adminId, adminEmail, "APPROVE_WITHDRAWAL", "WithdrawalRequest",
                saved.getId().toString(), Map.of("status", "PENDING"), Map.of("status", "APPROVED_PAID", "bankRef", bankReference != null ? bankReference : ""), ipAddress);

        notifyDriverWithdrawalUpdated(saved);

        return toDto(saved);
    }

    @Transactional
    public WithdrawalDtos.WithdrawalResponse rejectWithdrawal(UUID adminId, String adminEmail,
                                                             UUID withdrawalId, String reason,
                                                             String ipAddress) {
        WithdrawalRequest wr = withdrawalRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "Không tìm thấy yêu cầu rút tiền"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WITHDRAWAL_ALREADY_PROCESSED",
                    "Yêu cầu rút tiền này đã được xử lý (Trạng thái: " + wr.getStatus() + ")");
        }

        LedgerAccount payoutClearing = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING);
        LedgerAccount driverAcc = ledgerService.getOrCreateDriverEarningsAccount(wr.getDriverUserId());

        BigDecimal amount = BigDecimal.valueOf(wr.getAmountVnd());

        // Posting template WITHDRAWAL_REVERSAL: Dr PAYOUT_CLEARING / Cr DRIVER_EARNINGS
        PostingRequest revReq = new PostingRequest(
                null,
                null,
                LedgerTxType.WITHDRAWAL_REVERSAL,
                "Tu choi yeu cau rut tien: " + reason,
                "WITHDRAW-REV-" + wr.getId(),
                "WITHDRAWAL",
                wr.getId(),
                wr.getHoldLedgerTxId(),
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(driverAcc.getId(), EntryType.CREDIT, amount)
                )
        );
        LedgerTransaction revTx = ledgerService.post(revReq);

        wr.setStatus("REJECTED");
        wr.setReviewedBy(adminId);
        wr.setReviewedAt(Instant.now());
        wr.setRejectReason(reason);
        wr.setFinalLedgerTxId(revTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        auditLogService.record(adminId, adminEmail, "REJECT_WITHDRAWAL", "WithdrawalRequest",
                saved.getId().toString(), Map.of("status", "PENDING"), Map.of("status", "REJECTED", "reason", reason != null ? reason : ""), ipAddress);

        notifyDriverWithdrawalUpdated(saved);

        return toDto(saved);
    }

    @Transactional
    public WithdrawalDtos.WithdrawalResponse cancelWithdrawal(UUID driverUserId, UUID withdrawalId) {
        WithdrawalRequest wr = withdrawalRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "Không tìm thấy yêu cầu rút tiền"));

        if (!wr.getDriverUserId().equals(driverUserId)) {
            throw new PaymentBusinessException(HttpStatus.FORBIDDEN, "WITHDRAWAL_FORBIDDEN", "Không có quyền hủy yêu cầu rút tiền này");
        }

        if (!"PENDING".equals(wr.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "WITHDRAWAL_CANNOT_CANCEL", "Chỉ có thể hủy yêu cầu đang ở trạng thái chờ duyệt");
        }

        LedgerAccount payoutClearing = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING);
        LedgerAccount driverAcc = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);

        BigDecimal amount = BigDecimal.valueOf(wr.getAmountVnd());

        PostingRequest revReq = new PostingRequest(
                null,
                null,
                LedgerTxType.WITHDRAWAL_REVERSAL,
                "Tai xe tu huy yeu cau rut tien",
                "WITHDRAW-CANCEL-" + wr.getId(),
                "WITHDRAWAL",
                wr.getId(),
                wr.getHoldLedgerTxId(),
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(driverAcc.getId(), EntryType.CREDIT, amount)
                )
        );
        LedgerTransaction revTx = ledgerService.post(revReq);

        wr.setStatus("CANCELLED");
        wr.setFinalLedgerTxId(revTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        notifyDriverWithdrawalUpdated(saved);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalDtos.WithdrawalResponse> getDriverWithdrawals(UUID driverUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return withdrawalRepository.findByDriverUserIdOrderByCreatedAtDesc(driverUserId, pageable)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalDtos.WithdrawalResponse> getAdminWithdrawals(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        if (status != null && !status.isBlank()) {
            return withdrawalRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toDto);
        }
        return withdrawalRepository.findAll(pageable).map(this::toDto);
    }

    private void notifyDriverWithdrawalUpdated(WithdrawalRequest wr) {
        messagingTemplate.convertAndSendToUser(wr.getDriverUserId().toString(), "/queue/driver-payment", Map.of(
                "type", "WITHDRAWAL_UPDATED",
                "withdrawalId", wr.getId(),
                "status", wr.getStatus(),
                "amountVnd", wr.getAmountVnd()
        ));
    }

    private WithdrawalDtos.WithdrawalResponse toDto(WithdrawalRequest w) {
        return new WithdrawalDtos.WithdrawalResponse(
                w.getId(),
                w.getDriverUserId(),
                w.getAmountVnd(),
                w.getBankCode(),
                w.getBankAccountLast4(),
                w.getAccountHolderName(),
                w.getStatus(),
                w.getCreatedAt(),
                w.getReviewedAt(),
                w.getRejectReason()
        );
    }
}


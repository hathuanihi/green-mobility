package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.drivervehicle.service.DriverPublicService;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.*;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.service.LedgerService;
import com.greenmobility.modules.payment.domain.WithdrawalRequest;
import com.greenmobility.modules.payment.dto.WithdrawalDtos;
import com.greenmobility.modules.payment.repository.WithdrawalRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.UUID;

@Service
public class WithdrawalService {

    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WithdrawalRequestRepository withdrawalRepository;
    private final LedgerService ledgerService;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final PlatformSettingsService settingsService;
    private final EncryptionService encryptionService;
    private final AuditLogService auditLogService;
    private final OutboxService outboxService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DriverPublicService driverPublicService;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepository,
                             LedgerService ledgerService,
                             PlatformSettingsService settingsService,
                             EncryptionService encryptionService,
                             AuditLogService auditLogService,
                             OutboxService outboxService,
                             SimpMessagingTemplate messagingTemplate) {
        this(withdrawalRepository, ledgerService, null, settingsService, encryptionService,
                auditLogService, outboxService, messagingTemplate, null);
    }

    @Autowired
    public WithdrawalService(WithdrawalRequestRepository withdrawalRepository,
                             LedgerService ledgerService,
                             @Autowired(required = false) LedgerAccountRepository ledgerAccountRepository,
                             PlatformSettingsService settingsService,
                             EncryptionService encryptionService,
                             AuditLogService auditLogService,
                             OutboxService outboxService,
                             SimpMessagingTemplate messagingTemplate,
                             @Autowired(required = false) DriverPublicService driverPublicService) {
        this.withdrawalRepository = withdrawalRepository;
        this.ledgerService = ledgerService;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.settingsService = settingsService;
        this.encryptionService = encryptionService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.messagingTemplate = messagingTemplate;
        this.driverPublicService = driverPublicService;
    }

    @Transactional
    public WithdrawalDtos.WithdrawalResponse requestWithdrawal(UUID driverUserId, long amountVnd,
                                                              String bankCode, String accountNo,
                                                              String accountHolder) {
        if (driverPublicService != null) {
            Optional<String> kycStatus = driverPublicService.getKycStatusByUserId(driverUserId);
            if (kycStatus.isEmpty() || !"APPROVED".equalsIgnoreCase(kycStatus.get())) {
                throw new PaymentBusinessException(HttpStatus.CONFLICT, "WITHDRAWAL_KYC_REQUIRED",
                        "Tài xế phải hoàn tất duyệt hồ sơ KYC trước khi yêu cầu rút tiền");
            }
        }

        long min = settingsService.getWithdrawMinVnd();
        long max = settingsService.getWithdrawMaxVnd();
        long dailyLimit = settingsService.getWithdrawDailyLimitVnd();

        if (amountVnd < min || amountVnd > max) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "WALLET_INVALID_AMOUNT",
                    String.format("Số tiền rút phải từ %,dđ đến %,dđ", min, max));
        }

        // Lock DRIVER_EARNINGS account FOR UPDATE before checking daily limit and balance (prevents concurrent overdraft)
        LedgerAccount driverAcc = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);
        if (ledgerAccountRepository != null) {
            driverAcc = ledgerAccountRepository.findByIdWithLock(driverAcc.getId()).orElse(driverAcc);
        }

        ZonedDateTime startOfDayGmt7 = ZonedDateTime.now(GMT7_ZONE).toLocalDate().atStartOfDay(GMT7_ZONE);
        Instant startOfDay = startOfDayGmt7.toInstant();
        Long usedToday = withdrawalRepository.calculateDailyWithdrawnAmount(driverUserId, startOfDay);
        if (usedToday + amountVnd > dailyLimit) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WITHDRAWAL_DAILY_LIMIT",
                    String.format("Vượt hạn mức rút tiền trong ngày (Đã dùng: %,dđ / Hạn mức: %,dđ)", usedToday, dailyLimit));
        }

        BigDecimal currentBal = driverAcc.getBalance();
        BigDecimal requested = BigDecimal.valueOf(amountVnd);

        if (currentBal.compareTo(requested) < 0) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT_BALANCE",
                    String.format("Số dư thu nhập khả dụng không đủ (Hiện có: %,dđ, Yêu cầu: %,dđ)", currentBal.longValue(), amountVnd));
        }

        LedgerAccount payoutClearing = ledgerService.getSystemAccount(AccountType.PAYOUT_CLEARING);
        String holdIdemKey = "WITHDRAW-HOLD-" + driverUserId + "-" + UUID.randomUUID();

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
        recordWithdrawalOutbox(saved);
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

        LedgerTransaction paidTx = ledgerService.post(new PostingRequest(
                null, null, LedgerTxType.WITHDRAWAL_PAID,
                "Duyet chi rut tien tai xe (Ref: " + (bankReference != null ? bankReference : "N/A") + ")",
                "WITHDRAW-PAID-" + wr.getId(), "WITHDRAWAL", wr.getId(), null,
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(externalBank.getId(), EntryType.CREDIT, amount)
                )
        ));

        wr.setStatus("APPROVED_PAID");
        wr.setReviewedBy(adminId);
        wr.setReviewedAt(Instant.now());
        wr.setFinalLedgerTxId(paidTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        auditLogService.record(adminId, adminEmail, "APPROVE_WITHDRAWAL", "WithdrawalRequest",
                saved.getId().toString(), Map.of("status", "PENDING"),
                Map.of("status", "APPROVED_PAID", "bankRef", bankReference != null ? bankReference : ""), ipAddress);

        recordWithdrawalOutbox(saved);
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

        LedgerTransaction revTx = ledgerService.post(new PostingRequest(
                null, null, LedgerTxType.WITHDRAWAL_REVERSAL,
                "Tu choi yeu cau rut tien: " + reason,
                "WITHDRAW-REV-" + wr.getId(), "WITHDRAWAL", wr.getId(), wr.getHoldLedgerTxId(),
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(driverAcc.getId(), EntryType.CREDIT, amount)
                )
        ));

        wr.setStatus("REJECTED");
        wr.setReviewedBy(adminId);
        wr.setReviewedAt(Instant.now());
        wr.setRejectReason(reason);
        wr.setFinalLedgerTxId(revTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        auditLogService.record(adminId, adminEmail, "REJECT_WITHDRAWAL", "WithdrawalRequest",
                saved.getId().toString(), Map.of("status", "PENDING"),
                Map.of("status", "REJECTED", "reason", reason != null ? reason : ""), ipAddress);

        recordWithdrawalOutbox(saved);
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

        LedgerTransaction revTx = ledgerService.post(new PostingRequest(
                null, null, LedgerTxType.WITHDRAWAL_REVERSAL,
                "Tai xe tu huy yeu cau rut tien",
                "WITHDRAW-CANCEL-" + wr.getId(), "WITHDRAWAL", wr.getId(), wr.getHoldLedgerTxId(),
                List.of(
                        new PostingEntry(payoutClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(driverAcc.getId(), EntryType.CREDIT, amount)
                )
        ));

        wr.setStatus("CANCELLED");
        wr.setFinalLedgerTxId(revTx.getId());
        WithdrawalRequest saved = withdrawalRepository.save(wr);

        recordWithdrawalOutbox(saved);
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

    private void recordWithdrawalOutbox(WithdrawalRequest wr) {
        outboxService.recordEvent("Withdrawal", wr.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_WITHDRAWAL_UPDATED, Map.of(
                        "withdrawalId", wr.getId().toString(),
                        "driverUserId", wr.getDriverUserId().toString(),
                        "status", wr.getStatus(),
                        "amountVnd", wr.getAmountVnd()
                ));
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

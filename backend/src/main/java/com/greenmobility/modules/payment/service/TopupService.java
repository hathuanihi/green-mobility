package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.TopupDtos;
import com.greenmobility.modules.payment.gateway.CheckoutRequest;
import com.greenmobility.modules.payment.gateway.CheckoutResult;
import com.greenmobility.modules.payment.gateway.PaymentGateway;
import com.greenmobility.modules.payment.ledger.LedgerService;
import com.greenmobility.modules.payment.ledger.PostingEntry;
import com.greenmobility.modules.payment.ledger.PostingRequest;
import com.greenmobility.modules.payment.repository.TopupRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TopupService {

    private static final Logger log = LoggerFactory.getLogger(TopupService.class);

    private final TopupRequestRepository topupRepository;
    private final PlatformSettingsService settingsService;
    private final LedgerService ledgerService;
    private final PaymentService paymentService;
    private final OutboxService outboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public TopupService(TopupRequestRepository topupRepository,
                        PlatformSettingsService settingsService,
                        LedgerService ledgerService,
                        PaymentService paymentService,
                        OutboxService outboxService,
                        SimpMessagingTemplate messagingTemplate) {
        this.topupRepository = topupRepository;
        this.settingsService = settingsService;
        this.ledgerService = ledgerService;
        this.paymentService = paymentService;
        this.outboxService = outboxService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public TopupDtos.TopupResponse createTopup(UUID userId, long amountVnd, String gatewayName) {
        long min = settingsService.getTopupMinVnd();
        long max = settingsService.getTopupMaxVnd();

        if (amountVnd < min || amountVnd > max) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "WALLET_INVALID_AMOUNT",
                    String.format("Số tiền nạp phải từ %,dđ đến %,dđ", min, max));
        }

        long pendingCount = topupRepository.countByUserIdAndStatus(userId, "PENDING");
        if (pendingCount >= 5) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "TOPUP_MAX_PENDING",
                    "Bạn đã có 5 yêu cầu nạp tiền đang chờ. Vui lòng hoàn tất hoặc đợi hết hạn trước khi tạo yêu cầu mới.");
        }

        String normalizedGateway = gatewayName != null ? gatewayName.toUpperCase() : "VNPAY";
        String txnRef = "TOPUP" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Instant expiresAt = Instant.now().plusSeconds(900); // 15 mins

        TopupRequest topup = new TopupRequest();
        topup.setUserId(userId);
        topup.setAmountVnd(amountVnd);
        topup.setGateway(normalizedGateway);
        topup.setGatewayTxnRef(txnRef);
        topup.setStatus("PENDING");
        topup.setExpiresAt(expiresAt);

        TopupRequest saved = topupRepository.save(topup);

        PaymentGateway gateway = paymentService.selectGateway(normalizedGateway);
        CheckoutRequest req = new CheckoutRequest(
                txnRef,
                amountVnd,
                "Nap tien Vi Xanh Green Mobility",
                "127.0.0.1",
                null,
                expiresAt
        );

        CheckoutResult result = gateway.createCheckout(req);

        return new TopupDtos.TopupResponse(
                saved.getId(),
                saved.getAmountVnd(),
                saved.getGateway(),
                saved.getStatus(),
                result.payUrl(),
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void settleTopup(String gatewayTxnRef, String gatewayTransactionNo) {
        TopupRequest topup = topupRepository.findByGatewayTxnRefWithLock(gatewayTxnRef)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", "Không tìm thấy yêu cầu nạp tiền"));

        if ("PAID".equals(topup.getStatus())) {
            log.info("Topup {} is already settled. Idempotent return.", gatewayTxnRef);
            return;
        }

        LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(topup.getUserId());
        LedgerAccount gatewayClearing = ledgerService.getSystemAccount(AccountType.GATEWAY_CLEARING);

        BigDecimal amount = BigDecimal.valueOf(topup.getAmountVnd());

        // Posting template TOPUP: Dr GATEWAY_CLEARING / Cr USER_GREEN_WALLET
        PostingRequest postingRequest = new PostingRequest(
                null,
                null,
                LedgerTxType.TOPUP,
                "Nap tien Vi Xanh qua cong " + topup.getGateway(),
                "TOPUP-TX-" + topup.getId(),
                "TOPUP",
                topup.getId(),
                null,
                List.of(
                        new PostingEntry(gatewayClearing.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(userWallet.getId(), EntryType.CREDIT, amount)
                )
        );

        LedgerTransaction tx = ledgerService.post(postingRequest);

        topup.setStatus("PAID");
        topup.setLedgerTransactionId(tx.getId());
        topupRepository.save(topup);

        // Record outbox event
        Map<String, Object> outboxPayload = Map.of(
                "topupId", topup.getId(),
                "userId", topup.getUserId(),
                "amountVnd", topup.getAmountVnd(),
                "gateway", topup.getGateway(),
                "paidAt", Instant.now().toString()
        );
        outboxService.recordEvent("Topup", topup.getId().toString(), RabbitMQConfig.ROUTING_KEY_WALLET_TOPUP, outboxPayload);

        // Send WS notification
        messagingTemplate.convertAndSendToUser(topup.getUserId().toString(), "/queue/payment-status", Map.of(
                "type", "TOPUP_SUCCESS",
                "topupId", topup.getId(),
                "amountVnd", topup.getAmountVnd()
        ));
    }

    @Transactional(readOnly = true)
    public TopupDtos.TopupResponse getTopupById(UUID userId, UUID topupId) {
        TopupRequest topup = topupRepository.findById(topupId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", "Không tìm thấy yêu cầu nạp tiền"));

        if (!topup.getUserId().equals(userId)) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "TOPUP_NOT_FOUND", "Không tìm thấy yêu cầu nạp tiền");
        }

        return new TopupDtos.TopupResponse(
                topup.getId(),
                topup.getAmountVnd(),
                topup.getGateway(),
                topup.getStatus(),
                null,
                topup.getExpiresAt(),
                topup.getCreatedAt()
        );
    }
}


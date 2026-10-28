package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.*;
import com.greenmobility.modules.payment.gateway.*;
import com.greenmobility.modules.payment.ledger.*;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.repository.PaymentWebhookEventRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final LedgerService ledgerService;
    private final CommissionService commissionService;
    private final PlatformSettingsService settingsService;
    private final OutboxService outboxService;
    private final TripModuleApi tripModuleApi;
    private final SimpMessagingTemplate messagingTemplate;
    private final VnPayGateway vnPayGateway;
    private final MomoGateway momoGateway;
    private final Optional<MockGateway> mockGateway;
    private final GatewayProperties gatewayProperties;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentWebhookEventRepository webhookEventRepository,
                          LedgerService ledgerService,
                          CommissionService commissionService,
                          PlatformSettingsService settingsService,
                          OutboxService outboxService,
                          TripModuleApi tripModuleApi,
                          SimpMessagingTemplate messagingTemplate,
                          VnPayGateway vnPayGateway,
                          MomoGateway momoGateway,
                          Optional<MockGateway> mockGateway,
                          GatewayProperties gatewayProperties) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.ledgerService = ledgerService;
        this.commissionService = commissionService;
        this.settingsService = settingsService;
        this.outboxService = outboxService;
        this.tripModuleApi = tripModuleApi;
        this.messagingTemplate = messagingTemplate;
        this.vnPayGateway = vnPayGateway;
        this.momoGateway = momoGateway;
        this.mockGateway = mockGateway;
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * Consumer entrypoint: initiate payment when trip is completed. Idempotent by trip_id.
     */
    @Transactional
    public Payment initiatePaymentForTrip(TripCompletedEvent event) {
        UUID tripId = event.getTripId();
        Optional<Payment> existing = paymentRepository.findByTripId(tripId);
        if (existing.isPresent()) {
            log.info("Payment already initiated for trip {}: id={}, status={}", tripId, existing.get().getId(), existing.get().getStatus());
            return existing.get();
        }

        UUID driverUserId = tripModuleApi.getDriverUserId(event.getDriverId());
        BigDecimal fare = event.getFinalAmountVnd() != null ? event.getFinalAmountVnd() : BigDecimal.ZERO;
        BigDecimal commRate = settingsService.getCommissionRate();
        CommissionService.CommissionSplit split = commissionService.calculateSplit(fare, commRate);

        Payment payment = new Payment();
        payment.setTripId(tripId);
        payment.setCustomerId(event.getCustomerId());
        payment.setDriverUserId(driverUserId);
        payment.setMethod(event.getPaymentMethod() != null ? event.getPaymentMethod() : "CASH");
        payment.setAmountVnd(split.fareVnd());
        payment.setCommissionRate(split.commissionRate());
        payment.setCommissionVnd(split.commissionVnd());
        payment.setDriverAmountVnd(split.driverAmountVnd());
        payment.setStatus("INITIATED");

        Payment saved = paymentRepository.save(payment);

        // If fare is 0, settle immediately
        if (split.fareVnd() == 0) {
            settleZeroFarePayment(saved);
            return saved;
        }

        // Automatic settlement based on initial method
        String method = payment.getMethod();
        if ("GREEN_WALLET".equalsIgnoreCase(method) || "WALLET".equalsIgnoreCase(method)) {
            payment.setMethod("GREEN_WALLET");
            try {
                settle(saved.getId(), "GREEN_WALLET", null);
            } catch (PaymentBusinessException ex) {
                if ("WALLET_INSUFFICIENT_BALANCE".equals(ex.getErrorCode())) {
                    saved.setStatus("FAILED");
                    saved.setFailureCode("WALLET_INSUFFICIENT_BALANCE");
                    saved.setFailureMessage("Số dư Ví Xanh không đủ để thanh toán cuốc xe");
                    paymentRepository.save(saved);
                    tripModuleApi.markPaymentStatus(tripId, PaymentStatus.FAILED);
                } else {
                    throw ex;
                }
            }
        } else if ("CASH".equalsIgnoreCase(method)) {
            saved.setStatus("PENDING");
            paymentRepository.save(saved);
            tripModuleApi.markPaymentStatus(tripId, PaymentStatus.PENDING);
            notifyDriverCashRequired(saved);
        }

        return saved;
    }

    /**
     * Customer checkout: choose method and obtain payUrl or settle synchronously.
     */
    @Transactional
    public CheckoutResponse createCheckout(UUID customerId, UUID tripId, String method, String clientIp) {
        TripSnapshotDto trip = tripModuleApi.getTrip(tripId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy chuyến đi"));

        if (!trip.customerId().equals(customerId)) {
            throw new PaymentBusinessException(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "Bạn không có quyền thanh toán chuyến đi này");
        }

        if (trip.paymentStatus() == PaymentStatus.PAID) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PAID", "Chuyến đi này đã được thanh toán thành công");
        }

        Payment payment = paymentRepository.findActivePaymentByTripIdWithLock(tripId)
                .orElseGet(() -> {
                    BigDecimal fare = trip.finalAmount() != null ? trip.finalAmount() : BigDecimal.ZERO;
                    CommissionService.CommissionSplit split = commissionService.calculateSplit(fare, settingsService.getCommissionRate());
                    Payment p = new Payment();
                    p.setTripId(tripId);
                    p.setCustomerId(customerId);
                    p.setDriverUserId(trip.driverUserId());
                    p.setMethod(method);
                    p.setAmountVnd(split.fareVnd());
                    p.setCommissionRate(split.commissionRate());
                    p.setCommissionVnd(split.commissionVnd());
                    p.setDriverAmountVnd(split.driverAmountVnd());
                    p.setStatus("INITIATED");
                    return paymentRepository.save(p);
                });

        if ("PAID".equals(payment.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PAID", "Giao dịch đã thanh toán thành công");
        }

        String normalizedMethod = method.toUpperCase();
        payment.setMethod(normalizedMethod);

        if ("GREEN_WALLET".equals(normalizedMethod)) {
            LedgerAccount wallet = ledgerService.getOrCreateUserWallet(customerId);
            long fare = payment.getAmountVnd();
            if (wallet.getBalance().compareTo(BigDecimal.valueOf(fare)) < 0) {
                long shortfall = BigDecimal.valueOf(fare).subtract(wallet.getBalance()).longValue();
                Map<String, Object> errorData = Map.of("shortfallVnd", shortfall, "balanceVnd", wallet.getBalance().longValue());
                throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT_BALANCE",
                        "Số dư Ví Xanh không đủ để thanh toán (thiếu " + shortfall + "đ)", errorData);
            }

            settle(payment.getId(), "GREEN_WALLET", null);
            return new CheckoutResponse(payment.getId(), "PAID", "GREEN_WALLET", fare, null, null, null, null);
        }

        if ("CASH".equals(normalizedMethod)) {
            payment.setStatus("PENDING");
            Payment saved = paymentRepository.save(payment);
            tripModuleApi.markPaymentStatus(tripId, PaymentStatus.PENDING);
            notifyDriverCashRequired(saved);
            return new CheckoutResponse(saved.getId(), "PENDING", "CASH", saved.getAmountVnd(), null, null, null, null);
        }

        // Online Gateway: VNPAY or MOMO
        if (!"VNPAY".equals(normalizedMethod) && !"MOMO".equals(normalizedMethod)) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_METHOD", "Phương thức thanh toán không hợp lệ: " + method);
        }

        Instant now = Instant.now();
        int expireMins = settingsService.getPaymentExpireMinutes();
        Instant expiresAt = now.plusSeconds(expireMins * 60L);

        String txnRef = generateGatewayTxnRef(payment.getId());
        payment.setGateway(normalizedMethod);
        payment.setGatewayTxnRef(txnRef);
        payment.setExpiresAt(expiresAt);
        payment.setStatus("PENDING");
        Payment saved = paymentRepository.save(payment);
        tripModuleApi.markPaymentStatus(tripId, PaymentStatus.PROCESSING);

        PaymentGateway gateway = selectGateway(normalizedMethod);
        CheckoutRequest req = new CheckoutRequest(
                txnRef,
                saved.getAmountVnd(),
                "Thanh toan chuyen " + trip.tripCode(),
                clientIp,
                null,
                expiresAt
        );

        CheckoutResult result = gateway.createCheckout(req);
        return new CheckoutResponse(
                saved.getId(),
                "PENDING",
                normalizedMethod,
                saved.getAmountVnd(),
                result.payUrl(),
                result.deeplink(),
                result.qrCodeUrl(),
                expiresAt
        );
    }

    /**
     * Single source of truth to mark payment as PAID (R3, R4, R5, §7.2).
     */
    @Transactional
    public void settle(UUID paymentId, String source, String gatewayTxnNo) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        if ("PAID".equals(payment.getStatus())) {
            log.info("Payment {} is already settled (PAID). Idempotent exit.", paymentId);
            return;
        }

        if (List.of("CANCELLED", "EXPIRED", "FAILED").contains(payment.getStatus())) {
            log.warn("LATE_IPN: Payment {} was in {} state, but funds arrived from {}. Settling now.",
                    paymentId, payment.getStatus(), source);
        }

        long f = payment.getAmountVnd();
        long c = payment.getCommissionVnd();
        long d = payment.getDriverAmountVnd();
        String method = payment.getMethod();

        UUID ledgerTxId = null;

        if (f > 0) {
            LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);
            LedgerAccount driverEarnings = payment.getDriverUserId() != null
                    ? ledgerService.getOrCreateDriverEarningsAccount(payment.getDriverUserId())
                    : platformRev;

            List<PostingEntry> entries = new ArrayList<>();

            if ("GREEN_WALLET".equalsIgnoreCase(method)) {
                LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(payment.getCustomerId());
                entries.add(new PostingEntry(userWallet.getId(), EntryType.DEBIT, BigDecimal.valueOf(f)));
                if (d > 0) {
                    entries.add(new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(d)));
                }
                if (c > 0) {
                    entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));
                }

                PostingRequest postReq = new PostingRequest(
                        null,
                        payment.getTripId(),
                        LedgerTxType.TRIP_PAYMENT_WALLET,
                        "Thanh toan chuyen di bang Vi Xanh",
                        "PAYMENT-TX-" + payment.getId(),
                        "PAYMENT",
                        payment.getId(),
                        null,
                        entries
                );
                LedgerTransaction tx = ledgerService.post(postReq);
                ledgerTxId = tx.getId();

            } else if ("VNPAY".equalsIgnoreCase(method) || "MOMO".equalsIgnoreCase(method) || "MOCK".equalsIgnoreCase(method)) {
                LedgerAccount gatewayClearing = ledgerService.getSystemAccount(AccountType.GATEWAY_CLEARING);
                entries.add(new PostingEntry(gatewayClearing.getId(), EntryType.DEBIT, BigDecimal.valueOf(f)));
                if (d > 0) {
                    entries.add(new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(d)));
                }
                if (c > 0) {
                    entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));
                }

                PostingRequest postReq = new PostingRequest(
                        null,
                        payment.getTripId(),
                        LedgerTxType.TRIP_PAYMENT_GATEWAY,
                        "Thanh toan chuyen di qua cong " + method,
                        "PAYMENT-TX-" + payment.getId(),
                        "PAYMENT",
                        payment.getId(),
                        null,
                        entries
                );
                LedgerTransaction tx = ledgerService.post(postReq);
                ledgerTxId = tx.getId();

            } else if ("CASH".equalsIgnoreCase(method)) {
                if (c > 0 && driverEarnings != null) {
                    entries.add(new PostingEntry(driverEarnings.getId(), EntryType.DEBIT, BigDecimal.valueOf(c)));
                    entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));

                    PostingRequest postReq = new PostingRequest(
                            null,
                            payment.getTripId(),
                            LedgerTxType.TRIP_PAYMENT_CASH,
                            "Thu phi hoa hong chuyen tien mat",
                            "PAYMENT-TX-" + payment.getId(),
                            "PAYMENT",
                            payment.getId(),
                            null,
                            entries
                    );
                    LedgerTransaction tx = ledgerService.post(postReq);
                    ledgerTxId = tx.getId();
                }
            }
        }

        payment.setStatus("PAID");
        payment.setPaidAt(Instant.now());
        payment.setLedgerTransactionId(ledgerTxId);
        if (gatewayTxnNo != null) {
            payment.setGatewayTransactionNo(gatewayTxnNo);
        }
        paymentRepository.save(payment);

        tripModuleApi.markPaymentStatus(payment.getTripId(), PaymentStatus.PAID);

        // Record outbox event
        Map<String, Object> outboxPayload = Map.of(
                "paymentId", payment.getId(),
                "tripId", payment.getTripId(),
                "customerId", payment.getCustomerId(),
                "driverUserId", payment.getDriverUserId() != null ? payment.getDriverUserId().toString() : "",
                "method", payment.getMethod(),
                "amountVnd", payment.getAmountVnd(),
                "commissionVnd", payment.getCommissionVnd(),
                "paidAt", payment.getPaidAt().toString()
        );
        outboxService.recordEvent("Payment", payment.getId().toString(), RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCEEDED, outboxPayload);

        // Send WebSocket status updates
        notifyPaymentStatusChange(payment);
    }

    private void settleZeroFarePayment(Payment payment) {
        payment.setStatus("PAID");
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        tripModuleApi.markPaymentStatus(payment.getTripId(), PaymentStatus.PAID);
        notifyPaymentStatusChange(payment);
    }

    public PaymentDto getPaymentById(UUID callerUserId, String role, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        boolean isCustomer = payment.getCustomerId().equals(callerUserId);
        boolean isDriver = payment.getDriverUserId() != null && payment.getDriverUserId().equals(callerUserId);
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(role) || "ROLE_FINANCE".equalsIgnoreCase(role);

        if (!isCustomer && !isDriver && !isAdmin) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán");
        }

        return toDto(payment);
    }

    public PaymentDto getPaymentByTripId(UUID customerId, UUID tripId) {
        Payment payment = paymentRepository.findByTripId(tripId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán cho chuyến đi"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán");
        }

        return toDto(payment);
    }

    public List<PaymentMethodDto> getAvailableMethods(UUID customerId) {
        LedgerAccount wallet = ledgerService.getOrCreateUserWallet(customerId);
        long balance = wallet.getBalance().longValue();

        return List.of(
                new PaymentMethodDto("GREEN_WALLET", "Ví Xanh Green Mobility", true, balance, "wallet"),
                new PaymentMethodDto("CASH", "Tiền mặt", true, 0, "banknote"),
                new PaymentMethodDto("VNPAY", "Cổng VNPay", true, 0, "credit-card"),
                new PaymentMethodDto("MOMO", "Ví MoMo", true, 0, "smartphone")
        );
    }

    @Transactional
    public void cancelPayment(UUID customerId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new PaymentBusinessException(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "Không có quyền hủy thanh toán này");
        }

        if (!"PENDING".equals(payment.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_CANNOT_CANCEL", "Chỉ có thể hủy giao dịch đang chờ thanh toán");
        }

        payment.setStatus("CANCELLED");
        paymentRepository.save(payment);
        tripModuleApi.markPaymentStatus(payment.getTripId(), PaymentStatus.PENDING);
    }

    private void notifyPaymentStatusChange(Payment payment) {
        Map<String, Object> ws = Map.of(
                "paymentId", payment.getId(),
                "tripId", payment.getTripId(),
                "status", payment.getStatus(),
                "method", payment.getMethod(),
                "amountVnd", payment.getAmountVnd()
        );
        messagingTemplate.convertAndSendToUser(payment.getCustomerId().toString(), "/queue/payment-status", ws);
        if (payment.getDriverUserId() != null) {
            messagingTemplate.convertAndSendToUser(payment.getDriverUserId().toString(), "/queue/driver-payment", ws);
        }
    }

    private void notifyDriverCashRequired(Payment payment) {
        if (payment.getDriverUserId() == null) return;
        Map<String, Object> ws = Map.of(
                "type", "CASH_REQUIRED",
                "paymentId", payment.getId(),
                "tripId", payment.getTripId(),
                "amountVnd", payment.getAmountVnd()
        );
        messagingTemplate.convertAndSendToUser(payment.getDriverUserId().toString(), "/queue/driver-payment", ws);
    }

    public PaymentGateway selectGateway(String method) {
        if ("mock".equalsIgnoreCase(gatewayProperties.getGatewayMode()) && mockGateway.isPresent()) {
            return mockGateway.get();
        }
        if ("VNPAY".equalsIgnoreCase(method)) {
            return vnPayGateway;
        }
        if ("MOMO".equalsIgnoreCase(method)) {
            return momoGateway;
        }
        if (mockGateway.isPresent()) {
            return mockGateway.get();
        }
        return vnPayGateway;
    }

    private String generateGatewayTxnRef(UUID paymentId) {
        String compactId = paymentId.toString().replace("-", "").substring(0, 12);
        return System.currentTimeMillis() + compactId;
    }

    public PaymentDto toDto(Payment p) {
        return new PaymentDto(
                p.getId(),
                p.getTripId(),
                p.getMethod(),
                p.getStatus(),
                p.getAmountVnd(),
                p.getCommissionVnd() != null ? p.getCommissionVnd() : 0,
                p.getDriverAmountVnd() != null ? p.getDriverAmountVnd() : 0,
                null,
                null,
                null,
                p.getExpiresAt(),
                p.getPaidAt(),
                p.getCreatedAt()
        );
    }
}


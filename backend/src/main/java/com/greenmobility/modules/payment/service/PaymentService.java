package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.EntryType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import com.greenmobility.modules.ledger.domain.LedgerTxType;
import com.greenmobility.modules.ledger.service.LedgerService;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.dto.CheckoutResponse;
import com.greenmobility.modules.payment.dto.PaymentDto;
import com.greenmobility.modules.payment.dto.PaymentMethodDto;
import com.greenmobility.modules.payment.gateway.*;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.repository.PaymentWebhookEventRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
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

    @Transactional
    public Payment initiatePaymentForTrip(TripCompletedEvent event) {
        UUID tripId = event.getTripId();
        Optional<Payment> existing = paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(tripId);
        if (existing.isPresent()) {
            log.info("Payment already initiated for trip {}: id={}, status={}",
                    tripId, existing.get().getId(), existing.get().getStatus());
            return existing.get();
        }

        UUID driverUserId = tripModuleApi.getDriverUserId(event.getDriverId());
        BigDecimal finalFare = event.getFinalAmountVnd() != null ? event.getFinalAmountVnd() : BigDecimal.ZERO;
        BigDecimal grossFare = event.getFareAmountVnd() != null ? event.getFareAmountVnd() : finalFare;
        if (grossFare.compareTo(finalFare) < 0) {
            grossFare = finalFare;
        }
        long netVnd = Math.max(0L, finalFare.longValue());
        long grossVnd = Math.max(netVnd, grossFare.longValue());
        long discountVnd = grossVnd - netVnd;

        CommissionService.CommissionSplit split = commissionService.calculateSplit(
                BigDecimal.valueOf(grossVnd), settingsService.getCommissionRate());

        Payment payment = new Payment();
        payment.setTripId(tripId);
        payment.setCustomerId(event.getCustomerId());
        payment.setDriverUserId(driverUserId);
        String rawMethod = event.getPaymentMethod() != null ? event.getPaymentMethod() : "CASH";
        payment.setMethod("WALLET".equalsIgnoreCase(rawMethod) ? "GREEN_WALLET" : rawMethod.toUpperCase());
        payment.setAmountVnd(netVnd);
        payment.setGrossFareVnd(grossVnd);
        payment.setDiscountVnd(discountVnd);
        payment.setCommissionRate(split.commissionRate());
        payment.setCommissionVnd(split.commissionVnd());
        payment.setDriverAmountVnd(split.driverAmountVnd());
        payment.setStatus("INITIATED");

        Payment saved = paymentRepository.save(payment);

        if (netVnd == 0L) {
            settleZeroFarePayment(saved);
            return saved;
        }

        String method = saved.getMethod();
        if ("GREEN_WALLET".equalsIgnoreCase(method)) {
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
            notifyDriverCashRequired(saved, event.getDriverId());
        }

        return saved;
    }

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
                    BigDecimal finalFare = trip.finalAmount() != null ? trip.finalAmount() : BigDecimal.ZERO;
                    BigDecimal grossFare = trip.fareAmount() != null ? trip.fareAmount() : finalFare;
                    long netVnd = Math.max(0L, finalFare.longValue());
                    long grossVnd = Math.max(netVnd, grossFare.longValue());
                    long discountVnd = grossVnd - netVnd;
                    CommissionService.CommissionSplit split = commissionService.calculateSplit(
                            BigDecimal.valueOf(grossVnd), settingsService.getCommissionRate());

                    Payment p = new Payment();
                    p.setTripId(tripId);
                    p.setCustomerId(customerId);
                    p.setDriverUserId(trip.driverUserId());
                    p.setMethod(method);
                    p.setAmountVnd(netVnd);
                    p.setGrossFareVnd(grossVnd);
                    p.setDiscountVnd(discountVnd);
                    p.setCommissionRate(split.commissionRate());
                    p.setCommissionVnd(split.commissionVnd());
                    p.setDriverAmountVnd(split.driverAmountVnd());
                    p.setStatus("INITIATED");
                    return paymentRepository.save(p);
                });

        if ("PAID".equals(payment.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PAID", "Giao dịch đã thanh toán thành công");
        }

        String normalizedMethod = "WALLET".equalsIgnoreCase(method) ? "GREEN_WALLET" : method.toUpperCase();
        payment.setMethod(normalizedMethod);

        if (payment.getAmountVnd() == 0L) {
            settleZeroFarePayment(payment);
            return new CheckoutResponse(payment.getId(), "PAID", normalizedMethod, 0L, null, null, null, null);
        }

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
            notifyDriverCashRequired(saved, trip.driverId());
            return new CheckoutResponse(saved.getId(), "PENDING", "CASH", saved.getAmountVnd(), null, null, null, null);
        }

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

        long fNet = payment.getAmountVnd() != null ? payment.getAmountVnd() : 0L;
        long discountV = payment.getDiscountVnd() != null ? payment.getDiscountVnd() : 0L;
        long c = payment.getCommissionVnd() != null ? payment.getCommissionVnd() : 0L;
        long d = payment.getDriverAmountVnd() != null ? payment.getDriverAmountVnd() : 0L;
        String method = payment.getMethod();

        UUID ledgerTxId = null;

        if (fNet == 0L) {
            settleZeroFarePayment(payment);
            return;
        }

        LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);
        LedgerAccount driverEarnings = payment.getDriverUserId() != null
                ? ledgerService.getOrCreateDriverEarningsAccount(payment.getDriverUserId())
                : platformRev;

        if ("GREEN_WALLET".equalsIgnoreCase(method)) {
            List<PostingEntry> entries = new ArrayList<>();
            LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(payment.getCustomerId());
            entries.add(new PostingEntry(userWallet.getId(), EntryType.DEBIT, BigDecimal.valueOf(fNet)));
            if (discountV > 0) {
                LedgerAccount promoExpense = ledgerService.getSystemAccount(AccountType.PROMO_EXPENSE);
                entries.add(new PostingEntry(promoExpense.getId(), EntryType.DEBIT, BigDecimal.valueOf(discountV)));
            }
            if (d > 0) {
                entries.add(new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(d)));
            }
            if (c > 0) {
                entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));
            }

            LedgerTransaction tx = ledgerService.post(new PostingRequest(
                    null, payment.getTripId(), LedgerTxType.TRIP_PAYMENT_WALLET,
                    "Thanh toan chuyen di bang Vi Xanh", "PAYMENT-TX-" + payment.getId(),
                    "PAYMENT", payment.getId(), null, entries
            ));
            ledgerTxId = tx.getId();

        } else if ("VNPAY".equalsIgnoreCase(method) || "MOMO".equalsIgnoreCase(method) || "MOCK".equalsIgnoreCase(method)) {
            List<PostingEntry> entries = new ArrayList<>();
            LedgerAccount gatewayClearing = ledgerService.getSystemAccount(AccountType.GATEWAY_CLEARING);
            entries.add(new PostingEntry(gatewayClearing.getId(), EntryType.DEBIT, BigDecimal.valueOf(fNet)));
            if (discountV > 0) {
                LedgerAccount promoExpense = ledgerService.getSystemAccount(AccountType.PROMO_EXPENSE);
                entries.add(new PostingEntry(promoExpense.getId(), EntryType.DEBIT, BigDecimal.valueOf(discountV)));
            }
            if (d > 0) {
                entries.add(new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(d)));
            }
            if (c > 0) {
                entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));
            }

            LedgerTransaction tx = ledgerService.post(new PostingRequest(
                    null, payment.getTripId(), LedgerTxType.TRIP_PAYMENT_GATEWAY,
                    "Thanh toan chuyen di qua cong " + method, "PAYMENT-TX-" + payment.getId(),
                    "PAYMENT", payment.getId(), null, entries
            ));
            ledgerTxId = tx.getId();

        } else if ("CASH".equalsIgnoreCase(method)) {
            if (c > 0 && payment.getDriverUserId() != null) {
                LedgerTransaction tx = ledgerService.post(new PostingRequest(
                        null, payment.getTripId(), LedgerTxType.TRIP_PAYMENT_CASH,
                        "Thu phi hoa hong chuyen tien mat", "PAYMENT-TX-" + payment.getId(),
                        "PAYMENT", payment.getId(), null,
                        List.of(
                                new PostingEntry(driverEarnings.getId(), EntryType.DEBIT, BigDecimal.valueOf(c)),
                                new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c))
                        )
                ));
                ledgerTxId = tx.getId();
            }
            if (discountV > 0 && payment.getDriverUserId() != null) {
                LedgerAccount promoExpense = ledgerService.getSystemAccount(AccountType.PROMO_EXPENSE);
                LedgerTransaction subsidyTx = ledgerService.post(new PostingRequest(
                        null, payment.getTripId(), LedgerTxType.VOUCHER_SUBSIDY,
                        "Tro gia voucher chuyen tien mat", "PAYMENT-SUBSIDY-" + payment.getId(),
                        "VOUCHER_SUBSIDY", payment.getId(), null,
                        List.of(
                                new PostingEntry(promoExpense.getId(), EntryType.DEBIT, BigDecimal.valueOf(discountV)),
                                new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(discountV))
                        )
                ));
                if (ledgerTxId == null) {
                    ledgerTxId = subsidyTx.getId();
                }
            }
        }

        finalizePaidPayment(payment, ledgerTxId, gatewayTxnNo);
    }

    private void settleZeroFarePayment(Payment payment) {
        if ("PAID".equals(payment.getStatus())) {
            return;
        }
        long discountV = payment.getDiscountVnd() != null ? payment.getDiscountVnd() : 0L;
        long c = payment.getCommissionVnd() != null ? payment.getCommissionVnd() : 0L;
        long d = payment.getDriverAmountVnd() != null ? payment.getDriverAmountVnd() : 0L;
        UUID ledgerTxId = null;

        if (discountV > 0 && (c > 0 || d > 0)) {
            LedgerAccount promoExpense = ledgerService.getSystemAccount(AccountType.PROMO_EXPENSE);
            LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);
            LedgerAccount driverEarnings = payment.getDriverUserId() != null
                    ? ledgerService.getOrCreateDriverEarningsAccount(payment.getDriverUserId())
                    : platformRev;

            List<PostingEntry> entries = new ArrayList<>();
            entries.add(new PostingEntry(promoExpense.getId(), EntryType.DEBIT, BigDecimal.valueOf(discountV)));
            if (d > 0) {
                entries.add(new PostingEntry(driverEarnings.getId(), EntryType.CREDIT, BigDecimal.valueOf(d)));
            }
            if (c > 0) {
                entries.add(new PostingEntry(platformRev.getId(), EntryType.CREDIT, BigDecimal.valueOf(c)));
            }
            LedgerTransaction tx = ledgerService.post(new PostingRequest(
                    null, payment.getTripId(), LedgerTxType.VOUCHER_SUBSIDY,
                    "Tro gia voucher giam 100% cuoc xe", "PAYMENT-SUBSIDY-" + payment.getId(),
                    "VOUCHER_SUBSIDY", payment.getId(), null, entries
            ));
            ledgerTxId = tx.getId();
        }

        finalizePaidPayment(payment, ledgerTxId, null);
    }

    private void finalizePaidPayment(Payment payment, UUID ledgerTxId, String gatewayTxnNo) {
        payment.setStatus("PAID");
        payment.setPaidAt(Instant.now());
        payment.setLedgerTransactionId(ledgerTxId);
        if (gatewayTxnNo != null) {
            payment.setGatewayTransactionNo(gatewayTxnNo);
        }
        paymentRepository.save(payment);

        tripModuleApi.markPaymentStatus(payment.getTripId(), PaymentStatus.PAID);

        Map<String, Object> outboxPayload = Map.of(
                "paymentId", payment.getId().toString(),
                "tripId", payment.getTripId().toString(),
                "customerId", payment.getCustomerId().toString(),
                "driverUserId", payment.getDriverUserId() != null ? payment.getDriverUserId().toString() : "",
                "method", payment.getMethod(),
                "amountVnd", payment.getAmountVnd(),
                "discountVnd", payment.getDiscountVnd() != null ? payment.getDiscountVnd() : 0L,
                "commissionVnd", payment.getCommissionVnd() != null ? payment.getCommissionVnd() : 0L,
                "paidAt", payment.getPaidAt().toString()
        );
        outboxService.recordEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_PAYMENT_SUCCEEDED, outboxPayload);

        notifyPaymentStatusChange(payment);
    }

    public PaymentDto getPaymentById(UUID callerUserId, String role, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        boolean isCustomer = payment.getCustomerId().equals(callerUserId);
        boolean isDriver = payment.getDriverUserId() != null && payment.getDriverUserId().equals(callerUserId);
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(role)
                || "ROLE_FINANCE".equalsIgnoreCase(role)
                || "ROLE_OPERATOR".equalsIgnoreCase(role);

        if (!isCustomer && !isDriver && !isAdmin) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán");
        }

        return toDto(payment);
    }

    public PaymentDto getPaymentByTripId(UUID customerId, UUID tripId) {
        Payment payment = paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(tripId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán cho chuyến đi"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán");
        }

        return toDto(payment);
    }

    @Transactional
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
        messagingTemplate.convertAndSend("/topic/trip/" + payment.getTripId() + "/payment", ws);
        messagingTemplate.convertAndSendToUser(payment.getCustomerId().toString(), "/queue/payment-status", ws);
        if (payment.getDriverUserId() != null) {
            messagingTemplate.convertAndSendToUser(payment.getDriverUserId().toString(), "/queue/driver-payment", ws);
        }
    }

    private void notifyDriverCashRequired(Payment payment, UUID driverProfileId) {
        Map<String, Object> ws = Map.of(
                "type", "CASH_REQUIRED",
                "paymentId", payment.getId(),
                "tripId", payment.getTripId(),
                "amountVnd", payment.getAmountVnd()
        );
        if (payment.getDriverUserId() != null) {
            messagingTemplate.convertAndSendToUser(payment.getDriverUserId().toString(), "/queue/driver-payment", ws);
        }
        if (driverProfileId != null) {
            messagingTemplate.convertAndSendToUser(driverProfileId.toString(), "/queue/driver-payment", ws);
        }
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
        String shortId = paymentId.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "GM" + System.currentTimeMillis() + shortId;
    }

    public PaymentDto toDto(Payment p) {
        return new PaymentDto(
                p.getId(),
                p.getTripId(),
                p.getMethod(),
                p.getStatus(),
                p.getAmountVnd(),
                p.getCommissionVnd() != null ? p.getCommissionVnd() : 0L,
                p.getDriverAmountVnd() != null ? p.getDriverAmountVnd() : 0L,
                null,
                null,
                null,
                p.getExpiresAt(),
                p.getPaidAt(),
                p.getCreatedAt()
        );
    }
}

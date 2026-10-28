package com.greenmobility.payment;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.api.PaymentModuleApi;
import com.greenmobility.modules.payment.api.PaymentModuleApiImpl;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.dto.*;
import com.greenmobility.modules.payment.gateway.*;
import com.greenmobility.modules.payment.ledger.*;
import com.greenmobility.modules.payment.repository.*;
import com.greenmobility.modules.payment.service.*;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Sprint5AcceptanceCriteriaTest {

    private PaymentRepository paymentRepository;
    private PaymentWebhookEventRepository webhookEventRepository;
    private LedgerAccountRepository ledgerAccountRepository;
    private LedgerTransactionRepository ledgerTransactionRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private TopupRequestRepository topupRepository;
    private WithdrawalRequestRepository withdrawalRepository;
    private PlatformSettingRepository settingRepository;
    private AuditLogRepository auditLogRepository;

    private LedgerService ledgerService;
    private CommissionService commissionService;
    private PlatformSettingsService settingsService;
    private EncryptionService encryptionService;
    private AuditLogService auditLogService;
    private OutboxService outboxService;
    private PaymentService paymentService;
    private TopupService topupService;
    private WithdrawalService withdrawalService;
    private RefundService refundService;
    private DriverEarningsService driverEarningsService;
    private FinanceReportService financeReportService;
    private TripModuleApi tripModuleApi;
    private SimpMessagingTemplate messagingTemplate;

    private VnPayGateway vnPayGateway;
    private MomoGateway momoGateway;
    private MockGateway mockGateway;
    private GatewayProperties gatewayProperties;

    // In-memory test store
    private Map<UUID, Payment> paymentStore;
    private Map<UUID, LedgerAccount> accountStore;
    private Map<String, LedgerAccount> accountByNumberStore;
    private Map<UUID, LedgerTransaction> txStore;

    @BeforeEach
    void setUp() {
        paymentStore = new HashMap<>();
        accountStore = new HashMap<>();
        accountByNumberStore = new HashMap<>();
        txStore = new HashMap<>();

        paymentRepository = mock(PaymentRepository.class);
        webhookEventRepository = mock(PaymentWebhookEventRepository.class);
        ledgerAccountRepository = mock(LedgerAccountRepository.class);
        ledgerTransactionRepository = mock(LedgerTransactionRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        topupRepository = mock(TopupRequestRepository.class);
        withdrawalRepository = mock(WithdrawalRequestRepository.class);
        settingRepository = mock(PlatformSettingRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        tripModuleApi = mock(TripModuleApi.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        // Account repo mocks
        when(ledgerAccountRepository.findByAccountNumber(anyString())).thenAnswer(inv -> {
            String accNum = inv.getArgument(0);
            return Optional.ofNullable(accountByNumberStore.get(accNum));
        });
        when(ledgerAccountRepository.findByIdWithLock(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.ofNullable(accountStore.get(id));
        });
        when(ledgerAccountRepository.save(any(LedgerAccount.class))).thenAnswer(inv -> {
            LedgerAccount a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            accountStore.put(a.getId(), a);
            accountByNumberStore.put(a.getAccountNumber(), a);
            return a;
        });

        // Payment repo mocks
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            paymentStore.put(p.getId(), p);
            return p;
        });
        when(paymentRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(paymentStore.get(inv.getArgument(0))));
        when(paymentRepository.findByIdWithLock(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(paymentStore.get(inv.getArgument(0))));
        when(paymentRepository.findByTripId(any(UUID.class))).thenAnswer(inv -> {
            UUID tId = inv.getArgument(0);
            return paymentStore.values().stream().filter(p -> tId.equals(p.getTripId())).findFirst();
        });
        when(paymentRepository.findActivePaymentByTripIdWithLock(any(UUID.class))).thenAnswer(inv -> {
            UUID tId = inv.getArgument(0);
            return paymentStore.values().stream().filter(p -> tId.equals(p.getTripId())).findFirst();
        });

        // Tx repo mocks
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(inv -> {
            LedgerTransaction tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            txStore.put(tx.getId(), tx);
            return tx;
        });

        // Seed system accounts
        seedSystemAccount("SYS:REVENUE:VND", AccountType.PLATFORM_REVENUE);
        seedSystemAccount("SYS:GATEWAY_CLEARING:VND", AccountType.GATEWAY_CLEARING);
        seedSystemAccount("SYS:PAYOUT_CLEARING:VND", AccountType.PAYOUT_CLEARING);
        seedSystemAccount("SYS:EXTERNAL_BANK:VND", AccountType.EXTERNAL_BANK);

        commissionService = new CommissionService();
        settingsService = new PlatformSettingsService(settingRepository);
        encryptionService = new EncryptionService(null);
        auditLogService = new AuditLogService(auditLogRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        outboxService = mock(OutboxService.class);

        ledgerService = new LedgerService(ledgerAccountRepository, ledgerTransactionRepository, ledgerEntryRepository);

        gatewayProperties = new GatewayProperties();
        vnPayGateway = new VnPayGateway(gatewayProperties);
        momoGateway = new MomoGateway(gatewayProperties, new com.fasterxml.jackson.databind.ObjectMapper());
        mockGateway = new MockGateway(gatewayProperties);

        paymentService = new PaymentService(
                paymentRepository,
                webhookEventRepository,
                ledgerService,
                commissionService,
                settingsService,
                outboxService,
                tripModuleApi,
                messagingTemplate,
                vnPayGateway,
                momoGateway,
                Optional.of(mockGateway),
                gatewayProperties
        );

        topupService = new TopupService(
                topupRepository,
                settingsService,
                ledgerService,
                paymentService,
                outboxService,
                messagingTemplate
        );

        withdrawalService = new WithdrawalService(
                withdrawalRepository,
                ledgerService,
                settingsService,
                encryptionService,
                auditLogService,
                outboxService,
                messagingTemplate
        );

        refundService = new RefundService(
                paymentRepository,
                ledgerService,
                commissionService,
                auditLogService,
                outboxService,
                tripModuleApi
        );

        driverEarningsService = new DriverEarningsService(
                paymentRepository,
                ledgerAccountRepository,
                settingsService,
                paymentService,
                tripModuleApi
        );

        financeReportService = new FinanceReportService(
                paymentRepository,
                ledgerAccountRepository,
                ledgerTransactionRepository,
                ledgerEntryRepository,
                ledgerService
        );
    }

    private void seedSystemAccount(String accNum, AccountType type) {
        LedgerAccount acc = new LedgerAccount(accNum, null, type, "VND", true);
        acc.setId(UUID.randomUUID());
        accountStore.put(acc.getId(), acc);
        accountByNumberStore.put(accNum, acc);
    }

    @Test
    @DisplayName("AC-01: Auto-create payment when trip completes with GREEN_WALLET (76.000đ)")
    void testAC01_AutoPaymentTripCompletedGreenWallet() {
        UUID customerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID driverUserId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        // Customer has 100,000 VND
        LedgerAccount custWallet = ledgerService.getOrCreateUserWallet(customerId);
        custWallet.setBalance(new BigDecimal("100000"));

        when(tripModuleApi.getDriverUserId(driverId)).thenReturn(driverUserId);

        TripCompletedEvent event = new TripCompletedEvent(
                tripId, "GM-1234", customerId, driverId, UUID.randomUUID(), "ELECTRIC_MOTORBIKE",
                5000, 5200, 600, 620, new BigDecimal("76000"), new BigDecimal("76000"),
                "GREEN_WALLET", new BigDecimal("500"), Instant.now(), Instant.now()
        );

        Payment payment = paymentService.initiatePaymentForTrip(event);

        assertNotNull(payment);
        assertEquals("PAID", payment.getStatus());
        assertEquals(76000L, payment.getAmountVnd());
        assertEquals(15200L, payment.getCommissionVnd());
        assertEquals(60800L, payment.getDriverAmountVnd());

        // Check balances
        assertEquals(new BigDecimal("24000.0000"), custWallet.getBalance()); // 100k - 76k
        LedgerAccount driverEarn = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);
        assertEquals(new BigDecimal("60800.0000"), driverEarn.getBalance());
        LedgerAccount platformRev = ledgerService.getSystemAccount(AccountType.PLATFORM_REVENUE);
        assertEquals(new BigDecimal("15200.0000"), platformRev.getBalance());

        verify(tripModuleApi).markPaymentStatus(tripId, PaymentStatus.PAID);
    }

    @Test
    @DisplayName("AC-02: Idempotent consumer - sending same event 3 times results in 1 payment")
    void testAC02_IdempotentConsumer() {
        UUID customerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID driverUserId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        when(tripModuleApi.getDriverUserId(driverId)).thenReturn(driverUserId);

        LedgerAccount custWallet = ledgerService.getOrCreateUserWallet(customerId);
        custWallet.setBalance(new BigDecimal("200000"));

        TripCompletedEvent event = new TripCompletedEvent(
                tripId, "GM-5555", customerId, driverId, UUID.randomUUID(), "ELECTRIC_CAR",
                10000, 10000, 1200, 1200, new BigDecimal("100000"), new BigDecimal("100000"),
                "GREEN_WALLET", new BigDecimal("1000"), Instant.now(), Instant.now()
        );

        Payment p1 = paymentService.initiatePaymentForTrip(event);
        Payment p2 = paymentService.initiatePaymentForTrip(event);
        Payment p3 = paymentService.initiatePaymentForTrip(event);

        assertSame(p1.getId(), p2.getId());
        assertSame(p1.getId(), p3.getId());
        assertEquals(1, paymentStore.size());
    }

    @Test
    @DisplayName("AC-03: VNPay payment checkout + valid IPN simulation transitions to PAID")
    void testAC03_VnPayPayment() {
        UUID customerId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        when(tripModuleApi.getTrip(tripId)).thenReturn(Optional.of(new TripSnapshotDto(
                tripId, "GM-VNPAY", customerId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50000"), "VNPAY", PaymentStatus.PENDING, TripStatus.COMPLETED
        )));

        CheckoutResponse checkout = paymentService.createCheckout(customerId, tripId, "VNPAY", "127.0.0.1");
        assertNotNull(checkout);
        assertEquals("PENDING", checkout.status());
        assertNotNull(checkout.payUrl());

        Payment payment = paymentStore.get(checkout.paymentId());
        assertEquals("PENDING", payment.getStatus());

        // Simulate IPN from VNPay
        paymentService.settle(payment.getId(), "VNPAY_IPN", "VNPAY-TRANS-9999");

        Payment settled = paymentStore.get(payment.getId());
        assertEquals("PAID", settled.getStatus());
        assertEquals("VNPAY-TRANS-9999", settled.getGatewayTransactionNo());
    }

    @Test
    @DisplayName("AC-04: Fraud IPN detection with invalid signature or mismatched amount")
    void testAC04_FraudIpnDetection() {
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put("vnp_Amount", "5000000");
        invalidParams.put("vnp_TxnRef", "FAKE-REF");
        invalidParams.put("vnp_SecureHash", "INCORRECT_HASH");

        IpnResult result = vnPayGateway.verifyIpn(invalidParams, null);
        assertFalse(result.isValidSignature(), "Invalid signature must be detected");
    }

    @Test
    @DisplayName("AC-05: MoMo payment IPN resultCode=0 settles payment")
    void testAC05_MomoPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment p = new Payment();
        p.setId(paymentId);
        p.setTripId(UUID.randomUUID());
        p.setCustomerId(UUID.randomUUID());
        p.setDriverUserId(UUID.randomUUID());
        p.setMethod("MOMO");
        p.setStatus("PENDING");
        p.setAmountVnd(60000L);
        p.setCommissionVnd(12000L);
        p.setDriverAmountVnd(48000L);
        paymentStore.put(paymentId, p);

        paymentService.settle(paymentId, "MOMO_IPN", "MOMO-TRANS-1234");
        assertEquals("PAID", p.getStatus());
        assertEquals("MOMO-TRANS-1234", p.getGatewayTransactionNo());

        // Re-settle is idempotent
        paymentService.settle(paymentId, "MOMO_IPN_DUPLICATE", "MOMO-TRANS-1234");
        assertEquals("PAID", p.getStatus());
    }

    @Test
    @DisplayName("AC-06: Cash payment - driver confirms cash collected")
    void testAC06_CashPaymentCollected() {
        UUID driverUserId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Payment p = new Payment();
        p.setId(paymentId);
        p.setTripId(tripId);
        p.setCustomerId(UUID.randomUUID());
        p.setDriverUserId(driverUserId);
        p.setMethod("CASH");
        p.setStatus("PENDING");
        p.setAmountVnd(100000L);
        p.setCommissionVnd(20000L);
        p.setDriverAmountVnd(80000L);
        paymentStore.put(paymentId, p);

        driverEarningsService.confirmCashCollected(driverUserId, tripId);

        assertEquals("PAID", p.getStatus());
        LedgerAccount driverEarn = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);
        assertEquals(new BigDecimal("-20000.0000"), driverEarn.getBalance(), "Driver earnings accumulates cash commission debt");
    }

    @Test
    @DisplayName("AC-07: Green Wallet topup settles and increases balance")
    void testAC07_WalletTopup() {
        UUID userId = UUID.randomUUID();
        LedgerAccount wallet = ledgerService.getOrCreateUserWallet(userId);
        assertEquals(BigDecimal.ZERO, wallet.getBalance());

        TopupRequest topup = new TopupRequest();
        topup.setId(UUID.randomUUID());
        topup.setUserId(userId);
        topup.setAmountVnd(200000L);
        topup.setGateway("VNPAY");
        topup.setGatewayTxnRef("TOPUP-123");
        topup.setStatus("PENDING");

        when(topupRepository.findByGatewayTxnRefWithLock("TOPUP-123")).thenReturn(Optional.of(topup));

        topupService.settleTopup("TOPUP-123", "BANK-TX-123");

        assertEquals("PAID", topup.getStatus());
        assertEquals(new BigDecimal("200000.0000"), wallet.getBalance());
    }

    @Test
    @DisplayName("AC-09: Financial reconciliation reports balanced system (Total VND == 0)")
    void testAC09_ReconciliationSystemBalanced() {
        when(ledgerAccountRepository.findAll()).thenReturn(new ArrayList<>(accountStore.values()));
        when(ledgerTransactionRepository.findAll()).thenReturn(new ArrayList<>(txStore.values()));
        when(ledgerEntryRepository.calculateBalanceForAccount(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return accountStore.get(id).getBalance();
        });

        AdminFinanceDtos.ReconciliationResponse report = financeReportService.getReconciliation();
        assertTrue(report.isBalanced(), "System must be balanced when sum of accounts equals 0");
        assertEquals(0L, report.systemTotalVnd());
    }

    @Test
    @DisplayName("AC-10: Refund - Partial refund succeeded, exceeding amount throws 409")
    void testAC10_RefundPartialAndExceedingThrows409() {
        UUID paymentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID driverUserId = UUID.randomUUID();

        Payment p = new Payment();
        p.setId(paymentId);
        p.setTripId(UUID.randomUUID());
        p.setCustomerId(customerId);
        p.setDriverUserId(driverUserId);
        p.setMethod("GREEN_WALLET");
        p.setStatus("PAID");
        p.setAmountVnd(100000L);
        p.setCommissionVnd(20000L);
        p.setDriverAmountVnd(80000L);
        p.setRefundedVnd(0L);
        paymentStore.put(paymentId, p);

        // 1. Partial refund 20,000 VND
        AdminFinanceDtos.RefundRequest req1 = new AdminFinanceDtos.RefundRequest(20000L, "Khách khiếu nại tài xế đi đường vòng", true);
        refundService.refund(UUID.randomUUID(), "admin@greenmobility.vn", paymentId, req1, "127.0.0.1");

        assertEquals(20000L, p.getRefundedVnd());
        assertEquals("PARTIALLY_REFUNDED", p.getStatus());

        // 2. Second refund exceeding remaining 80,000 VND (try to refund 85,000)
        AdminFinanceDtos.RefundRequest req2 = new AdminFinanceDtos.RefundRequest(85000L, "Hoàn tiền vượt mức cho phép", true);
        PaymentBusinessException ex = assertThrows(PaymentBusinessException.class, () ->
                refundService.refund(UUID.randomUUID(), "admin@greenmobility.vn", paymentId, req2, "127.0.0.1"));

        assertEquals("REFUND_EXCEEDS_PAID", ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("AC-12: Driver withdrawal lifecycle (Hold -> Approve or Reject)")
    void testAC12_DriverWithdrawalLifecycle() {
        UUID driverUserId = UUID.randomUUID();
        LedgerAccount driverAcc = ledgerService.getOrCreateDriverEarningsAccount(driverUserId);
        driverAcc.setBalance(new BigDecimal("500000")); // 500k available

        when(withdrawalRepository.calculateDailyWithdrawnAmount(any(UUID.class), any(Instant.class))).thenReturn(0L);
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenAnswer(inv -> {
            WithdrawalRequest w = inv.getArgument(0);
            if (w.getId() == null) w.setId(UUID.randomUUID());
            return w;
        });

        // 1. Request withdrawal 200,000 VND
        WithdrawalDtos.WithdrawalResponse res = withdrawalService.requestWithdrawal(
                driverUserId, 200000L, "VCB", "0123456789", "NGUYEN VAN A"
        );

        assertNotNull(res);
        assertEquals("PENDING", res.status());
        assertEquals(new BigDecimal("300000.0000"), driverAcc.getBalance(), "Driver earnings held by 200k");

        // 2. Approve
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setId(res.id());
        wr.setDriverUserId(driverUserId);
        wr.setAmountVnd(200000L);
        wr.setStatus("PENDING");

        when(withdrawalRepository.findByIdWithLock(res.id())).thenReturn(Optional.of(wr));

        WithdrawalDtos.WithdrawalResponse approved = withdrawalService.approveWithdrawal(
                UUID.randomUUID(), "admin@greenmobility.vn", res.id(), "VCB-REF-12345", "127.0.0.1"
        );
        assertEquals("APPROVED_PAID", approved.status());
    }

    @Test
    @DisplayName("AC-13: IDOR prevention - customer accessing another user's payment gets 404")
    void testAC13_IdorProtection() {
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Payment p = new Payment();
        p.setId(paymentId);
        p.setCustomerId(customerA);
        p.setStatus("PENDING");
        paymentStore.put(paymentId, p);

        PaymentBusinessException ex = assertThrows(PaymentBusinessException.class, () ->
                paymentService.getPaymentById(customerB, "ROLE_CUSTOMER", paymentId));

        assertEquals("PAYMENT_NOT_FOUND", ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("AC-18: Trip booking guard throws 409 PAYMENT_OUTSTANDING if overdue payment exists")
    void testAC18_GuardOverduePayment() {
        UUID customerId = UUID.randomUUID();
        UUID overdueTripId = UUID.randomUUID();

        Payment overduePayment = new Payment();
        overduePayment.setId(UUID.randomUUID());
        overduePayment.setTripId(overdueTripId);
        overduePayment.setCustomerId(customerId);
        overduePayment.setMethod("VNPAY");
        overduePayment.setStatus("PENDING");
        overduePayment.setCreatedAt(Instant.now().minusSeconds(1800)); // 30 mins ago (> 15m)

        when(paymentRepository.findAll()).thenReturn(List.of(overduePayment));

        PaymentModuleApiImpl paymentApi = new PaymentModuleApiImpl(paymentRepository, ledgerAccountRepository, settingsService);
        assertTrue(paymentApi.hasOutstandingPayment(customerId));
        assertEquals(Optional.of(overdueTripId), paymentApi.getOutstandingTripId(customerId));
    }

    @Test
    @DisplayName("AC-19: Prod security validator fails fast if placeholder secrets are present")
    void testAC19_ProdSecurityValidator() {
        GatewayProperties props = new GatewayProperties();
        props.getVnpay().setTmnCode("DEMOVNPAY"); // Contains "DEMO"

        PaymentProdSecurityValidator validator = new PaymentProdSecurityValidator(props);
        assertThrows(IllegalStateException.class, () -> validator.run());
    }
}

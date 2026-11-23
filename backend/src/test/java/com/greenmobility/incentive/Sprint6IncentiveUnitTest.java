package com.greenmobility.incentive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.domain.OutboxEvent;
import com.greenmobility.common.platform.job.OutboxPublisher;
import com.greenmobility.common.platform.repository.OutboxEventRepository;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.fraud.api.FraudModuleApi;
import com.greenmobility.modules.incentive.api.IncentiveModuleApi;
import com.greenmobility.modules.incentive.domain.Reward;
import com.greenmobility.modules.incentive.domain.RewardRedemption;
import com.greenmobility.modules.incentive.domain.TripIncentiveAward;
import com.greenmobility.modules.incentive.repository.RewardRedemptionRepository;
import com.greenmobility.modules.incentive.repository.RewardRepository;
import com.greenmobility.modules.incentive.repository.TripIncentiveAwardRepository;
import com.greenmobility.modules.incentive.service.EcoStoreService;
import com.greenmobility.modules.incentive.service.IncentiveAwardService;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.*;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.dto.CarbonEstimateDto;
import com.greenmobility.modules.trip.entity.PaymentMethod;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.service.CarbonEstimateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Sprint6IncentiveUnitTest {

    private TripIncentiveAwardRepository awardRepository;
    private LedgerModuleApi ledgerModuleApi;
    private LedgerAccountRepository accountRepository;
    private PlatformSettingsService settingsService;
    private OutboxService outboxService;
    private AuditLogService auditLogService;
    private TripModuleApi tripModuleApi;
    private FraudModuleApi fraudModuleApi;
    private IncentiveAwardService awardService;

    private RewardRepository rewardRepository;
    private RewardRedemptionRepository redemptionRepository;
    private EcoStoreService ecoStoreService;

    @BeforeEach
    void setUp() {
        awardRepository = mock(TripIncentiveAwardRepository.class);
        ledgerModuleApi = mock(LedgerModuleApi.class);
        accountRepository = mock(LedgerAccountRepository.class);
        settingsService = mock(PlatformSettingsService.class);
        outboxService = mock(OutboxService.class);
        auditLogService = mock(AuditLogService.class);
        tripModuleApi = mock(TripModuleApi.class);
        fraudModuleApi = mock(FraudModuleApi.class);

        when(settingsService.getPccGramsPerUnit()).thenReturn(new BigDecimal("1000"));
        when(settingsService.getPccCustomerShareRatio()).thenReturn(new BigDecimal("0.70"));
        when(settingsService.getEcoPointGramsPerPoint()).thenReturn(new BigDecimal("100"));
        when(settingsService.getDriverEcoPointRatio()).thenReturn(new BigDecimal("0.50"));

        awardService = new IncentiveAwardService(
                awardRepository, ledgerModuleApi, accountRepository,
                settingsService, outboxService, auditLogService,
                tripModuleApi, fraudModuleApi, mock(SimpMessagingTemplate.class)
        );

        rewardRepository = mock(RewardRepository.class);
        redemptionRepository = mock(RewardRedemptionRepository.class);
        ecoStoreService = new EcoStoreService(
                rewardRepository, redemptionRepository, ledgerModuleApi,
                accountRepository, settingsService, outboxService,
                auditLogService, new ObjectMapper()
        );
    }

    @Test
    @DisplayName("AC-06: Chia PCC 70/30 dùng floor scale 4 bảo toàn 100% CO2 & bỏ qua entry 0")
    void testPcc7030RoundingAndZeroEntrySkip() {
        UUID tripId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        // Case 1: 333.33g -> T = 0.3333 PCC -> C = floor(0.3333 * 0.7, 4) = 0.2333, D = 0.1000
        TripIncentiveAward award1 = new TripIncentiveAward();
        award1.setId(UUID.randomUUID());
        award1.setTripId(tripId);
        award1.setCustomerId(customerId);
        award1.setDriverUserId(driverId);
        award1.setStatus("PENDING_CONDITIONS");
        award1.setPaymentReady(true);

        when(awardRepository.findByTripIdWithLock(tripId)).thenReturn(Optional.of(award1));
        when(awardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tripModuleApi.getTrip(tripId)).thenReturn(Optional.empty());
        when(fraudModuleApi.hasBlockingFraudAlert(tripId)).thenReturn(false);

        LedgerAccount sysCarbon = makeAcc("SYS:CARBON_RESERVE:PCC", AccountType.SYSTEM_CARBON_RESERVE, "PCC", BigDecimal.ZERO);
        LedgerAccount sysLoyalty = makeAcc("SYS:LOYALTY_RESERVE:ECO_POINT", AccountType.SYSTEM_LOYALTY_RESERVE, "ECO_POINT", BigDecimal.ZERO);
        LedgerAccount custPccAcc = makeAcc("WALLET:PCC:CUST", AccountType.USER_CARBON_WALLET, "PCC", BigDecimal.ZERO);
        LedgerAccount drvPccAcc = makeAcc("WALLET:PCC:DRV", AccountType.USER_CARBON_WALLET, "PCC", BigDecimal.ZERO);
        LedgerAccount custPtsAcc = makeAcc("WALLET:PTS:CUST", AccountType.USER_POINTS_WALLET, "ECO_POINT", BigDecimal.ZERO);
        LedgerAccount drvPtsAcc = makeAcc("WALLET:PTS:DRV", AccountType.USER_POINTS_WALLET, "ECO_POINT", BigDecimal.ZERO);

        when(ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE)).thenReturn(sysCarbon);
        when(ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_LOYALTY_RESERVE)).thenReturn(sysLoyalty);
        when(ledgerModuleApi.getOrCreateCarbonWallet(customerId)).thenReturn(custPccAcc);
        when(ledgerModuleApi.getOrCreateCarbonWallet(driverId)).thenReturn(drvPccAcc);
        when(ledgerModuleApi.getOrCreatePointsWallet(customerId)).thenReturn(custPtsAcc);
        when(ledgerModuleApi.getOrCreatePointsWallet(driverId)).thenReturn(drvPtsAcc);
        when(ledgerModuleApi.post(any())).thenAnswer(inv -> {
            LedgerTransaction tx = new LedgerTransaction();
            tx.setId(UUID.randomUUID());
            return tx;
        });

        TripIncentiveAward result1 = awardService.onCarbonCalculated(tripId, customerId, driverId, new BigDecimal("333.33"));
        assertEquals(new BigDecimal("0.3333"), result1.getTotalPcc());
        assertEquals(new BigDecimal("0.2333"), result1.getCustomerPcc());
        assertEquals(new BigDecimal("0.1000"), result1.getDriverPcc());
        assertEquals(0, result1.getCustomerPcc().add(result1.getDriverPcc()).compareTo(result1.getTotalPcc()));
        assertEquals(3, result1.getCustomerPoints()); // floor(333.33 / 100) = 3
        assertEquals(1, result1.getDriverPoints());   // max(1, floor(3 * 0.5)) = 1

        // Verify fixed idempotency keys INC:{tripId}:PCC and INC:{tripId}:PTS
        ArgumentCaptor<PostingRequest> postingCaptor = ArgumentCaptor.forClass(PostingRequest.class);
        verify(ledgerModuleApi, times(2)).post(postingCaptor.capture());
        List<PostingRequest> posts = postingCaptor.getAllValues();
        assertEquals("INC:" + tripId + ":PCC", posts.get(0).idempotencyKey());
        assertEquals("INC:" + tripId + ":PTS", posts.get(1).idempotencyKey());

        // Case 2: 0.05g -> T = 0.0000 PCC -> skip PCC ledger posting, award min 1 cust point & 1 driver point
        UUID trip2 = UUID.randomUUID();
        TripIncentiveAward award2 = new TripIncentiveAward();
        award2.setId(UUID.randomUUID());
        award2.setTripId(trip2);
        award2.setCustomerId(customerId);
        award2.setDriverUserId(driverId);
        award2.setStatus("PENDING_CONDITIONS");
        award2.setPaymentReady(true);

        reset(ledgerModuleApi);
        when(awardRepository.findByTripIdWithLock(trip2)).thenReturn(Optional.of(award2));
        when(tripModuleApi.getTrip(trip2)).thenReturn(Optional.empty());
        when(fraudModuleApi.hasBlockingFraudAlert(trip2)).thenReturn(false);
        when(ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_LOYALTY_RESERVE)).thenReturn(sysLoyalty);
        when(ledgerModuleApi.getOrCreatePointsWallet(customerId)).thenReturn(custPtsAcc);
        when(ledgerModuleApi.getOrCreatePointsWallet(driverId)).thenReturn(drvPtsAcc);
        when(ledgerModuleApi.post(any())).thenAnswer(inv -> {
            LedgerTransaction tx = new LedgerTransaction();
            tx.setId(UUID.randomUUID());
            return tx;
        });

        TripIncentiveAward result2 = awardService.onCarbonCalculated(trip2, customerId, driverId, new BigDecimal("0.05"));
        assertEquals(0, BigDecimal.ZERO.compareTo(result2.getTotalPcc()));
        assertEquals(1, result2.getCustomerPoints());
        assertEquals(1, result2.getDriverPoints());
        verify(ledgerModuleApi, times(1)).post(any()); // Only PTS tx posted; PCC = 0 skipped
    }

    @Test
    @DisplayName("AC-13: Voucher PERCENT/FIXED_VND, giới hạn 100%, và vòng đời hết hạn khi đang RESERVED")
    void testVoucherPreviewAndExpiryWhileReserved() {
        UUID custId = UUID.randomUUID();
        UUID rewardId = UUID.randomUUID();
        UUID redemptionId = UUID.randomUUID();

        Reward pctReward = new Reward();
        pctReward.setId(rewardId);
        pctReward.setTitle("Giảm 30%");
        pctReward.setCategory("VOUCHER_RIDE");
        pctReward.setDiscountType("PERCENT");
        pctReward.setDiscountValue(30L);
        pctReward.setMaxDiscountVnd(50000L);
        pctReward.setMinFareVnd(40000L);

        RewardRedemption rd = new RewardRedemption();
        rd.setId(redemptionId);
        rd.setRewardId(rewardId);
        rd.setUserId(custId);
        rd.setRedemptionCode("GM-TEST1234");
        rd.setStatus("AVAILABLE");
        rd.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(redemptionRepository.findById(redemptionId)).thenReturn(Optional.of(rd));
        when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(pctReward));

        // 100,000 * 30% = 30,000 (< max 50k)
        IncentiveModuleApi.VoucherPreviewDto preview1 = ecoStoreService.previewVoucher(
                custId, redemptionId, null, BigDecimal.valueOf(100000));
        assertEquals(BigDecimal.valueOf(30000), preview1.discountAmountVnd());
        assertEquals(BigDecimal.valueOf(70000), preview1.finalFareVnd());

        // 300,000 * 30% = 90,000 -> capped at 50,000
        IncentiveModuleApi.VoucherPreviewDto previewCap = ecoStoreService.previewVoucher(
                custId, redemptionId, null, BigDecimal.valueOf(300000));
        assertEquals(BigDecimal.valueOf(50000), previewCap.discountAmountVnd());
        assertEquals(BigDecimal.valueOf(250000), previewCap.finalFareVnd());

        // Min fare check (35,000 < 40,000)
        PaymentBusinessException minFareEx = assertThrows(PaymentBusinessException.class, () ->
                ecoStoreService.previewVoucher(custId, redemptionId, null, BigDecimal.valueOf(35000)));
        assertEquals("VOUCHER_MIN_FARE_NOT_MET", minFareEx.getErrorCode());

        // Reserved voucher whose expiresAt elapses mid-trip remains usable to completion -> USED
        UUID tripId = UUID.randomUUID();
        rd.setStatus("RESERVED");
        rd.setAppliedTripId(tripId);
        rd.setExpiresAt(Instant.now().minusSeconds(60));
        when(redemptionRepository.findReservedByAppliedTripIdWithLock(tripId)).thenReturn(Optional.of(rd));

        ecoStoreService.markVoucherUsedForTrip(tripId);
        assertEquals("USED", rd.getStatus());
        assertNotNull(rd.getUsedAt());

        // Reserved voucher whose expiresAt elapsed and trip cancelled -> transitions to EXPIRED
        rd.setStatus("RESERVED");
        rd.setUsedAt(null);
        ecoStoreService.releaseVoucherForCancelledTrip(tripId);
        assertEquals("EXPIRED", rd.getStatus());
        assertNull(rd.getAppliedTripId());
    }

    @Test
    @DisplayName("AC-09: Refund 1 phần không clawback; Full refund khi đã tiêu PCC ghi CLAWBACK_SHORTFALL_AUDIT & không trừ VND")
    void testRefundPartialAndShortfallAuditEdgeCases() {
        UUID tripId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID awardId = UUID.randomUUID();

        TripIncentiveAward award = new TripIncentiveAward();
        award.setId(awardId);
        award.setTripId(tripId);
        award.setCustomerId(customerId);
        award.setStatus("CREDITED");
        award.setTotalPcc(new BigDecimal("1.0000"));
        award.setCustomerPcc(new BigDecimal("1.0000"));
        award.setDriverPcc(BigDecimal.ZERO);
        award.setCustomerPoints(10);
        award.setDriverPoints(0);

        when(awardRepository.findByTripIdWithLock(tripId)).thenReturn(Optional.of(award));

        // 1. Partial refund -> status remains CREDITED, no ledger calls, audit recorded
        awardService.onPaymentRefunded(tripId, false, "Hoan phi 1 phan su co nho");
        assertEquals("CREDITED", award.getStatus());
        verify(ledgerModuleApi, never()).post(any());
        verify(auditLogService).record(isNull(), eq("SYSTEM"), eq("PARTIAL_REFUND_INCENTIVE_UNCHANGED"),
                eq("TripIncentiveAward"), eq(awardId.toString()), anyMap(), anyMap(), anyString());

        // 2. Full refund after customer converted PCC to VND (balance 0.1000 < 1.0000) -> shortfall audit, never auto-debit VND
        LedgerAccount custPcc = makeAcc("WALLET:PCC:CUST", AccountType.USER_CARBON_WALLET, "PCC", new BigDecimal("0.1000"));
        LedgerAccount custPts = makeAcc("WALLET:PTS:CUST", AccountType.USER_POINTS_WALLET, "ECO_POINT", BigDecimal.valueOf(10));
        when(ledgerModuleApi.getOrCreateCarbonWallet(customerId)).thenReturn(custPcc);
        when(ledgerModuleApi.getOrCreatePointsWallet(customerId)).thenReturn(custPts);
        when(accountRepository.findByIdWithLock(custPcc.getId())).thenReturn(Optional.of(custPcc));
        when(accountRepository.findByIdWithLock(custPts.getId())).thenReturn(Optional.of(custPts));

        awardService.onPaymentRefunded(tripId, true, "Hoan tien toan phan do tai xe huy sai");
        assertEquals("CLAWBACK_SHORTFALL_AUDIT", award.getStatus());
        verify(ledgerModuleApi, never()).getOrCreateUserWallet(any());
        verify(ledgerModuleApi, never()).post(any());
        verify(auditLogService).record(isNull(), eq("SYSTEM"), eq("CLAWBACK_SHORTFALL_AUDIT"),
                eq("TripIncentiveAward"), eq(awardId.toString()), anyMap(), anyMap(), anyString());
    }

    @Test
    @DisplayName("AC-19: OutboxPublisher chờ broker confirm ACK mới đánh dấu SENT; NACK/Return đánh dấu FAILED ở lần 10")
    void testOutboxPublisherConfirmNackAndReturned() {
        OutboxEventRepository outboxRepo = mock(OutboxEventRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OutboxPublisher publisher = new OutboxPublisher(outboxRepo, rabbitTemplate, new ObjectMapper());

        // 1. Broker ACK & not returned -> SENT
        OutboxEvent ackEvent = new OutboxEvent();
        ackEvent.setId(UUID.randomUUID());
        ackEvent.setRoutingKey("payment.event.succeeded");
        ackEvent.setPayload("{\"tripId\":\"123\"}");
        ackEvent.setStatus("NEW");
        ackEvent.setAttempts(0);

        when(outboxRepo.findPendingEventsForPublishing(any(), eq(50))).thenReturn(List.of(ackEvent));
        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        publisher.publishPendingEvents();
        assertEquals("SENT", ackEvent.getStatus());
        assertNotNull(ackEvent.getSentAt());

        // 2. Broker NACK on 10th attempt -> FAILED
        OutboxEvent nackEvent = new OutboxEvent();
        nackEvent.setId(UUID.randomUUID());
        nackEvent.setRoutingKey("payment.event.succeeded");
        nackEvent.setPayload("{\"tripId\":\"456\"}");
        nackEvent.setStatus("NEW");
        nackEvent.setAttempts(9);

        when(outboxRepo.findPendingEventsForPublishing(any(), eq(50))).thenReturn(List.of(nackEvent));
        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.getFuture().complete(new CorrelationData.Confirm(false, "Broker NACK test"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        publisher.publishPendingEvents();
        assertEquals("FAILED", nackEvent.getStatus());
        assertEquals(10, nackEvent.getAttempts());

        // 3. Broker ACK but Returned unroutable -> NOT marked SENT
        OutboxEvent returnedEvent = new OutboxEvent();
        returnedEvent.setId(UUID.randomUUID());
        returnedEvent.setRoutingKey("unroutable.key");
        returnedEvent.setPayload("{\"tripId\":\"789\"}");
        returnedEvent.setStatus("NEW");
        returnedEvent.setAttempts(0);

        when(outboxRepo.findPendingEventsForPublishing(any(), eq(50))).thenReturn(List.of(returnedEvent));
        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(3);
            cd.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", "ex", "unroutable.key"));
            cd.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        publisher.publishPendingEvents();
        assertEquals("NEW", returnedEvent.getStatus());
        assertEquals(1, returnedEvent.getAttempts());
        assertNull(returnedEvent.getSentAt());
    }

    @Test
    @DisplayName("AC-01: CarbonEstimateService chuẩn lưới điện 2024 CV 11232/BNNMT-BĐKH (618.70 gCO2/kWh)")
    void test2024EmissionFactorsConsistency() {
        CarbonEstimateService estimateService = new CarbonEstimateService();
        ReflectionTestUtils.setField(estimateService, "gridEmissionFactorGco2Kwh", 618.7);
        ReflectionTestUtils.setField(estimateService, "treeAbsorptionDailyKg", 0.06);
        ReflectionTestUtils.setField(estimateService, "ledBulbHourlyGco2", 6.187);

        CarbonEstimateDto bike = estimateService.estimateCarbon(VehicleType.ELECTRIC_MOTORBIKE, 10.0);
        assertEquals(0, new BigDecimal("487.00").compareTo(bike.getCo2SavedGrams())); // 48.70 * 10

        CarbonEstimateDto car4 = estimateService.estimateCarbon(VehicleType.ELECTRIC_CAR_4SEAT, 10.0);
        assertEquals(0, new BigDecimal("684.80").compareTo(car4.getCo2SavedGrams())); // 68.48 * 10

        CarbonEstimateDto car7 = estimateService.estimateCarbon(VehicleType.ELECTRIC_CAR_7SEAT, 10.0);
        assertEquals(0, new BigDecimal("986.30").compareTo(car7.getCo2SavedGrams())); // 98.63 * 10
    }

    private LedgerAccount makeAcc(String num, AccountType type, String ccy, BigDecimal balance) {
        LedgerAccount acc = new LedgerAccount();
        acc.setId(UUID.randomUUID());
        acc.setAccountNumber(num);
        acc.setAccountType(type);
        acc.setCurrency(ccy);
        acc.setBalance(balance);
        acc.setStatus("ACTIVE");
        return acc;
    }
}

package com.greenmobility.incentive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.common.platform.domain.OutboxEvent;
import com.greenmobility.common.platform.job.OutboxPublisher;
import com.greenmobility.common.platform.repository.AuditLogRepository;
import com.greenmobility.common.platform.repository.OutboxEventRepository;
import com.greenmobility.common.security.JwtTokenProvider;
import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import com.greenmobility.modules.carbon.event.CarbonCalculatedEvent;
import com.greenmobility.modules.carbon.repository.TripImpactReceiptRepository;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.fraud.entity.FraudAlert;
import com.greenmobility.modules.fraud.repository.FraudAlertRepository;
import com.greenmobility.modules.identity.entity.Role;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.entity.UserStatus;
import com.greenmobility.modules.identity.repository.UserRepository;
import com.greenmobility.modules.incentive.domain.Reward;
import com.greenmobility.modules.incentive.domain.RewardRedemption;
import com.greenmobility.modules.incentive.domain.TripIncentiveAward;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.job.IncentiveReconciliationJob;
import com.greenmobility.modules.incentive.repository.RewardRedemptionRepository;
import com.greenmobility.modules.incentive.repository.RewardRepository;
import com.greenmobility.modules.incentive.repository.TripIncentiveAwardRepository;
import com.greenmobility.modules.incentive.service.EcoStoreService;
import com.greenmobility.modules.incentive.service.IncentiveAwardService;
import com.greenmobility.modules.incentive.service.IncentiveWalletService;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.EntryType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import com.greenmobility.modules.ledger.domain.LedgerTxType;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.dto.AdminFinanceDtos;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.payment.service.DriverEarningsService;
import com.greenmobility.modules.payment.service.PaymentService;
import com.greenmobility.modules.payment.service.RefundService;
import com.greenmobility.modules.trip.entity.PaymentMethod;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Sprint6IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("greenmobility_test")
            .withUsername("green_admin")
            .withPassword("green_password_2026");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.validate-on-migrate", () -> "true");
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("security.jwt.secret-key",
                () -> "GreenMobilitySprint6IntegrationTestJwtSecretKey2026UltraSecure32Bytes!!");
        registry.add("payment.gateway-mode", () -> "sandbox");
    }

    private static final UUID SEED_CUSTOMER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID SEED_DRIVER_USER_ID = UUID.fromString("9c12b7a8-1234-5678-9abc-def012345678");
    private static final UUID SEED_DRIVER_PROFILE_ID = UUID.fromString("88888888-1234-5678-9abc-def012345678");
    private static final UUID VOUCHER_20K_REWARD_ID = UUID.fromString("f1000001-1111-4444-8888-000000000001");
    private static final UUID VOUCHER_15K_REWARD_ID = UUID.fromString("f1000003-1111-4444-8888-000000000003");
    private static final UUID TREE_REWARD_ID = UUID.fromString("f1000004-1111-4444-8888-000000000004");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private UserRepository userRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private FraudAlertRepository fraudAlertRepository;
    @Autowired private TripImpactReceiptRepository receiptRepository;
    @Autowired private RewardRepository rewardRepository;
    @Autowired private RewardRedemptionRepository redemptionRepository;
    @Autowired private TripIncentiveAwardRepository awardRepository;
    @Autowired private LedgerAccountRepository accountRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private LedgerModuleApi ledgerModuleApi;
    @Autowired private PaymentService paymentService;
    @Autowired private DriverEarningsService driverEarningsService;
    @Autowired private RefundService refundService;
    @Autowired private IncentiveAwardService awardService;
    @Autowired private IncentiveWalletService incentiveWalletService;
    @Autowired private EcoStoreService ecoStoreService;
    @Autowired private OutboxPublisher outboxPublisher;
    @Autowired private IncentiveReconciliationJob reconciliationJob;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    private User adminUser;
    private User financeUser;
    private User operatorUser;
    private User customerUserA;
    private User customerUserB;
    private User disabledUser;
    private String adminToken;
    private String financeToken;
    private String operatorToken;
    private String customerAToken;
    private String customerBToken;
    private String driverToken;

    @BeforeEach
    void initActors() {
        adminUser = ensureUser("0900990001", "admin.s6@gm.vn", "Admin Sprint6", Role.ROLE_ADMIN, UserStatus.ACTIVE);
        financeUser = ensureUser("0900990002", "finance.s6@gm.vn", "Finance Sprint6", Role.ROLE_FINANCE, UserStatus.ACTIVE);
        operatorUser = ensureUser("0900990003", "operator.s6@gm.vn", "Operator Sprint6", Role.ROLE_OPERATOR, UserStatus.ACTIVE);
        customerUserA = userRepository.findById(SEED_CUSTOMER_ID).orElseThrow();
        customerUserB = ensureUser("0900990004", "custb.s6@gm.vn", "Customer B Sprint6", Role.ROLE_CUSTOMER, UserStatus.ACTIVE);
        disabledUser = ensureUser("0900990005", "disabled.s6@gm.vn", "Disabled User", Role.ROLE_CUSTOMER, UserStatus.SUSPENDED);

        ledgerModuleApi.provisionUserWallets(customerUserA.getId(), false);
        ledgerModuleApi.provisionUserWallets(customerUserB.getId(), false);
        ledgerModuleApi.provisionUserWallets(SEED_DRIVER_USER_ID, true);

        adminToken = jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getPhoneNumber(), adminUser.getRole().name());
        financeToken = jwtTokenProvider.generateToken(financeUser.getId(), financeUser.getPhoneNumber(), financeUser.getRole().name());
        operatorToken = jwtTokenProvider.generateToken(operatorUser.getId(), operatorUser.getPhoneNumber(), operatorUser.getRole().name());
        customerAToken = jwtTokenProvider.generateToken(customerUserA.getId(), customerUserA.getPhoneNumber(), customerUserA.getRole().name());
        customerBToken = jwtTokenProvider.generateToken(customerUserB.getId(), customerUserB.getPhoneNumber(), customerUserB.getRole().name());
        driverToken = jwtTokenProvider.generateToken(SEED_DRIVER_USER_ID, "0987654321", "ROLE_DRIVER");
    }

    @Test
    @Order(1)
    @DisplayName("AC-01: Flyway V1-V6 thành công, hệ số phát thải 2024 CV 11232/BNNMT-BĐKH & DB CHECK constraint")
    void ac01_emissionFactors2024AndCheckConstraint() {
        List<Map<String, Object>> activeFactors = jdbcTemplate.queryForList(
                "SELECT vehicle_category, baseline_gasoline_factor_gco2_km, ev_energy_consumption_kwh_km, " +
                        "grid_emission_factor_gco2_kwh, calculated_ev_factor_gco2_km, net_co2_saving_per_km " +
                        "FROM emission_factors WHERE is_active = TRUE ORDER BY vehicle_category");

        assertEquals(3, activeFactors.size());
        Map<String, BigDecimal> expectedNet = Map.of(
                "ELECTRIC_MOTORBIKE", new BigDecimal("48.70"),
                "ELECTRIC_CAR_4SEAT", new BigDecimal("68.48"),
                "ELECTRIC_CAR_7SEAT", new BigDecimal("98.63")
        );
        for (Map<String, Object> row : activeFactors) {
            String cat = (String) row.get("vehicle_category");
            assertEquals(0, new BigDecimal("618.70").compareTo((BigDecimal) row.get("grid_emission_factor_gco2_kwh")));
            assertEquals(0, expectedNet.get(cat).compareTo((BigDecimal) row.get("net_co2_saving_per_km")));
        }

        // Violate CHECK constraint (incorrect net_co2_saving_per_km != baseline - ev)
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "INSERT INTO emission_factors (id, vehicle_category, baseline_gasoline_factor_gco2_km, " +
                        "ev_energy_consumption_kwh_km, grid_emission_factor_gco2_kwh, calculated_ev_factor_gco2_km, " +
                        "net_co2_saving_per_km, region, effective_from, is_active) " +
                        "VALUES (gen_random_uuid(), 'ELECTRIC_MOTORBIKE', 68.50, 0.0320, 618.70, 19.80, 99.99, 'TEST', CURRENT_DATE, FALSE)"
        ));
    }

    @Test
    @Order(2)
    @DisplayName("AC-02 & AC-03: Sổ cái đa tiền tệ tách biệt (Trigger + Service) & Bất biến sổ cái DB (chặn UPDATE/DELETE)")
    void ac02_ac03_multiCurrencyIsolationAndImmutability() {
        LedgerAccount custVnd = ledgerModuleApi.getOrCreateUserWallet(customerUserA.getId());
        LedgerAccount sysCarbonPcc = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE);

        // Service rejects mixed currency
        PaymentBusinessException mixedEx = assertThrows(PaymentBusinessException.class, () ->
                ledgerModuleApi.post(new PostingRequest(
                        null, null, LedgerTxType.TOPUP, "Mixed currency",
                        "MIXED-" + UUID.randomUUID(), "TEST", null, null,
                        List.of(
                                new PostingEntry(sysCarbonPcc.getId(), EntryType.DEBIT, BigDecimal.TEN),
                                new PostingEntry(custVnd.getId(), EntryType.CREDIT, BigDecimal.TEN)
                        )
                )));
        assertEquals("LEDGER_INVALID_CURRENCY", mixedEx.getErrorCode());

        // Service rejects unbalanced transaction
        LedgerAccount custPcc = ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId());
        PaymentBusinessException unbalEx = assertThrows(PaymentBusinessException.class, () ->
                ledgerModuleApi.post(new PostingRequest(
                        null, null, LedgerTxType.TRIP_CARBON_REWARD, "Unbalanced",
                        "UNBAL-" + UUID.randomUUID(), "TEST", null, null,
                        List.of(
                                new PostingEntry(sysCarbonPcc.getId(), EntryType.DEBIT, BigDecimal.ONE),
                                new PostingEntry(custPcc.getId(), EntryType.CREDIT, BigDecimal.TEN)
                        )
                )));
        assertEquals("LEDGER_UNBALANCED", unbalEx.getErrorCode());

        // Valid transaction posted -> DB triggers reject UPDATE & DELETE on ledger_transactions and ledger_entries
        LedgerTransaction tx = ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.TRIP_CARBON_REWARD, "Test immutable",
                "IMMUT-" + UUID.randomUUID(), "TEST", null, null,
                List.of(
                        new PostingEntry(sysCarbonPcc.getId(), EntryType.DEBIT, new BigDecimal("0.5000")),
                        new PostingEntry(custPcc.getId(), EntryType.CREDIT, new BigDecimal("0.5000"))
                )
        ));

        assertThrows(Exception.class, () ->
                jdbcTemplate.update("UPDATE ledger_transactions SET description = 'HACKED' WHERE id = ?", tx.getId()));
        assertThrows(Exception.class, () ->
                jdbcTemplate.update("DELETE FROM ledger_entries WHERE transaction_id = ?", tx.getId()));

        // Also DB trigger trg_check_tx_single_currency rejects cross-currency SQL insert bypassing service
        UUID rawTxId = UUID.randomUUID();
        assertThrows(Exception.class, () -> {
            jdbcTemplate.update("INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description) " +
                    "VALUES (?, ?, 'TRIP_CARBON_REWARD', 'raw mixed')", rawTxId, "RAW-" + rawTxId);
            jdbcTemplate.update("INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after) " +
                    "VALUES (gen_random_uuid(), ?, ?, 'DEBIT', 1.0000, 0), (gen_random_uuid(), ?, ?, 'CREDIT', 1.0000, 0)",
                    rawTxId, sysCarbonPcc.getId(), rawTxId, custVnd.getId());
        });
    }

    @Test
    @Order(3)
    @DisplayName("AC-04, AC-05 & AC-06: Join-Barrier Carbon-first & Payment-first, chia PCC 70/30, EcoPoints & Idempotent")
    void ac04_ac05_ac06_joinBarrierBothOrdersAndIdempotent() {
        BigDecimal custPccBefore = ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId()).getBalance();
        BigDecimal drvPccBefore = ledgerModuleApi.getOrCreateCarbonWallet(SEED_DRIVER_USER_ID).getBalance();

        // Order A (AC-04): Carbon first -> PENDING_CONDITIONS, then Payment -> CREDITED
        Trip tripA = createCompletedTrip(customerUserA.getId(), SEED_DRIVER_PROFILE_ID, 50000L, 0L, PaymentMethod.GREEN_WALLET, PaymentStatus.PAID);
        TripIncentiveAward stepA1 = awardService.onCarbonCalculated(tripA.getId(), customerUserA.getId(), SEED_DRIVER_USER_ID, new BigDecimal("1000.00"));
        assertEquals("PENDING_CONDITIONS", stepA1.getStatus());
        assertTrue(stepA1.isCarbonReady());
        assertFalse(stepA1.isPaymentReady());

        TripIncentiveAward stepA2 = awardService.onPaymentSucceeded(tripA.getId());
        assertEquals("CREDITED", stepA2.getStatus());
        assertEquals(0, new BigDecimal("1.0000").compareTo(stepA2.getTotalPcc()));
        assertEquals(0, new BigDecimal("0.7000").compareTo(stepA2.getCustomerPcc()));
        assertEquals(0, new BigDecimal("0.3000").compareTo(stepA2.getDriverPcc()));
        assertEquals(10, stepA2.getCustomerPoints());
        assertEquals(5, stepA2.getDriverPoints());

        // Order B (AC-05): Payment first -> PENDING_CONDITIONS, then Carbon -> CREDITED, repeated call idempotent
        Trip tripB = createCompletedTrip(customerUserA.getId(), SEED_DRIVER_PROFILE_ID, 50000L, 0L, PaymentMethod.GREEN_WALLET, PaymentStatus.PAID);
        TripIncentiveAward stepB1 = awardService.onPaymentSucceeded(tripB.getId());
        assertEquals("PENDING_CONDITIONS", stepB1.getStatus());
        assertTrue(stepB1.isPaymentReady());
        assertFalse(stepB1.isCarbonReady());

        TripIncentiveAward stepB2 = awardService.onCarbonCalculated(tripB.getId(), customerUserA.getId(), SEED_DRIVER_USER_ID, new BigDecimal("500.00"));
        assertEquals("CREDITED", stepB2.getStatus());
        // Duplicate replay must not double-credit
        TripIncentiveAward replayB = awardService.onPaymentSucceeded(tripB.getId());
        assertEquals("CREDITED", replayB.getStatus());
        assertEquals(stepB2.getPccLedgerTxId(), replayB.getPccLedgerTxId());

        BigDecimal custPccAfter = ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId()).getBalance();
        BigDecimal drvPccAfter = ledgerModuleApi.getOrCreateCarbonWallet(SEED_DRIVER_USER_ID).getBalance();
        assertEquals(0, custPccBefore.add(new BigDecimal("1.0500")).compareTo(custPccAfter));
        assertEquals(0, drvPccBefore.add(new BigDecimal("0.4500")).compareTo(drvPccAfter));
    }

    @Test
    @Order(4)
    @DisplayName("AC-07, AC-08 & AC-10: Held for Fraud -> Admin Approve / Void, và Fraud đến muộn qua ADMIN_REVERSAL")
    void ac07_ac08_ac10_fraudHoldApproveVoidAndLateFraudReversal() throws Exception {
        // 1. Trip with PENDING fraud alert -> HELD_FOR_FRAUD, then Admin approves -> CREDITED
        Trip fraudTrip1 = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 40000L, 0L, PaymentMethod.CASH, PaymentStatus.PAID);
        fraudAlertRepository.save(new FraudAlert(fraudTrip1.getId(), SEED_DRIVER_PROFILE_ID, customerUserB.getId(),
                "GPS_SPOOFING", new BigDecimal("92.50"), "{\"reason\":\"Mock velocity jump\"}"));

        awardService.onPaymentSucceeded(fraudTrip1.getId());
        TripIncentiveAward held1 = awardService.onCarbonCalculated(fraudTrip1.getId(), customerUserB.getId(), SEED_DRIVER_USER_ID, new BigDecimal("1000.00"));
        assertEquals("HELD_FOR_FRAUD", held1.getStatus());

        mockMvc.perform(post("/admin/incentive/awards/{id}/approve", held1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Da xac minh hanh trinh hop le qua telemetry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREDITED"));

        // 2. Second trip held for fraud -> Admin voids -> VOIDED_FRAUD
        Trip fraudTrip2 = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 40000L, 0L, PaymentMethod.CASH, PaymentStatus.PAID);
        fraudAlertRepository.save(new FraudAlert(fraudTrip2.getId(), SEED_DRIVER_PROFILE_ID, customerUserB.getId(),
                "GPS_SPOOFING", new BigDecimal("98.00"), "{\"reason\":\"Confirmed spoof\"}"));

        awardService.onPaymentSucceeded(fraudTrip2.getId());
        TripIncentiveAward held2 = awardService.onCarbonCalculated(fraudTrip2.getId(), customerUserB.getId(), SEED_DRIVER_USER_ID, new BigDecimal("1000.00"));
        assertEquals("HELD_FOR_FRAUD", held2.getStatus());

        mockMvc.perform(post("/admin/incentive/awards/{id}/void", held2.getId())
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Gian lan toa do GPS xac nhan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED_FRAUD"));

        // 3. AC-10: Late fraud alert arriving after CREDITED -> Admin reverses via ADMIN_REVERSAL
        TripIncentiveAward approved = awardRepository.findById(held1.getId()).orElseThrow();
        fraudAlertRepository.save(new FraudAlert(fraudTrip1.getId(), SEED_DRIVER_PROFILE_ID, customerUserB.getId(),
                "LATE_REPORT", new BigDecimal("88.00"), "{\"reason\":\"Post-trip audit flag\"}"));
        assertEquals("CREDITED", awardRepository.findById(approved.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/admin/ledger/transactions/{id}/reverse", approved.getPccLedgerTxId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "REV-LATE-FRAUD-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thu hoi PCC do phat hien gian lan GPS muon\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("ADMIN_REVERSAL"));
    }

    @Test
    @Order(5)
    @DisplayName("AC-09: Full refund clawback tự động & CLAWBACK_SHORTFALL_AUDIT khi đã đổi PCC, Partial refund giữ nguyên")
    void ac09_fullRefundClawbackAndShortfall() {
        // Fund customerB with 200k VND for wallet trip payments
        topupVnd(customerUserB.getId(), 200000L);

        // 1. Full refund while user still has PCC & PTS -> INCENTIVE_CLAWBACK
        Trip tripFull = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 60000L, 0L, PaymentMethod.GREEN_WALLET, PaymentStatus.PENDING);
        paymentService.createCheckout(customerUserB.getId(), tripFull.getId(), "GREEN_WALLET", "127.0.0.1");
        awardService.onPaymentSucceeded(tripFull.getId());
        awardService.onCarbonCalculated(tripFull.getId(), customerUserB.getId(), SEED_DRIVER_USER_ID, new BigDecimal("1000.00"));
        assertEquals("CREDITED", awardRepository.findByTripId(tripFull.getId()).orElseThrow().getStatus());

        Payment payFull = paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(tripFull.getId()).orElseThrow();
        refundService.refund(adminUser.getId(), adminUser.getEmail(), payFull.getId(),
                new AdminFinanceDtos.RefundRequest(60000L, "Hoan tien toan phan su co chuyen di", true), "127.0.0.1");
        awardService.onPaymentRefunded(tripFull.getId(), true, "Hoan tien toan phan su co chuyen di");

        TripIncentiveAward clawed = awardRepository.findByTripId(tripFull.getId()).orElseThrow();
        assertEquals("CLAWED_BACK", clawed.getStatus());
        assertNotNull(clawed.getClawbackPccTxId());

        // 2. Partial refund -> CREDITED unchanged
        Trip tripPart = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 80000L, 0L, PaymentMethod.GREEN_WALLET, PaymentStatus.PENDING);
        paymentService.createCheckout(customerUserB.getId(), tripPart.getId(), "GREEN_WALLET", "127.0.0.1");
        awardService.onPaymentSucceeded(tripPart.getId());
        awardService.onCarbonCalculated(tripPart.getId(), customerUserB.getId(), SEED_DRIVER_USER_ID, new BigDecimal("1000.00"));

        Payment payPart = paymentRepository.findFirstByTripIdOrderByCreatedAtDesc(tripPart.getId()).orElseThrow();
        refundService.refund(adminUser.getId(), adminUser.getEmail(), payPart.getId(),
                new AdminFinanceDtos.RefundRequest(20000L, "Hoan 1 phan phi cau duong nham lan", true), "127.0.0.1");
        awardService.onPaymentRefunded(tripPart.getId(), false, "Hoan 1 phan phi cau duong");
        assertEquals("CREDITED", awardRepository.findByTripId(tripPart.getId()).orElseThrow().getStatus());

        // 3. Convert customerB's PCC -> VND, then full refund on tripPart's remaining amount -> CLAWBACK_SHORTFALL_AUDIT & NO VND debit
        BigDecimal availPcc = ledgerModuleApi.getOrCreateCarbonWallet(customerUserB.getId()).getBalance();
        if (availPcc.compareTo(new BigDecimal("0.5000")) >= 0) {
            incentiveWalletService.convertPccToVnd(customerUserB.getId(), "ROLE_CUSTOMER", availPcc, "CONV-SHORTFALL-" + UUID.randomUUID());
        }
        long vndBeforeShortfall = ledgerModuleApi.getOrCreateUserWallet(customerUserB.getId()).getBalance().longValue();

        refundService.refund(adminUser.getId(), adminUser.getEmail(), payPart.getId(),
                new AdminFinanceDtos.RefundRequest(60000L, "Hoan tien phan con lai chuyen di", true), "127.0.0.1");
        awardService.onPaymentRefunded(tripPart.getId(), true, "Hoan toan phan sau khi da doi PCC");

        assertEquals("CLAWBACK_SHORTFALL_AUDIT", awardRepository.findByTripId(tripPart.getId()).orElseThrow().getStatus());
        long vndAfterShortfall = ledgerModuleApi.getOrCreateUserWallet(customerUserB.getId()).getBalance().longValue();
        // VND only increased by the 60k refund, never auto-debited for PCC shortfall
        assertEquals(vndBeforeShortfall + 60000L, vndAfterShortfall);
    }

    @Test
    @Order(6)
    @DisplayName("AC-11 & AC-12: Đổi quà đồng thời 200 luồng (0 oversell, 0 âm ví) & Hủy đổi quà AVAILABLE")
    void ac11_ac12_concurrentEcoStoreRedemption200ThreadsAndCancel() throws Exception {
        // Create limited reward qty = 10, maxRedemptionsPerUser = 200, cost = 0.5000 PCC + 1 ECO_POINT
        Reward scarce = new Reward();
        scarce.setTitle("Voucher Hiếm Giới Hạn");
        scarce.setPartnerName("Green Mobility");
        scarce.setCategory("VOUCHER_RIDE");
        scarce.setCarbonCreditCost(new BigDecimal("0.5000"));
        scarce.setLoyaltyPointCost(1);
        scarce.setQuantityAvailable(10);
        scarce.setIsActive(true);
        scarce.setDiscountType("FIXED_VND");
        scarce.setDiscountValue(10000L);
        scarce.setMaxDiscountVnd(10000L);
        scarce.setMinFareVnd(0L);
        scarce.setValidityDays(30);
        scarce.setMaxRedemptionsPerUser(500);
        scarce = rewardRepository.save(scarce);

        // Give customerA plenty of PCC and ECO_POINT via balanced double-entry
        creditPcc(customerUserA.getId(), new BigDecimal("150.0000"));
        creditPoints(customerUserA.getId(), 500);

        UUID rewardId = scarce.getId();
        int threads = 200;
        ExecutorService pool = Executors.newFixedThreadPool(24);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger otherFailCount = new AtomicInteger(0);
        List<UUID> redeemedIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            final String key = "CONC-REDEEM-" + i + "-" + UUID.randomUUID();
            pool.submit(() -> {
                try {
                    startGate.await();
                    IncentiveDtos.RedemptionResponseDto res = ecoStoreService.redeemReward(customerUserA.getId(), rewardId, key);
                    successCount.incrementAndGet();
                    redeemedIds.add(res.id());
                } catch (PaymentBusinessException ex) {
                    if ("REWARD_OUT_OF_STOCK".equals(ex.getErrorCode())) {
                        outOfStockCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    otherFailCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneGate.await(45, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(10, successCount.get(), "Exactly 10 stock must succeed across 200 threads");
        assertEquals(190, outOfStockCount.get());
        assertEquals(0, otherFailCount.get());
        assertEquals(0, rewardRepository.findById(rewardId).orElseThrow().getQuantityAvailable());
        assertTrue(ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId()).getBalance().compareTo(BigDecimal.ZERO) >= 0);

        // AC-12: Cancel 1 AVAILABLE redemption -> qty returns to 1 & PCC/PTS refunded
        BigDecimal pccBeforeCancel = ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId()).getBalance();
        mockMvc.perform(post("/incentive/redemptions/{id}/cancel", redeemedIds.get(0))
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Doi y khong dung voucher nua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertEquals(1, rewardRepository.findById(rewardId).orElseThrow().getQuantityAvailable());
        BigDecimal pccAfterCancel = ledgerModuleApi.getOrCreateCarbonWallet(customerUserA.getId()).getBalance();
        assertEquals(0, pccBeforeCancel.add(new BigDecimal("0.5000")).compareTo(pccAfterCancel));
    }

    @Test
    @Order(7)
    @DisplayName("AC-13 & AC-14: Voucher Trip lifecycle (Reserve/Cancel/Used) + Trợ giá SYS:PROMO_EXPENSE:VND (CASH & 100%)")
    void ac13_ac14_voucherTripLifecycleAndPromoSubsidyCashAnd100Pct() {
        // 1. Test Voucher RESERVED -> cancelled trip returns AVAILABLE -> re-reserved & completed even past expiry -> USED
        creditPoints(customerUserA.getId(), 100);
        creditPcc(customerUserA.getId(), new BigDecimal("5.0000"));
        IncentiveDtos.RedemptionResponseDto voucher15k = ecoStoreService.redeemReward(
                customerUserA.getId(), VOUCHER_15K_REWARD_ID, "V15K-" + UUID.randomUUID());

        UUID dummyCancelTripId = UUID.randomUUID();
        Trip cancelTrip = createCompletedTrip(customerUserA.getId(), SEED_DRIVER_PROFILE_ID, 30000L, 15000L, PaymentMethod.CASH, PaymentStatus.PENDING);
        var reserved = ecoStoreService.reserveVoucherForTrip(customerUserA.getId(), cancelTrip.getId(), voucher15k.id(), null, BigDecimal.valueOf(30000));
        assertEquals(BigDecimal.valueOf(15000), reserved.discountAmountVnd());
        assertEquals("RESERVED", redemptionRepository.findById(voucher15k.id()).orElseThrow().getStatus());

        ecoStoreService.releaseVoucherForCancelledTrip(cancelTrip.getId());
        assertEquals("AVAILABLE", redemptionRepository.findById(voucher15k.id()).orElseThrow().getStatus());

        // AC-14 Case A: 100% discount trip (gross = 15,000, discount = 15,000, final = 0)
        Trip zeroTrip = createCompletedTrip(customerUserA.getId(), SEED_DRIVER_PROFILE_ID, 15000L, 15000L, PaymentMethod.VNPAY, PaymentStatus.PENDING);
        ecoStoreService.reserveVoucherForTrip(customerUserA.getId(), zeroTrip.getId(), voucher15k.id(), null, BigDecimal.valueOf(15000));
        // Simulate expiry elapsed during active trip -> markVoucherUsedForTrip still marks USED
        RewardRedemption rdEnt = redemptionRepository.findById(voucher15k.id()).orElseThrow();
        rdEnt.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        redemptionRepository.save(rdEnt);

        var checkoutZero = paymentService.createCheckout(customerUserA.getId(), zeroTrip.getId(), "VNPAY", "127.0.0.1");
        assertEquals("PAID", checkoutZero.status());
        assertEquals(0L, checkoutZero.amountVnd());

        ecoStoreService.markVoucherUsedForTrip(zeroTrip.getId());
        assertEquals("USED", redemptionRepository.findById(voucher15k.id()).orElseThrow().getStatus());

        // Verify payment.event.succeeded outbox was emitted for zero-fare 100% discount
        List<OutboxEvent> zeroOutbox = outboxEventRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(checkoutZero.paymentId().toString())
                        && "payment.event.succeeded".equals(e.getRoutingKey()))
                .toList();
        assertFalse(zeroOutbox.isEmpty(), "Zero-fare 100% discount must emit payment.event.succeeded");

        // AC-14 Case B: CASH trip with 20k voucher discount on 50k gross fare
        long drvEarningsBefore = ledgerModuleApi.getOrCreateDriverEarningsAccount(SEED_DRIVER_USER_ID).getBalance().longValue();
        Trip cashVoucherTrip = createCompletedTrip(customerUserA.getId(), SEED_DRIVER_PROFILE_ID, 50000L, 20000L, PaymentMethod.CASH, PaymentStatus.PENDING);
        paymentService.initiatePaymentForTrip(new TripCompletedEvent(
                cashVoucherTrip.getId(), cashVoucherTrip.getTripCode(), customerUserA.getId(), SEED_DRIVER_PROFILE_ID,
                null, VehicleType.ELECTRIC_MOTORBIKE.name(), 5000, 5000, 600, 600,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(30000), PaymentMethod.CASH.name(),
                BigDecimal.valueOf(243.50), Instant.now().minusSeconds(600), Instant.now()
        ));
        driverEarningsService.confirmCashCollected(SEED_DRIVER_USER_ID, cashVoucherTrip.getId());

        // Driver net effect: -10,000 commission (20% of 50,000 gross) + 20,000 VOUCHER_SUBSIDY = +10,000 net!
        long drvEarningsAfter = ledgerModuleApi.getOrCreateDriverEarningsAccount(SEED_DRIVER_USER_ID).getBalance().longValue();
        assertEquals(drvEarningsBefore + 10000L, drvEarningsAfter);
    }

    @Test
    @Order(8)
    @DisplayName("AC-15: Quy đổi PCC -> VND cho Customer & Driver + Chặn vượt trần tháng khi chạy đồng thời")
    void ac15_pccToVndConversionAndMonthlyCapConcurrency() throws Exception {
        // 1. Driver converts 2.0000 PCC -> credits DRIVER_EARNINGS (1,000 VND at 500/PCC)
        creditPcc(SEED_DRIVER_USER_ID, new BigDecimal("5.0000"));
        long drvVndBefore = ledgerModuleApi.getOrCreateDriverEarningsAccount(SEED_DRIVER_USER_ID).getBalance().longValue();

        mockMvc.perform(post("/incentive/carbon/convert")
                        .header("Authorization", "Bearer " + driverToken)
                        .header("Idempotency-Key", "CONV-DRV-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pccAmount\": 2.0000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetAccountType").value("DRIVER_EARNINGS"))
                .andExpect(jsonPath("$.data.vndCredited").value(1000));

        long drvVndAfter = ledgerModuleApi.getOrCreateDriverEarningsAccount(SEED_DRIVER_USER_ID).getBalance().longValue();
        assertEquals(drvVndBefore + 1000L, drvVndAfter);

        // 2. Customer concurrent conversions testing 100.0000 PCC monthly cap: give 250 PCC, run 3 concurrent 45.0000 PCC requests
        creditPcc(customerUserA.getId(), new BigDecimal("250.0000"));
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final String key = "CONC-CAP-" + i + "-" + UUID.randomUUID();
            futures.add(pool.submit(() -> {
                latch.await();
                try {
                    incentiveWalletService.convertPccToVnd(customerUserA.getId(), "ROLE_CUSTOMER", new BigDecimal("45.0000"), key);
                    return "OK";
                } catch (PaymentBusinessException ex) {
                    return ex.getErrorCode();
                }
            }));
        }
        latch.countDown();
        int ok = 0, capExceeded = 0;
        for (Future<String> f : futures) {
            String res = f.get(15, TimeUnit.SECONDS);
            if ("OK".equals(res)) ok++;
            else if ("PCC_MONTHLY_CAP_EXCEEDED".equals(res)) capExceeded++;
        }
        pool.shutdown();
        // At most 2 of 45 PCC (= 90 PCC <= 100 PCC cap) can succeed; the 3rd (135 PCC > 100) MUST fail with cap exceeded
        assertEquals(2, ok);
        assertEquals(1, capExceeded);
    }

    @Test
    @Order(9)
    @DisplayName("AC-16, AC-17 & AC-18: Đóng băng ví (409 WALLET_FROZEN), Circuit-Breaker & ADMIN_REVERSAL")
    void ac16_ac17_ac18_freezeWalletCircuitBreakerAndReversal() throws Exception {
        creditPcc(customerUserB.getId(), new BigDecimal("5.0000"));
        creditPoints(customerUserB.getId(), 50);

        // AC-16: Freeze customerB wallet -> convert returns 409 WALLET_FROZEN, then unfreeze
        mockMvc.perform(post("/admin/ledger/users/{userId}/freeze", customerUserB.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nghi van gian lan tu giao dien Admin\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/incentive/carbon/convert")
                        .header("Authorization", "Bearer " + customerBToken)
                        .header("Idempotency-Key", "FRZ-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pccAmount\": 1.0000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WALLET_FROZEN"));

        mockMvc.perform(post("/admin/ledger/users/{userId}/unfreeze", customerUserB.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Da hoan tat xac minh an toan\"}"))
                .andExpect(status().isOk());

        // AC-17: Circuit breaker -> 409 REDEMPTION_FROZEN_CIRCUIT_BREAKER, then disable
        mockMvc.perform(post("/admin/ledger/circuit-breaker")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frozen\": true, \"reason\": \"Kiem toan dinh ky\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/incentive/redeem")
                        .header("Authorization", "Bearer " + customerBToken)
                        .header("Idempotency-Key", "CB-REDEEM-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rewardId\": \"" + VOUCHER_15K_REWARD_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REDEMPTION_FROZEN_CIRCUIT_BREAKER"));

        mockMvc.perform(post("/admin/ledger/circuit-breaker")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frozen\": false, \"reason\": \"Ket thuc kiem toan\"}"))
                .andExpect(status().isOk());

        // AC-18: Admin reversal -> short reason rejected, valid succeeds, duplicate reversal -> 409 LEDGER_ALREADY_REVERSED
        LedgerAccount custPcc = ledgerModuleApi.getOrCreateCarbonWallet(customerUserB.getId());
        LedgerAccount sysPcc = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE);
        LedgerTransaction tx = ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.TRIP_CARBON_REWARD, "Adjust for reversal test",
                "TEST-REV-" + UUID.randomUUID(), "TEST", null, null,
                List.of(
                        new PostingEntry(sysPcc.getId(), EntryType.DEBIT, new BigDecimal("1.0000")),
                        new PostingEntry(custPcc.getId(), EntryType.CREDIT, new BigDecimal("1.0000"))
                )
        ));

        mockMvc.perform(post("/admin/ledger/transactions/{id}/reverse", tx.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "REV-SHORT-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ngan\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/admin/ledger/transactions/{id}/reverse", tx.getId())
                        .header("Authorization", "Bearer " + financeToken)
                        .header("Idempotency-Key", "REV-OK-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Dao but toan do nhap nham gia tri thuong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("ADMIN_REVERSAL"));

        mockMvc.perform(post("/admin/ledger/transactions/{id}/reverse", tx.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "REV-DUP-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thu dao giao dich lan thu hai\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LEDGER_ALREADY_REVERSED"));
    }

    @Test
    @Order(10)
    @DisplayName("AC-19 & AC-20: Outbox RabbitMQ Broker Confirm + Admin Re-drive & IncentiveReconciliationJob")
    void ac19_ac20_outboxBrokerConfirmRedriveAndReconciliationJob() throws Exception {
        // AC-19A: Real RabbitMQ broker publish -> OutboxPublisher waits for confirm & marks SENT -> IncentiveEventConsumer credits
        Trip rabbitTrip = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 45000L, 0L, PaymentMethod.CASH, PaymentStatus.PAID);
        awardService.onPaymentSucceeded(rabbitTrip.getId());

        OutboxEvent carbonEvent = new OutboxEvent();
        carbonEvent.setAggregateType("Carbon");
        carbonEvent.setAggregateId(rabbitTrip.getId().toString());
        carbonEvent.setRoutingKey(RabbitMQConfig.ROUTING_KEY_CARBON_CALCULATED);
        carbonEvent.setPayload(objectMapper.writeValueAsString(new CarbonCalculatedEvent(
                rabbitTrip.getId(), rabbitTrip.getTripCode(), customerUserB.getId(), SEED_DRIVER_PROFILE_ID,
                SEED_DRIVER_USER_ID, new BigDecimal("750.00"), new BigDecimal("1100.00"), new BigDecimal("350.00"),
                new BigDecimal("0.7500"), 7, "slug-" + rabbitTrip.getId().toString().substring(0, 8), Instant.now()
        )));
        carbonEvent.setStatus("NEW");
        carbonEvent.setNextAttemptAt(Instant.now().minusSeconds(1));
        carbonEvent = outboxEventRepository.save(carbonEvent);
        final UUID carbonEventId = carbonEvent.getId();

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            outboxPublisher.publishPendingEvents();
            OutboxEvent sentEv = outboxEventRepository.findById(carbonEventId).orElseThrow();
            assertEquals("SENT", sentEv.getStatus());
        });

        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            TripIncentiveAward award = awardRepository.findByTripId(rabbitTrip.getId()).orElse(null);
            assertNotNull(award);
            assertEquals("CREDITED", award.getStatus());
        });

        // AC-19B: FAILED outbox event -> GET /admin/outbox/failed -> POST /admin/outbox/{id}/redrive resets to NEW
        OutboxEvent failedEv = new OutboxEvent();
        failedEv.setAggregateType("Payment");
        failedEv.setAggregateId(UUID.randomUUID().toString());
        failedEv.setRoutingKey("payment.event.succeeded");
        failedEv.setPayload("{\"tripId\":\"" + UUID.randomUUID() + "\"}");
        failedEv.setStatus("FAILED");
        failedEv.setAttempts(10);
        failedEv = outboxEventRepository.save(failedEv);

        mockMvc.perform(get("/admin/outbox/failed")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(failedEv.getId().toString())));

        mockMvc.perform(post("/admin/outbox/{id}/redrive", failedEv.getId())
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andExpect(jsonPath("$.data.attempts").value(0));

        // Clean failedEv to NEW->SENT or leave harmless pending event marked FAILED won't disturb ledger
        failedEv = outboxEventRepository.findById(failedEv.getId()).orElseThrow();
        failedEv.setStatus("SENT");
        outboxEventRepository.save(failedEv);

        // AC-20: Stuck PENDING_CONDITIONS award > 5 mins -> IncentiveReconciliationJob recovers using receipt + paid trip
        Trip stuckTrip = createCompletedTrip(customerUserB.getId(), SEED_DRIVER_PROFILE_ID, 35000L, 0L, PaymentMethod.CASH, PaymentStatus.PAID);
        TripImpactReceipt receipt = new TripImpactReceipt();
        receipt.setTripId(stuckTrip.getId());
        receipt.setCo2SavedGrams(new BigDecimal("487.00"));
        receipt.setBaselineGasolineCo2Grams(new BigDecimal("685.00"));
        receipt.setEvEmittedCo2Grams(new BigDecimal("198.00"));
        receipt.setTreeAbsorptionDaysEquiv(new BigDecimal("8.12"));
        receipt.setLedBulbHoursEquiv(new BigDecimal("78.71"));
        receipt.setShareableSlug("s6-rec-" + stuckTrip.getId().toString().substring(0, 8));
        receiptRepository.save(receipt);

        TripIncentiveAward stuckAward = new TripIncentiveAward();
        stuckAward.setTripId(stuckTrip.getId());
        stuckAward.setCustomerId(customerUserB.getId());
        stuckAward.setDriverUserId(SEED_DRIVER_USER_ID);
        stuckAward.setStatus("PENDING_CONDITIONS");
        stuckAward.setCarbonReady(false);
        stuckAward.setPaymentReady(false);
        stuckAward = awardRepository.save(stuckAward);
        jdbcTemplate.update("UPDATE trip_incentive_awards SET created_at = NOW() - INTERVAL '15 minutes' WHERE id = ?",
                stuckAward.getId());

        jdbcTemplate.update("DELETE FROM shedlock WHERE name = 'IncentiveReconciliationJob'");
        reconciliationJob.reconcileStuckPendingAwards();
        TripIncentiveAward reconciled = awardRepository.findById(stuckAward.getId()).orElseThrow();
        assertEquals("CREDITED", reconciled.getStatus());
        assertTrue(reconciled.isCarbonReady());
        assertTrue(reconciled.isPaymentReady());
    }

    @Test
    @Order(11)
    @DisplayName("AC-21: RBAC (OPERATOR read-only / FINANCE+ADMIN write), IDOR, Idempotency-Key=422, Disabled user & Refresh token blocked")
    void ac21_rbacIdorIdempotencyAndSecurityEdgeCases() throws Exception {
        // OPERATOR allowed on GET, forbidden (403) on POST/PUT
        mockMvc.perform(get("/admin/ledger/audit").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/incentive/catalog").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/ledger/circuit-breaker")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frozen\": true, \"reason\": \"Operator thu ghi\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/outbox/redrive-all")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        // Missing Idempotency-Key -> 422
        mockMvc.perform(post("/incentive/carbon/convert")
                        .header("Authorization", "Bearer " + customerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pccAmount\": 1.0000}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"));

        // IDOR: Customer A attempts to cancel Customer B's redemption -> 404
        creditPoints(customerUserB.getId(), 50);
        IncentiveDtos.RedemptionResponseDto rdB = ecoStoreService.redeemReward(
                customerUserB.getId(), VOUCHER_20K_REWARD_ID, "IDOR-" + UUID.randomUUID());
        mockMvc.perform(post("/incentive/redemptions/{id}/cancel", rdB.id())
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isNotFound());

        // Refresh token rejected as Bearer access token
        String refreshToken = jwtTokenProvider.generateRefreshToken(customerUserA.getId(), customerUserA.getPhoneNumber());
        mockMvc.perform(get("/incentive/summary")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isForbidden());

        // Disabled user blocked both on login and with pre-issued JWT
        String disabledJwt = jwtTokenProvider.generateToken(disabledUser.getId(), disabledUser.getPhoneNumber(), Role.ROLE_CUSTOMER.name());
        mockMvc.perform(get("/incentive/summary")
                        .header("Authorization", "Bearer " + disabledJwt))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0900990005\",\"password\":\"Admin@123456\"}"))
                .andExpect(status().isBadRequest());

        // New user registration provisions PCC + ECO_POINT + VND wallets automatically
        String newPhone = "09" + (10000000 + ThreadLocalRandom.current().nextInt(89999999));
        MvcResult regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + newPhone + "\",\"email\":\"" + newPhone + "@gm.vn\"," +
                                "\"password\":\"Secret@123\",\"fullName\":\"New Green User\",\"role\":\"ROLE_CUSTOMER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode regJson = objectMapper.readTree(regRes.getResponse().getContentAsString());
        UUID newUserId = UUID.fromString(regJson.path("data").path("userId").asText());
        assertTrue(ledgerModuleApi.findAccount(newUserId, AccountType.USER_CARBON_WALLET, "PCC").isPresent());
        assertTrue(ledgerModuleApi.findAccount(newUserId, AccountType.USER_POINTS_WALLET, "ECO_POINT").isPresent());
    }

    @Test
    @Order(12)
    @DisplayName("AC-24: Kiểm toán Tổng thể Sổ cái Đa tiền tệ Δ = 0.0000 cho VND, PCC, ECO_POINT")
    void ac24_fullLedgerAuditZeroDeltaAcrossAllCurrencies() throws Exception {
        MvcResult res = mockMvc.perform(get("/admin/ledger/audit")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBalanced").value(true))
                .andExpect(jsonPath("$.data.unbalancedTransactionsCount").value(0))
                .andExpect(jsonPath("$.data.crossCurrencyTransactionsCount").value(0))
                .andExpect(jsonPath("$.data.accountDiscrepanciesCount").value(0))
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        JsonNode totals = data.path("totalsByCurrency");
        for (String ccy : List.of("VND", "PCC", "ECO_POINT")) {
            BigDecimal delta = new BigDecimal(totals.path(ccy).asText("999"));
            assertEquals(0, BigDecimal.ZERO.compareTo(delta), "Currency " + ccy + " net sum must be 0, found " + delta);
        }
    }

    // --- Helper Fixtures ---

    private User ensureUser(String phone, String email, String name, Role role, UserStatus status) {
        return userRepository.findByPhoneNumber(phone).orElseGet(() -> {
            User u = new User(phone, email, passwordEncoder.encode("Admin@123456"), name, role);
            u.setStatus(status);
            return userRepository.save(u);
        });
    }

    private Trip createCompletedTrip(UUID customerId, UUID driverProfileId, long grossVnd, long discountVnd,
                                     PaymentMethod pm, PaymentStatus ps) {
        Trip t = new Trip();
        t.setTripCode("GM-S6-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        t.setCustomerId(customerId);
        t.setDriverId(driverProfileId);
        t.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        t.setPickupGeom(gf.createPoint(new Coordinate(106.6983, 10.7725)));
        t.setPickupAddress("Chợ Bến Thành, Q1");
        t.setDropoffGeom(gf.createPoint(new Coordinate(106.7218, 10.7951)));
        t.setDropoffAddress("Landmark 81, Bình Thạnh");
        t.setStatus(TripStatus.COMPLETED);
        t.setEstimatedDistanceM(5000);
        t.setEstimatedDurationS(600);
        t.setActualDistanceM(5000);
        t.setActualDurationS(600);
        t.setFareAmount(BigDecimal.valueOf(grossVnd));
        t.setDiscountAmount(BigDecimal.valueOf(discountVnd));
        t.setFinalAmount(BigDecimal.valueOf(Math.max(0L, grossVnd - discountVnd)));
        t.setPaymentMethod(pm);
        t.setPaymentStatus(ps);
        t.setCompletedAt(Instant.now());
        return tripRepository.save(t);
    }

    private void topupVnd(UUID userId, long amountVnd) {
        LedgerAccount sysTopup = ledgerModuleApi.getSystemAccount(AccountType.GATEWAY_CLEARING);
        LedgerAccount userWallet = ledgerModuleApi.getOrCreateUserWallet(userId);
        ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.TOPUP, "Test Topup VND",
                "TEST-TOPUP-" + UUID.randomUUID(), "TOPUP", null, null,
                List.of(
                        new PostingEntry(sysTopup.getId(), EntryType.DEBIT, BigDecimal.valueOf(amountVnd)),
                        new PostingEntry(userWallet.getId(), EntryType.CREDIT, BigDecimal.valueOf(amountVnd))
                )
        ));
    }

    private void creditPcc(UUID userId, BigDecimal amount) {
        LedgerAccount sysPcc = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE);
        LedgerAccount userPcc = ledgerModuleApi.getOrCreateCarbonWallet(userId);
        ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.TRIP_CARBON_REWARD, "Test Credit PCC",
                "TEST-PCC-" + UUID.randomUUID(), "TEST", null, null,
                List.of(
                        new PostingEntry(sysPcc.getId(), EntryType.DEBIT, amount),
                        new PostingEntry(userPcc.getId(), EntryType.CREDIT, amount)
                )
        ));
    }

    private void creditPoints(UUID userId, int points) {
        LedgerAccount sysPts = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_LOYALTY_RESERVE);
        LedgerAccount userPts = ledgerModuleApi.getOrCreatePointsWallet(userId);
        ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.TRIP_POINTS_REWARD, "Test Credit PTS",
                "TEST-PTS-" + UUID.randomUUID(), "TEST", null, null,
                List.of(
                        new PostingEntry(sysPts.getId(), EntryType.DEBIT, BigDecimal.valueOf(points)),
                        new PostingEntry(userPts.getId(), EntryType.CREDIT, BigDecimal.valueOf(points))
                )
        ));
    }
}

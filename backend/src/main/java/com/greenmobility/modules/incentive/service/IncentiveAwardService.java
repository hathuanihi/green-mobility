package com.greenmobility.modules.incentive.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.fraud.api.FraudModuleApi;
import com.greenmobility.modules.incentive.domain.TripIncentiveAward;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.repository.TripIncentiveAwardRepository;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.EntryType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import com.greenmobility.modules.ledger.domain.LedgerTxType;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class IncentiveAwardService {

    private static final Logger log = LoggerFactory.getLogger(IncentiveAwardService.class);

    private final TripIncentiveAwardRepository awardRepository;
    private final LedgerModuleApi ledgerModuleApi;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final PlatformSettingsService settingsService;
    private final OutboxService outboxService;
    private final AuditLogService auditLogService;
    private final TripModuleApi tripModuleApi;
    private final FraudModuleApi fraudModuleApi;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public IncentiveAwardService(TripIncentiveAwardRepository awardRepository,
                                 LedgerModuleApi ledgerModuleApi,
                                 LedgerAccountRepository ledgerAccountRepository,
                                 PlatformSettingsService settingsService,
                                 OutboxService outboxService,
                                 AuditLogService auditLogService,
                                 TripModuleApi tripModuleApi,
                                 @Autowired(required = false) FraudModuleApi fraudModuleApi,
                                 @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.awardRepository = awardRepository;
        this.ledgerModuleApi = ledgerModuleApi;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.settingsService = settingsService;
        this.outboxService = outboxService;
        this.auditLogService = auditLogService;
        this.tripModuleApi = tripModuleApi;
        this.fraudModuleApi = fraudModuleApi;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public TripIncentiveAward onCarbonCalculated(UUID tripId, UUID customerId, UUID driverUserId, BigDecimal co2SavedGrams) {
        UUID resolvedDriverUserId = resolveDriverUserId(tripId, driverUserId);
        TripIncentiveAward award = ensureAwardRowLocked(tripId, customerId, resolvedDriverUserId);

        if (resolvedDriverUserId != null && award.getDriverUserId() == null) {
            award.setDriverUserId(resolvedDriverUserId);
        }
        award.setCo2SavedGrams(co2SavedGrams != null ? co2SavedGrams : BigDecimal.ZERO);
        award.setCarbonReady(true);
        awardRepository.save(award);

        return evaluateJoinBarrier(award);
    }

    @Transactional
    public TripIncentiveAward onPaymentSucceeded(UUID tripId) {
        Optional<TripSnapshotDto> tripOpt = tripModuleApi.getTrip(tripId);
        if (tripOpt.isEmpty()) {
            log.warn("Trip {} not found when handling PaymentSucceededEvent for incentive", tripId);
            return null;
        }
        TripSnapshotDto trip = tripOpt.get();
        TripIncentiveAward award = ensureAwardRowLocked(tripId, trip.customerId(), trip.driverUserId());

        if (trip.driverUserId() != null && award.getDriverUserId() == null) {
            award.setDriverUserId(trip.driverUserId());
        }
        award.setPaymentReady(true);
        awardRepository.save(award);

        return evaluateJoinBarrier(award);
    }

    @Transactional
    public TripIncentiveAward tryReconcilePendingAward(UUID awardId, BigDecimal receiptCo2Grams, boolean paymentPaid) {
        TripIncentiveAward award = awardRepository.findByIdWithLock(awardId).orElse(null);
        if (award == null || !"PENDING_CONDITIONS".equals(award.getStatus())) {
            return award;
        }
        if (!award.isCarbonReady() && receiptCo2Grams != null) {
            award.setCo2SavedGrams(receiptCo2Grams);
            award.setCarbonReady(true);
        }
        if (!award.isPaymentReady() && paymentPaid) {
            award.setPaymentReady(true);
        }
        if (award.getDriverUserId() == null) {
            UUID resolved = resolveDriverUserId(award.getTripId(), null);
            if (resolved != null) {
                award.setDriverUserId(resolved);
            }
        }
        awardRepository.save(award);
        return evaluateJoinBarrier(award);
    }

    @Transactional
    public void onPaymentRefunded(UUID tripId, boolean fullRefund, String reason) {
        Optional<TripIncentiveAward> awardOpt = awardRepository.findByTripIdWithLock(tripId);
        if (awardOpt.isEmpty()) {
            return;
        }
        TripIncentiveAward award = awardOpt.get();

        if (!fullRefund) {
            auditLogService.record(null, "SYSTEM", "PARTIAL_REFUND_INCENTIVE_UNCHANGED",
                    "TripIncentiveAward", award.getId().toString(),
                    Map.of("status", award.getStatus(), "tripId", tripId.toString()),
                    Map.of("action", "NO_CLAWBACK_ON_PARTIAL_REFUND", "reason", reason != null ? reason : ""),
                    "127.0.0.1");
            return;
        }

        if ("CLAWED_BACK".equals(award.getStatus())
                || "CLAWBACK_SHORTFALL_AUDIT".equals(award.getStatus())
                || "VOIDED_FRAUD".equals(award.getStatus())) {
            return;
        }

        if (!"CREDITED".equals(award.getStatus())) {
            String prevStatus = award.getStatus();
            award.setStatus("CLAWED_BACK");
            award.setReviewNote("Thu hồi trước khi giải ngân do hoàn tiền toàn phần: " + (reason != null ? reason : ""));
            awardRepository.save(award);
            auditLogService.record(null, "SYSTEM", "INCENTIVE_VOIDED_ON_FULL_REFUND",
                    "TripIncentiveAward", award.getId().toString(),
                    Map.of("status", prevStatus),
                    Map.of("status", "CLAWED_BACK", "tripId", tripId.toString()),
                    "127.0.0.1");
            return;
        }

        // Lock user PCC and PTS wallets in ascending UUID order BEFORE checking balances to avoid race conditions
        LedgerAccount custPccAcc = award.getCustomerPcc().compareTo(BigDecimal.ZERO) > 0
                ? ledgerModuleApi.getOrCreateCarbonWallet(award.getCustomerId()) : null;
        LedgerAccount driverPccAcc = (award.getDriverUserId() != null && award.getDriverPcc().compareTo(BigDecimal.ZERO) > 0)
                ? ledgerModuleApi.getOrCreateCarbonWallet(award.getDriverUserId()) : null;
        LedgerAccount custPtsAcc = award.getCustomerPoints() > 0
                ? ledgerModuleApi.getOrCreatePointsWallet(award.getCustomerId()) : null;
        LedgerAccount driverPtsAcc = (award.getDriverUserId() != null && award.getDriverPoints() > 0)
                ? ledgerModuleApi.getOrCreatePointsWallet(award.getDriverUserId()) : null;

        List<UUID> lockIds = java.util.stream.Stream.of(custPccAcc, driverPccAcc, custPtsAcc, driverPtsAcc)
                .filter(Objects::nonNull)
                .map(LedgerAccount::getId)
                .distinct()
                .sorted()
                .toList();
        Map<UUID, LedgerAccount> lockedAccs = new HashMap<>();
        for (UUID accId : lockIds) {
            ledgerAccountRepository.findByIdWithLock(accId).ifPresent(a -> lockedAccs.put(accId, a));
        }
        if (custPccAcc != null) custPccAcc = lockedAccs.getOrDefault(custPccAcc.getId(), custPccAcc);
        if (driverPccAcc != null) driverPccAcc = lockedAccs.getOrDefault(driverPccAcc.getId(), driverPccAcc);
        if (custPtsAcc != null) custPtsAcc = lockedAccs.getOrDefault(custPtsAcc.getId(), custPtsAcc);
        if (driverPtsAcc != null) driverPtsAcc = lockedAccs.getOrDefault(driverPtsAcc.getId(), driverPtsAcc);

        // Check sufficient balances in PCC and ECO_POINT wallets (NEVER auto-debit VND!)
        boolean shortfall = false;
        StringBuilder shortfallReasons = new StringBuilder();

        if (custPccAcc != null && custPccAcc.getBalance().compareTo(award.getCustomerPcc()) < 0) {
            shortfall = true;
            shortfallReasons.append("Customer PCC balance=").append(custPccAcc.getBalance())
                    .append(" < ").append(award.getCustomerPcc()).append("; ");
        }

        if (driverPccAcc != null && driverPccAcc.getBalance().compareTo(award.getDriverPcc()) < 0) {
            shortfall = true;
            shortfallReasons.append("Driver PCC balance=").append(driverPccAcc.getBalance())
                    .append(" < ").append(award.getDriverPcc()).append("; ");
        }

        if (custPtsAcc != null && custPtsAcc.getBalance().compareTo(BigDecimal.valueOf(award.getCustomerPoints())) < 0) {
            shortfall = true;
            shortfallReasons.append("Customer PTS balance=").append(custPtsAcc.getBalance())
                    .append(" < ").append(award.getCustomerPoints()).append("; ");
        }

        if (driverPtsAcc != null && driverPtsAcc.getBalance().compareTo(BigDecimal.valueOf(award.getDriverPoints())) < 0) {
            shortfall = true;
            shortfallReasons.append("Driver PTS balance=").append(driverPtsAcc.getBalance())
                    .append(" < ").append(award.getDriverPoints()).append("; ");
        }

        if (shortfall) {
            award.setStatus("CLAWBACK_SHORTFALL_AUDIT");
            award.setReviewNote("Khong du so du PCC/Eco-Point de thu hoi tu dong (khong tu dong tru VND): " + shortfallReasons);
            awardRepository.save(award);
            auditLogService.record(null, "SYSTEM", "CLAWBACK_SHORTFALL_AUDIT",
                    "TripIncentiveAward", award.getId().toString(),
                    Map.of("status", "CREDITED"),
                    Map.of("status", "CLAWBACK_SHORTFALL_AUDIT", "details", shortfallReasons.toString()),
                    "127.0.0.1");
            return;
        }

        // Execute atomic clawback for PCC & EcoPoints (bypass frozen guard since SYSTEM clawback)
        if (award.getTotalPcc().compareTo(BigDecimal.ZERO) > 0) {
            LedgerAccount carbonReserve = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE);
            List<PostingEntry> pccEntries = new ArrayList<>();
            BigDecimal totalClawPcc = BigDecimal.ZERO;
            if (custPccAcc != null && award.getCustomerPcc().compareTo(BigDecimal.ZERO) > 0) {
                pccEntries.add(new PostingEntry(custPccAcc.getId(), EntryType.DEBIT, award.getCustomerPcc()));
                totalClawPcc = totalClawPcc.add(award.getCustomerPcc());
            }
            if (driverPccAcc != null && award.getDriverPcc().compareTo(BigDecimal.ZERO) > 0) {
                pccEntries.add(new PostingEntry(driverPccAcc.getId(), EntryType.DEBIT, award.getDriverPcc()));
                totalClawPcc = totalClawPcc.add(award.getDriverPcc());
            }
            if (totalClawPcc.compareTo(BigDecimal.ZERO) > 0) {
                pccEntries.add(new PostingEntry(carbonReserve.getId(), EntryType.CREDIT, totalClawPcc));
                LedgerTransaction clawPccTx = ledgerModuleApi.post(new PostingRequest(
                        null, tripId, LedgerTxType.INCENTIVE_CLAWBACK,
                        "Thu hoi tin chi Carbon do hoan tien toan phan chuyen " + tripId,
                        "CLAWBACK:" + tripId + ":PCC",
                        "TRIP_AWARD_CLAWBACK", award.getId(), award.getPccLedgerTxId(), pccEntries, true
                ));
                award.setClawbackPccTxId(clawPccTx.getId());
            }
        }

        int totalClawPts = award.getCustomerPoints() + award.getDriverPoints();
        if (totalClawPts > 0) {
            LedgerAccount ptsReserve = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_LOYALTY_RESERVE);
            List<PostingEntry> ptsEntries = new ArrayList<>();
            int actualPts = 0;
            if (custPtsAcc != null && award.getCustomerPoints() > 0) {
                ptsEntries.add(new PostingEntry(custPtsAcc.getId(), EntryType.DEBIT, BigDecimal.valueOf(award.getCustomerPoints())));
                actualPts += award.getCustomerPoints();
            }
            if (driverPtsAcc != null && award.getDriverPoints() > 0) {
                ptsEntries.add(new PostingEntry(driverPtsAcc.getId(), EntryType.DEBIT, BigDecimal.valueOf(award.getDriverPoints())));
                actualPts += award.getDriverPoints();
            }
            if (actualPts > 0) {
                ptsEntries.add(new PostingEntry(ptsReserve.getId(), EntryType.CREDIT, BigDecimal.valueOf(actualPts)));
                LedgerTransaction clawPtsTx = ledgerModuleApi.post(new PostingRequest(
                        null, tripId, LedgerTxType.INCENTIVE_CLAWBACK,
                        "Thu hoi Diem Xanh do hoan tien toan phan chuyen " + tripId,
                        "CLAWBACK:" + tripId + ":PTS",
                        "TRIP_AWARD_CLAWBACK", award.getId(), award.getPointsLedgerTxId(), ptsEntries, true
                ));
                award.setClawbackPointsTxId(clawPtsTx.getId());
            }
        }

        award.setStatus("CLAWED_BACK");
        award.setReviewNote("Thu hồi thưởng tự động do hoàn tiền toàn phần: " + (reason != null ? reason : ""));
        awardRepository.save(award);
    }

    @Transactional
    public IncentiveDtos.TripAwardDto approveHeldAward(UUID awardId, UUID adminId, String adminEmail, String note, String ipAddress) {
        TripIncentiveAward award = awardRepository.findByIdWithLock(awardId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "AWARD_NOT_FOUND",
                        "Không tìm thấy bản ghi thưởng chuyến đi: " + awardId));
        if (!"HELD_FOR_FRAUD".equals(award.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "AWARD_INVALID_STATUS",
                    "Chỉ có thể duyệt giải ngân bản ghi đang ở trạng thái HELD_FOR_FRAUD");
        }
        award.setReviewedBy(adminId);
        award.setReviewedAt(Instant.now());
        award.setReviewNote(note);

        TripIncentiveAward credited = creditAward(award);

        auditLogService.record(adminId, adminEmail, "APPROVE_HELD_INCENTIVE",
                "TripIncentiveAward", award.getId().toString(),
                Map.of("status", "HELD_FOR_FRAUD"),
                Map.of("status", credited.getStatus(), "note", note != null ? note : ""),
                ipAddress);
        return toDto(credited);
    }

    @Transactional
    public IncentiveDtos.TripAwardDto voidHeldAward(UUID awardId, UUID adminId, String adminEmail, String note, String ipAddress) {
        if (note == null || note.trim().length() < 5) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "NOTE_REQUIRED",
                    "Vui lòng nhập lý do hủy thưởng (tối thiểu 5 ký tự)");
        }
        TripIncentiveAward award = awardRepository.findByIdWithLock(awardId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "AWARD_NOT_FOUND",
                        "Không tìm thấy bản ghi thưởng chuyến đi: " + awardId));
        if (!"HELD_FOR_FRAUD".equals(award.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "AWARD_INVALID_STATUS",
                    "Chỉ có thể hủy thưởng bản ghi đang ở trạng thái HELD_FOR_FRAUD");
        }

        award.setStatus("VOIDED_FRAUD");
        award.setReviewedBy(adminId);
        award.setReviewedAt(Instant.now());
        award.setReviewNote(note.trim());
        awardRepository.save(award);

        auditLogService.record(adminId, adminEmail, "VOID_HELD_INCENTIVE",
                "TripIncentiveAward", award.getId().toString(),
                Map.of("status", "HELD_FOR_FRAUD"),
                Map.of("status", "VOIDED_FRAUD", "note", note.trim()),
                ipAddress);
        return toDto(award);
    }

    @Transactional(readOnly = true)
    public Page<IncentiveDtos.TripAwardDto> searchAwards(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return awardRepository.findByStatusFiltered(status, pageable).map(this::toDto);
    }

    private TripIncentiveAward ensureAwardRowLocked(UUID tripId, UUID customerId, UUID driverUserId) {
        awardRepository.insertIfNotExists(tripId, customerId, driverUserId);
        return awardRepository.findByTripIdWithLock(tripId)
                .orElseThrow(() -> new IllegalStateException("Failed to lock trip_incentive_awards row for trip " + tripId));
    }

    private TripIncentiveAward evaluateJoinBarrier(TripIncentiveAward award) {
        if (!"PENDING_CONDITIONS".equals(award.getStatus())) {
            return award;
        }
        if (!award.isCarbonReady() || !award.isPaymentReady()) {
            return award;
        }

        // Check trip refund status in case payment was refunded concurrently
        Optional<TripSnapshotDto> tripOpt = tripModuleApi.getTrip(award.getTripId());
        if (tripOpt.isPresent() && tripOpt.get().paymentStatus() == PaymentStatus.REFUNDED) {
            award.setStatus("CLAWED_BACK");
            award.setReviewNote("Không giải ngân vì chuyến đi đã hoàn tiền toàn phần");
            return awardRepository.save(award);
        }

        // Fraud hold gate
        if (fraudModuleApi != null && fraudModuleApi.hasBlockingFraudAlert(award.getTripId())) {
            String reason = fraudModuleApi.getBlockingFraudReason(award.getTripId())
                    .orElse("Chuyến đi có cảnh báo gian lận cần Admin rà soát trước khi cộng thưởng");
            award.setStatus("HELD_FOR_FRAUD");
            award.setHoldReason(reason);
            log.warn("Trip {} incentive held due to fraud alert: {}", award.getTripId(), reason);
            return awardRepository.save(award);
        }

        return creditAward(award);
    }

    private TripIncentiveAward creditAward(TripIncentiveAward award) {
        UUID tripId = award.getTripId();
        BigDecimal co2Grams = award.getCo2SavedGrams() != null ? award.getCo2SavedGrams() : BigDecimal.ZERO;

        if (co2Grams.compareTo(BigDecimal.ZERO) <= 0) {
            award.setStatus("CREDITED");
            award.setCreditedAt(Instant.now());
            return awardRepository.save(award);
        }

        // 1. Compute PCC with 100% CO2 conservation: C = floor(T * ratio, 4), D = T - C
        BigDecimal pccGramsPerUnit = settingsService.getPccGramsPerUnit();
        BigDecimal totalPcc = co2Grams.divide(pccGramsPerUnit, 4, RoundingMode.FLOOR);

        UUID driverUserId = award.getDriverUserId();
        BigDecimal custShareRatio = settingsService.getPccCustomerShareRatio();
        BigDecimal customerPcc;
        BigDecimal driverPcc;
        if (driverUserId != null) {
            customerPcc = totalPcc.multiply(custShareRatio).setScale(4, RoundingMode.FLOOR);
            driverPcc = totalPcc.subtract(customerPcc).setScale(4, RoundingMode.FLOOR);
        } else {
            customerPcc = totalPcc;
            driverPcc = BigDecimal.ZERO.setScale(4, RoundingMode.FLOOR);
        }

        // 2. Compute Eco-Points: customer = max(1, floor(grams / ecoGrams)), driver = floor(customer * driverRatio)
        BigDecimal ecoGramsPerPoint = settingsService.getEcoPointGramsPerPoint();
        int rawCustPts = co2Grams.divide(ecoGramsPerPoint, 0, RoundingMode.FLOOR).intValue();
        int customerPts = Math.max(1, rawCustPts);
        BigDecimal driverEcoRatio = settingsService.getDriverEcoPointRatio();
        int driverPts = (driverUserId != null && driverEcoRatio.compareTo(BigDecimal.ZERO) > 0)
                ? Math.max(1, BigDecimal.valueOf(customerPts).multiply(driverEcoRatio).setScale(0, RoundingMode.FLOOR).intValue())
                : 0;

        award.setTotalPcc(totalPcc);
        award.setCustomerPcc(customerPcc);
        award.setDriverPcc(driverPcc);
        award.setCustomerPoints(customerPts);
        award.setDriverPoints(driverPts);

        // 3. Post PCC ledger transaction (fixed idempotencyKey INC:{tripId}:PCC, skip zero entries)
        if (totalPcc.compareTo(BigDecimal.ZERO) > 0) {
            LedgerAccount carbonReserve = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RESERVE);
            List<PostingEntry> pccEntries = new ArrayList<>();
            pccEntries.add(new PostingEntry(carbonReserve.getId(), EntryType.DEBIT, totalPcc));
            if (customerPcc.compareTo(BigDecimal.ZERO) > 0) {
                LedgerAccount custPccAcc = ledgerModuleApi.getOrCreateCarbonWallet(award.getCustomerId());
                pccEntries.add(new PostingEntry(custPccAcc.getId(), EntryType.CREDIT, customerPcc));
            }
            if (driverUserId != null && driverPcc.compareTo(BigDecimal.ZERO) > 0) {
                LedgerAccount driverPccAcc = ledgerModuleApi.getOrCreateCarbonWallet(driverUserId);
                pccEntries.add(new PostingEntry(driverPccAcc.getId(), EntryType.CREDIT, driverPcc));
            }
            if (pccEntries.size() >= 2) {
                LedgerTransaction pccTx = ledgerModuleApi.post(new PostingRequest(
                        null, tripId, LedgerTxType.TRIP_CARBON_REWARD,
                        "Cong Tin chi Carbon tu chuyen di xanh " + tripId,
                        "INC:" + tripId + ":PCC",
                        "TRIP_AWARD", award.getId(), null, pccEntries
                ));
                award.setPccLedgerTxId(pccTx.getId());
            }
        }

        // 4. Post ECO_POINT ledger transaction (fixed idempotencyKey INC:{tripId}:PTS)
        int totalPts = customerPts + driverPts;
        if (totalPts > 0) {
            LedgerAccount ptsReserve = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_LOYALTY_RESERVE);
            List<PostingEntry> ptsEntries = new ArrayList<>();
            ptsEntries.add(new PostingEntry(ptsReserve.getId(), EntryType.DEBIT, BigDecimal.valueOf(totalPts)));
            if (customerPts > 0) {
                LedgerAccount custPtsAcc = ledgerModuleApi.getOrCreatePointsWallet(award.getCustomerId());
                ptsEntries.add(new PostingEntry(custPtsAcc.getId(), EntryType.CREDIT, BigDecimal.valueOf(customerPts)));
            }
            if (driverUserId != null && driverPts > 0) {
                LedgerAccount driverPtsAcc = ledgerModuleApi.getOrCreatePointsWallet(driverUserId);
                ptsEntries.add(new PostingEntry(driverPtsAcc.getId(), EntryType.CREDIT, BigDecimal.valueOf(driverPts)));
            }
            LedgerTransaction ptsTx = ledgerModuleApi.post(new PostingRequest(
                    null, tripId, LedgerTxType.TRIP_POINTS_REWARD,
                    "Cong Diem Xanh tu chuyen di " + tripId,
                    "INC:" + tripId + ":PTS",
                    "TRIP_AWARD", award.getId(), null, ptsEntries
            ));
            award.setPointsLedgerTxId(ptsTx.getId());
        }

        award.setStatus("CREDITED");
        award.setCreditedAt(Instant.now());
        TripIncentiveAward saved = awardRepository.save(award);

        // 5. Outbox event for Sprint 7 Leaderboard/Badges + WebSocket push
        Map<String, Object> outboxPayload = Map.of(
                "awardId", saved.getId().toString(),
                "tripId", tripId.toString(),
                "customerId", saved.getCustomerId().toString(),
                "driverUserId", driverUserId != null ? driverUserId.toString() : "",
                "co2SavedGrams", co2Grams,
                "customerPcc", customerPcc,
                "driverPcc", driverPcc,
                "customerPoints", customerPts,
                "driverPoints", driverPts,
                "creditedAt", saved.getCreditedAt().toString()
        );
        outboxService.recordEvent("TripIncentiveAward", saved.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_INCENTIVE_CREDITED, outboxPayload);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSendToUser(saved.getCustomerId().toString(), "/queue/incentives", outboxPayload);
                if (driverUserId != null) {
                    messagingTemplate.convertAndSendToUser(driverUserId.toString(), "/queue/incentives", outboxPayload);
                }
            } catch (Exception ignored) {}
        }

        log.info("Credited trip incentive award tripId={}, customerPcc={}, driverPcc={}, customerPts={}, driverPts={}",
                tripId, customerPcc, driverPcc, customerPts, driverPts);
        return saved;
    }

    private UUID resolveDriverUserId(UUID tripId, UUID candidateDriverUserId) {
        if (candidateDriverUserId != null) {
            return candidateDriverUserId;
        }
        return tripModuleApi.getTrip(tripId)
                .map(TripSnapshotDto::driverUserId)
                .orElse(null);
    }

    public IncentiveDtos.TripAwardDto toDto(TripIncentiveAward a) {
        return new IncentiveDtos.TripAwardDto(
                a.getId(), a.getTripId(), a.getCustomerId(), a.getDriverUserId(),
                a.getCo2SavedGrams(), a.getTotalPcc(), a.getCustomerPcc(), a.getDriverPcc(),
                a.getCustomerPoints(), a.getDriverPoints(), a.isCarbonReady(), a.isPaymentReady(),
                a.getStatus(), a.getHoldReason(), a.getPccLedgerTxId(), a.getPointsLedgerTxId(),
                a.getCreditedAt(), a.getReviewedBy(), a.getReviewedAt(), a.getReviewNote(),
                a.getCreatedAt()
        );
    }
}

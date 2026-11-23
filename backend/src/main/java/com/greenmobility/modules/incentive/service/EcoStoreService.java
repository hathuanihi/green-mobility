package com.greenmobility.modules.incentive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.incentive.api.IncentiveModuleApi;
import com.greenmobility.modules.incentive.domain.Reward;
import com.greenmobility.modules.incentive.domain.RewardRedemption;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.repository.RewardRedemptionRepository;
import com.greenmobility.modules.incentive.repository.RewardRepository;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.*;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EcoStoreService implements IncentiveModuleApi {

    private static final Logger log = LoggerFactory.getLogger(EcoStoreService.class);
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final RewardRepository rewardRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final LedgerModuleApi ledgerModuleApi;
    private final LedgerAccountRepository accountRepository;
    private final PlatformSettingsService settingsService;
    private final OutboxService outboxService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public EcoStoreService(RewardRepository rewardRepository,
                           RewardRedemptionRepository redemptionRepository,
                           LedgerModuleApi ledgerModuleApi,
                           LedgerAccountRepository accountRepository,
                           PlatformSettingsService settingsService,
                           OutboxService outboxService,
                           AuditLogService auditLogService,
                           ObjectMapper objectMapper) {
        this.rewardRepository = rewardRepository;
        this.redemptionRepository = redemptionRepository;
        this.ledgerModuleApi = ledgerModuleApi;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
        this.outboxService = outboxService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<IncentiveDtos.RewardCatalogDto> listCatalog(boolean activeOnly, String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return rewardRepository.findCatalog(activeOnly, category, pageable).map(this::toCatalogDto);
    }

    @Transactional(readOnly = true)
    public Page<IncentiveDtos.RedemptionResponseDto> getMyRedemptions(UUID userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<RewardRedemption> pageData = redemptionRepository.findByUserIdFiltered(userId, status, pageable);
        Map<UUID, Reward> rewardMap = new HashMap<>();
        List<UUID> rewardIds = pageData.getContent().stream().map(RewardRedemption::getRewardId).distinct().toList();
        if (!rewardIds.isEmpty()) {
            rewardRepository.findAllById(rewardIds).forEach(r -> rewardMap.put(r.getId(), r));
        }
        return pageData.map(r -> toRedemptionDto(r, rewardMap.get(r.getRewardId())));
    }

    @Transactional
    public IncentiveDtos.RedemptionResponseDto redeemReward(UUID userId, UUID rewardId, String idempotencyKey) {
        if (settingsService.isRedemptionsFrozen()) {
            throw new PaymentBusinessException(HttpStatus.SERVICE_UNAVAILABLE, "INCENTIVE_SYSTEM_FROZEN",
                    "Hệ thống đổi quà Eco-Store đang tạm khóa để kiểm toán");
        }

        Reward reward = rewardRepository.findByIdWithLock(rewardId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND",
                        "Không tìm thấy ưu đãi/quà tặng này"));

        if (!Boolean.TRUE.equals(reward.getIsActive())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REWARD_INACTIVE", "Ưu đãi này hiện đã ngừng phát hành");
        }

        long userRedeemedCount = redemptionRepository.countByUserIdAndRewardIdAndStatusNotIn(
                userId, rewardId, List.of("CANCELLED"));
        if (userRedeemedCount >= reward.getMaxRedemptionsPerUser()) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REWARD_USER_LIMIT_EXCEEDED",
                    "Bạn đã đạt giới hạn tối đa " + reward.getMaxRedemptionsPerUser() + " lần đổi cho ưu đãi này");
        }

        // Atomic stock decrement to prevent overselling under high concurrency
        int updated = rewardRepository.decrementStockAtomic(rewardId);
        if (updated == 0) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REWARD_OUT_OF_STOCK",
                    "Quà tặng hoặc voucher này đã hết lượt đổi");
        }

        BigDecimal pccCost = reward.getCarbonCreditCost() != null ? reward.getCarbonCreditCost() : BigDecimal.ZERO;
        int ptsCost = reward.getLoyaltyPointCost() != null ? reward.getLoyaltyPointCost() : 0;
        String baseKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        UUID pccTxId = null;
        if (pccCost.compareTo(BigDecimal.ZERO) > 0) {
            LedgerAccount userPcc = ledgerModuleApi.getOrCreateCarbonWallet(userId);
            LedgerAccount partnerPcc = ledgerModuleApi.getSystemAccount(AccountType.PARTNER_REDEMPTION_POOL, "PCC");
            LedgerTransaction tx = ledgerModuleApi.post(new PostingRequest(
                    null, null, LedgerTxType.REWARD_REDEEM_PCC,
                    "Doi qua Eco-Store: " + reward.getTitle(),
                    "REDEEM-PCC-" + baseKey,
                    "REWARD_REDEMPTION", rewardId, null,
                    List.of(
                            new PostingEntry(userPcc.getId(), EntryType.DEBIT, pccCost),
                            new PostingEntry(partnerPcc.getId(), EntryType.CREDIT, pccCost)
                    )
            ));
            pccTxId = tx.getId();
        }

        UUID ptsTxId = null;
        if (ptsCost > 0) {
            LedgerAccount userPts = ledgerModuleApi.getOrCreatePointsWallet(userId);
            LedgerAccount targetPtsSys = "VOUCHER_RIDE".equalsIgnoreCase(reward.getCategory())
                    ? ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_BURN_ACCOUNT)
                    : ledgerModuleApi.getSystemAccount(AccountType.PARTNER_REDEMPTION_POOL, "ECO_POINT");
            LedgerTransaction tx = ledgerModuleApi.post(new PostingRequest(
                    null, null, LedgerTxType.REWARD_REDEEM_POINTS,
                    "Doi qua Eco-Store: " + reward.getTitle(),
                    "REDEEM-PTS-" + baseKey,
                    "REWARD_REDEMPTION", rewardId, null,
                    List.of(
                            new PostingEntry(userPts.getId(), EntryType.DEBIT, BigDecimal.valueOf(ptsCost)),
                            new PostingEntry(targetPtsSys.getId(), EntryType.CREDIT, BigDecimal.valueOf(ptsCost))
                    )
            ));
            ptsTxId = tx.getId();
        }

        RewardRedemption redemption = new RewardRedemption();
        redemption.setUserId(userId);
        redemption.setRewardId(rewardId);
        redemption.setRedemptionCode(generateUniqueCode());
        redemption.setCarbonSpent(pccCost);
        redemption.setPointsSpent(ptsCost);
        redemption.setPccLedgerTxId(pccTxId);
        redemption.setPointsLedgerTxId(ptsTxId);

        boolean isVoucher = "VOUCHER_RIDE".equalsIgnoreCase(reward.getCategory());
        redemption.setStatus(isVoucher ? "AVAILABLE" : "COMPLETED");
        redemption.setExpiresAt(Instant.now().plus(reward.getValidityDays(), ChronoUnit.DAYS));

        if ("TREE_PLANTING".equalsIgnoreCase(reward.getCategory())) {
            try {
                Map<String, Object> cert = Map.of(
                        "certificateCode", "TREE-" + redemption.getRedemptionCode(),
                        "partner", reward.getPartnerName(),
                        "projectTitle", reward.getTitle(),
                        "pccRetired", pccCost,
                        "issuedAt", Instant.now().toString()
                );
                redemption.setCertificateMetadata(objectMapper.writeValueAsString(cert));
            } catch (Exception ignored) {}
        }

        RewardRedemption saved = redemptionRepository.save(redemption);

        outboxService.recordEvent("RewardRedemption", saved.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_INCENTIVE_REDEEMED,
                Map.of(
                        "redemptionId", saved.getId().toString(),
                        "userId", userId.toString(),
                        "rewardId", rewardId.toString(),
                        "category", reward.getCategory(),
                        "redemptionCode", saved.getRedemptionCode(),
                        "carbonSpent", pccCost,
                        "pointsSpent", ptsCost,
                        "redeemedAt", saved.getCreatedAt().toString()
                ));

        return toRedemptionDto(saved, reward);
    }

    @Transactional
    public IncentiveDtos.RedemptionResponseDto cancelRedemption(UUID userId, UUID redemptionId, String reason) {
        RewardRedemption redemption = redemptionRepository.findByIdWithLock(redemptionId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "REDEMPTION_NOT_FOUND",
                        "Không tìm thấy lượt đổi quà này"));
        if (!redemption.getUserId().equals(userId)) {
            throw new PaymentBusinessException(HttpStatus.NOT_FOUND, "REDEMPTION_NOT_FOUND",
                    "Không tìm thấy lượt đổi quà này");
        }
        if (!List.of("AVAILABLE", "ISSUED", "COMPLETED").contains(redemption.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REDEMPTION_CANNOT_CANCEL",
                    "Chỉ có thể hủy ưu đãi chưa sử dụng và chưa bị giữ cho chuyến đi (trạng thái hiện tại: " + redemption.getStatus() + ")");
        }

        Reward reward = rewardRepository.findById(redemption.getRewardId()).orElse(null);
        if (reward != null && "TREE_PLANTING".equalsIgnoreCase(reward.getCategory())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "TREE_CERTIFICATE_NON_REFUNDABLE",
                    "Chứng nhận trồng cây đã cấp không thể hủy hoàn");
        }

        if (redemption.getCarbonSpent().compareTo(BigDecimal.ZERO) > 0) {
            LedgerAccount userPcc = ledgerModuleApi.getOrCreateCarbonWallet(userId);
            LedgerAccount partnerPcc = ledgerModuleApi.getSystemAccount(AccountType.PARTNER_REDEMPTION_POOL, "PCC");
            LedgerTransaction revPcc = ledgerModuleApi.post(new PostingRequest(
                    null, null, LedgerTxType.REWARD_CANCEL_REVERSAL,
                    "Hoan PCC huy doi qua " + redemption.getRedemptionCode(),
                    "CANCEL-REDEEM-PCC-" + redemptionId,
                    "REDEMPTION_CANCEL", redemptionId, redemption.getPccLedgerTxId(),
                    List.of(
                            new PostingEntry(partnerPcc.getId(), EntryType.DEBIT, redemption.getCarbonSpent()),
                            new PostingEntry(userPcc.getId(), EntryType.CREDIT, redemption.getCarbonSpent())
                    )
            ));
            redemption.setReversalPccTxId(revPcc.getId());
        }

        if (redemption.getPointsSpent() > 0) {
            LedgerAccount userPts = ledgerModuleApi.getOrCreatePointsWallet(userId);
            boolean isVoucher = reward == null || "VOUCHER_RIDE".equalsIgnoreCase(reward.getCategory());
            LedgerAccount sourcePtsSys = isVoucher
                    ? ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_BURN_ACCOUNT)
                    : ledgerModuleApi.getSystemAccount(AccountType.PARTNER_REDEMPTION_POOL, "ECO_POINT");
            LedgerTransaction revPts = ledgerModuleApi.post(new PostingRequest(
                    null, null, LedgerTxType.REWARD_CANCEL_REVERSAL,
                    "Hoan Diem Xanh huy doi qua " + redemption.getRedemptionCode(),
                    "CANCEL-REDEEM-PTS-" + redemptionId,
                    "REDEMPTION_CANCEL", redemptionId, redemption.getPointsLedgerTxId(),
                    List.of(
                            new PostingEntry(sourcePtsSys.getId(), EntryType.DEBIT, BigDecimal.valueOf(redemption.getPointsSpent())),
                            new PostingEntry(userPts.getId(), EntryType.CREDIT, BigDecimal.valueOf(redemption.getPointsSpent()))
                    )
            ));
            redemption.setReversalPointsTxId(revPts.getId());
        }

        rewardRepository.incrementStockAtomic(redemption.getRewardId());
        redemption.setStatus("CANCELLED");
        redemption.setCancelledAt(Instant.now());
        redemption.setCancelReason(reason != null ? reason.trim() : "Khách hàng hủy đổi ưu đãi");
        RewardRedemption saved = redemptionRepository.save(redemption);
        return toRedemptionDto(saved, reward);
    }

    // --- Voucher Lifecycle API (IncentiveModuleApi) ---

    @Override
    @Transactional(readOnly = true)
    public VoucherPreviewDto previewVoucher(UUID customerId, UUID voucherRedemptionId, String voucherCode, BigDecimal grossFareVnd) {
        RewardRedemption redemption = resolveRedemptionReadOnly(voucherRedemptionId, voucherCode);
        Reward reward = validateVoucherEligibility(redemption, customerId, grossFareVnd);
        BigDecimal discount = calculateDiscount(reward, grossFareVnd);
        BigDecimal finalFare = grossFareVnd.subtract(discount).max(BigDecimal.ZERO);
        return new VoucherPreviewDto(redemption.getId(), redemption.getRedemptionCode(), reward.getTitle(),
                grossFareVnd, discount, finalFare);
    }

    @Override
    @Transactional
    public VoucherReservationDto reserveVoucherForTrip(UUID customerId, UUID tripId, UUID voucherRedemptionId,
                                                       String voucherCode, BigDecimal grossFareVnd) {
        RewardRedemption redemption = resolveRedemptionLocked(voucherRedemptionId, voucherCode);
        Reward reward = validateVoucherEligibility(redemption, customerId, grossFareVnd);
        BigDecimal discount = calculateDiscount(reward, grossFareVnd);
        BigDecimal finalFare = grossFareVnd.subtract(discount).max(BigDecimal.ZERO);

        redemption.setStatus("RESERVED");
        redemption.setAppliedTripId(tripId);
        redemption.setDiscountAppliedVnd(discount.longValue());
        redemptionRepository.save(redemption);

        log.info("Reserved voucher {} for trip {}, discount={}đ, finalFare={}đ",
                redemption.getRedemptionCode(), tripId, discount, finalFare);
        return new VoucherReservationDto(redemption.getId(), redemption.getRedemptionCode(),
                reward.getTitle(), discount, finalFare);
    }

    @Override
    @Transactional
    public void markVoucherUsedForTrip(UUID tripId) {
        redemptionRepository.findReservedByAppliedTripIdWithLock(tripId).ifPresent(redemption -> {
            // Even if expiresAt elapsed during the trip, reserved vouchers remain usable to completion!
            redemption.setStatus("USED");
            redemption.setUsedAt(Instant.now());
            redemptionRepository.save(redemption);
            log.info("Marked voucher {} USED for completed trip {}", redemption.getRedemptionCode(), tripId);
        });
    }

    @Override
    @Transactional
    public void releaseVoucherForCancelledTrip(UUID tripId) {
        redemptionRepository.findReservedByAppliedTripIdWithLock(tripId).ifPresent(redemption -> {
            boolean expired = redemption.getExpiresAt() != null && redemption.getExpiresAt().isBefore(Instant.now());
            redemption.setStatus(expired ? "EXPIRED" : "AVAILABLE");
            redemption.setAppliedTripId(null);
            redemption.setDiscountAppliedVnd(null);
            redemptionRepository.save(redemption);
            log.info("Released voucher {} after trip {} cancellation -> status={}",
                    redemption.getRedemptionCode(), tripId, redemption.getStatus());
        });
    }

    // --- Admin Catalog Management ---

    @Transactional
    public IncentiveDtos.RewardCatalogDto createReward(UUID adminId, String adminEmail,
                                                       IncentiveDtos.AdminRewardRequest req, String ipAddress) {
        Reward r = new Reward();
        applyRewardRequest(r, req);
        Reward saved = rewardRepository.save(r);
        auditLogService.record(adminId, adminEmail, "CREATE_REWARD", "Reward", saved.getId().toString(),
                null, Map.of("title", saved.getTitle(), "category", saved.getCategory(), "qty", saved.getQuantityAvailable()),
                ipAddress);
        return toCatalogDto(saved);
    }

    @Transactional
    public IncentiveDtos.RewardCatalogDto updateReward(UUID adminId, String adminEmail, UUID rewardId,
                                                       IncentiveDtos.AdminRewardRequest req, String ipAddress) {
        Reward r = rewardRepository.findByIdWithLock(rewardId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND", "Không tìm thấy quà tặng"));
        Map<String, Object> before = Map.of("title", r.getTitle(), "qty", r.getQuantityAvailable(), "active", r.getIsActive());
        applyRewardRequest(r, req);
        Reward saved = rewardRepository.save(r);
        auditLogService.record(adminId, adminEmail, "UPDATE_REWARD", "Reward", saved.getId().toString(),
                before, Map.of("title", saved.getTitle(), "qty", saved.getQuantityAvailable(), "active", saved.getIsActive()),
                ipAddress);
        return toCatalogDto(saved);
    }

    private void applyRewardRequest(Reward r, IncentiveDtos.AdminRewardRequest req) {
        r.setTitle(req.title().trim());
        r.setDescription(req.description());
        r.setPartnerName(req.partnerName().trim());
        r.setCategory(req.category().trim().toUpperCase());
        r.setCarbonCreditCost(req.carbonCreditCost());
        r.setLoyaltyPointCost(req.loyaltyPointCost());
        r.setImageUrl(req.imageUrl());
        r.setQuantityAvailable(req.quantityAvailable());
        r.setIsActive(req.isActive() != null ? req.isActive() : true);
        r.setDiscountType(req.discountType());
        r.setDiscountValue(req.discountValue());
        r.setMaxDiscountVnd(req.maxDiscountVnd());
        r.setMinFareVnd(req.minFareVnd() != null ? req.minFareVnd() : 0L);
        r.setValidityDays(req.validityDays() != null ? req.validityDays() : 30);
        r.setMaxRedemptionsPerUser(req.maxRedemptionsPerUser() != null ? req.maxRedemptionsPerUser() : 5);
    }

    private RewardRedemption resolveRedemptionReadOnly(UUID id, String code) {
        if (id != null) {
            return redemptionRepository.findById(id)
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "Không tìm thấy voucher"));
        }
        if (code != null && !code.isBlank()) {
            return redemptionRepository.findByRedemptionCode(code.trim().toUpperCase())
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "Mã voucher không tồn tại"));
        }
        throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "VOUCHER_ID_OR_CODE_REQUIRED", "Vui lòng cung cấp ID hoặc mã voucher");
    }

    private RewardRedemption resolveRedemptionLocked(UUID id, String code) {
        if (id != null) {
            return redemptionRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "Không tìm thấy voucher"));
        }
        if (code != null && !code.isBlank()) {
            return redemptionRepository.findByRedemptionCode(code.trim().toUpperCase())
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "Mã voucher không tồn tại"));
        }
        throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "VOUCHER_ID_OR_CODE_REQUIRED", "Vui lòng cung cấp ID hoặc mã voucher");
    }

    private Reward validateVoucherEligibility(RewardRedemption redemption, UUID customerId, BigDecimal grossFareVnd) {
        if (!redemption.getUserId().equals(customerId)) {
            throw new PaymentBusinessException(HttpStatus.FORBIDDEN, "VOUCHER_NOT_OWNED", "Voucher này không thuộc tài khoản của bạn");
        }
        if (!List.of("AVAILABLE", "ISSUED").contains(redemption.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "VOUCHER_NOT_AVAILABLE",
                    "Voucher đã được sử dụng, đang giữ cho chuyến khác hoặc đã hủy (" + redemption.getStatus() + ")");
        }
        if (redemption.getExpiresAt() != null && redemption.getExpiresAt().isBefore(Instant.now())) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "VOUCHER_EXPIRED", "Voucher đã hết hạn sử dụng");
        }
        Reward reward = rewardRepository.findById(redemption.getRewardId())
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "REWARD_NOT_FOUND", "Không tìm thấy thông tin ưu đãi"));
        if (!"VOUCHER_RIDE".equalsIgnoreCase(reward.getCategory())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "NOT_A_RIDE_VOUCHER", "Mã ưu đãi này không phải voucher giảm giá chuyến xe");
        }
        long minFare = reward.getMinFareVnd() != null ? reward.getMinFareVnd() : 0L;
        if (grossFareVnd.compareTo(BigDecimal.valueOf(minFare)) < 0) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "VOUCHER_MIN_FARE_NOT_MET",
                    String.format("Voucher yêu cầu cước chuyến đi tối thiểu %,dđ", minFare));
        }
        return reward;
    }

    private BigDecimal calculateDiscount(Reward reward, BigDecimal grossFareVnd) {
        if (grossFareVnd == null || grossFareVnd.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rawDiscount;
        if ("PERCENT".equalsIgnoreCase(reward.getDiscountType()) && reward.getDiscountValue() != null) {
            rawDiscount = grossFareVnd.multiply(BigDecimal.valueOf(reward.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            if (reward.getMaxDiscountVnd() != null && reward.getMaxDiscountVnd() > 0) {
                rawDiscount = rawDiscount.min(BigDecimal.valueOf(reward.getMaxDiscountVnd()));
            }
        } else if (reward.getDiscountValue() != null) {
            rawDiscount = BigDecimal.valueOf(reward.getDiscountValue());
        } else {
            rawDiscount = BigDecimal.ZERO;
        }
        return rawDiscount.min(grossFareVnd).max(BigDecimal.ZERO);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("GM-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = sb.toString();
            if (redemptionRepository.findByRedemptionCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "GM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public IncentiveDtos.RewardCatalogDto toCatalogDto(Reward r) {
        return new IncentiveDtos.RewardCatalogDto(
                r.getId(), r.getTitle(), r.getDescription(), r.getPartnerName(), r.getCategory(),
                r.getCarbonCreditCost(), r.getLoyaltyPointCost(), r.getImageUrl(),
                r.getQuantityAvailable(), Boolean.TRUE.equals(r.getIsActive()),
                r.getDiscountType(), r.getDiscountValue(), r.getMaxDiscountVnd(),
                r.getMinFareVnd(), r.getValidityDays(), r.getMaxRedemptionsPerUser(), r.getCreatedAt()
        );
    }

    public IncentiveDtos.RedemptionResponseDto toRedemptionDto(RewardRedemption rd, Reward r) {
        return new IncentiveDtos.RedemptionResponseDto(
                rd.getId(), rd.getRewardId(),
                r != null ? r.getTitle() : "",
                r != null ? r.getPartnerName() : "",
                r != null ? r.getCategory() : "",
                rd.getRedemptionCode(), rd.getCarbonSpent(), rd.getPointsSpent(),
                rd.getStatus(),
                r != null ? r.getDiscountType() : null,
                r != null ? r.getDiscountValue() : null,
                r != null ? r.getMaxDiscountVnd() : null,
                r != null ? r.getMinFareVnd() : null,
                rd.getAppliedTripId(), rd.getDiscountAppliedVnd(),
                rd.getExpiresAt(), rd.getUsedAt(), rd.getCancelledAt(),
                rd.getCertificateMetadata(), rd.getCreatedAt()
        );
    }
}

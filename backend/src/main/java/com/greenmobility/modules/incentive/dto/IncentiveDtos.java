package com.greenmobility.modules.incentive.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IncentiveDtos {

    private IncentiveDtos() {}

    public record IncentiveSummaryDto(
            BigDecimal pccBalance,
            boolean pccFrozen,
            String pccFrozenReason,
            int ecoPointsBalance,
            boolean pointsFrozen,
            BigDecimal monthlyPccConverted,
            BigDecimal monthlyPccConvertCap,
            BigDecimal pccConvertMin,
            long pccToVndRateVnd,
            boolean redemptionsFrozen,
            long availableVouchersCount,
            BigDecimal lifetimeCo2SavedGrams
    ) {}

    public record IncentiveLedgerEntryDto(
            UUID id,
            String currency,
            String entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String transactionCode,
            String transactionType,
            String description,
            UUID tripId,
            Instant createdAt
    ) {}

    public record LedgerPageResponse(
            List<IncentiveLedgerEntryDto> items,
            String nextCursor,
            boolean hasMore
    ) {}

    public record RewardCatalogDto(
            UUID id,
            String title,
            String description,
            String partnerName,
            String category,
            BigDecimal carbonCreditCost,
            int loyaltyPointCost,
            String imageUrl,
            int quantityAvailable,
            boolean isActive,
            String discountType,
            Long discountValue,
            Long maxDiscountVnd,
            Long minFareVnd,
            int validityDays,
            int maxRedemptionsPerUser,
            Instant createdAt
    ) {}

    public record RedemptionResponseDto(
            UUID id,
            UUID rewardId,
            String rewardTitle,
            String partnerName,
            String category,
            String redemptionCode,
            BigDecimal carbonSpent,
            int pointsSpent,
            String status,
            String discountType,
            Long discountValue,
            Long maxDiscountVnd,
            Long minFareVnd,
            UUID appliedTripId,
            Long discountAppliedVnd,
            Instant expiresAt,
            Instant usedAt,
            Instant cancelledAt,
            String certificateMetadata,
            Instant createdAt
    ) {}

    public record RedeemRewardRequest(
            @NotNull(message = "rewardId không được để trống")
            UUID rewardId
    ) {}

    public record ConvertPccRequest(
            @NotNull(message = "Số lượng PCC muốn quy đổi không được để trống")
            @DecimalMin(value = "0.0001", message = "Số lượng PCC phải lớn hơn 0")
            BigDecimal pccAmount
    ) {}

    public record ConvertPccResponse(
            UUID conversionId,
            BigDecimal pccAmount,
            long rateVndPerPcc,
            long vndCredited,
            String targetAccountType,
            BigDecimal remainingPccBalance,
            long newVndBalance,
            Instant createdAt
    ) {}

    public record TripAwardDto(
            UUID id,
            UUID tripId,
            UUID customerId,
            UUID driverUserId,
            BigDecimal co2SavedGrams,
            BigDecimal totalPcc,
            BigDecimal customerPcc,
            BigDecimal driverPcc,
            int customerPoints,
            int driverPoints,
            boolean carbonReady,
            boolean paymentReady,
            String status,
            String holdReason,
            UUID pccLedgerTxId,
            UUID pointsLedgerTxId,
            Instant creditedAt,
            UUID reviewedBy,
            Instant reviewedAt,
            String reviewNote,
            Instant createdAt
    ) {}

    public record AdminRewardRequest(
            @NotBlank(message = "Tên quà tặng không được để trống")
            String title,
            String description,
            @NotBlank(message = "Đối tác không được để trống")
            String partnerName,
            @NotBlank(message = "Danh mục không được để trống")
            String category,
            @NotNull @DecimalMin("0.0000")
            BigDecimal carbonCreditCost,
            @Min(0)
            int loyaltyPointCost,
            String imageUrl,
            @Min(0)
            int quantityAvailable,
            Boolean isActive,
            String discountType,
            Long discountValue,
            Long maxDiscountVnd,
            @Min(0)
            Long minFareVnd,
            @Min(1)
            Integer validityDays,
            @Min(1)
            Integer maxRedemptionsPerUser
    ) {}

    public record AdminReviewAwardRequest(
            @Size(max = 500)
            String note
    ) {}
}

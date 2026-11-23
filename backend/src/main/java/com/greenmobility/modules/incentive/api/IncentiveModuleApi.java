package com.greenmobility.modules.incentive.api;

import java.math.BigDecimal;
import java.util.UUID;

public interface IncentiveModuleApi {

    VoucherPreviewDto previewVoucher(UUID customerId, UUID voucherRedemptionId, String voucherCode, BigDecimal grossFareVnd);

    VoucherReservationDto reserveVoucherForTrip(UUID customerId, UUID tripId, UUID voucherRedemptionId, String voucherCode, BigDecimal grossFareVnd);

    void markVoucherUsedForTrip(UUID tripId);

    void releaseVoucherForCancelledTrip(UUID tripId);

    record VoucherPreviewDto(
            UUID voucherRedemptionId,
            String voucherCode,
            String title,
            BigDecimal grossFareVnd,
            BigDecimal discountAmountVnd,
            BigDecimal finalFareVnd
    ) {}

    record VoucherReservationDto(
            UUID voucherRedemptionId,
            String voucherCode,
            String title,
            BigDecimal discountAmountVnd,
            BigDecimal finalFareVnd
    ) {}
}

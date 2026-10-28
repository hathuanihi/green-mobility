package com.greenmobility.modules.payment.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CommissionService {

    public record CommissionSplit(
            long fareVnd,
            BigDecimal commissionRate,
            long commissionVnd,
            long driverAmountVnd
    ) {}

    public record RefundSplit(
            long refundVnd,
            long commissionRefundVnd,
            long driverRefundVnd
    ) {}

    /**
     * Compute platform commission and driver earnings from final fare.
     * Guaranteed: C + D == F (no fractional penny lost).
     */
    public CommissionSplit calculateSplit(BigDecimal fare, BigDecimal commissionRate) {
        if (fare == null || fare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fare must be non-negative");
        }
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 1");
        }

        BigDecimal f = fare.setScale(0, RoundingMode.HALF_UP);
        BigDecimal c = f.multiply(commissionRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal d = f.subtract(c);

        return new CommissionSplit(
                f.longValueExact(),
                commissionRate,
                c.longValueExact(),
                d.longValueExact()
        );
    }

    /**
     * Compute proportional refund split between platform and driver.
     * Guaranteed: commissionRefund + driverRefund == refundVnd.
     */
    public RefundSplit calculateRefundSplit(long refundAmountVnd, long originalFareVnd, long originalCommissionVnd) {
        if (refundAmountVnd <= 0 || originalFareVnd <= 0) {
            throw new IllegalArgumentException("Amounts must be positive");
        }
        if (refundAmountVnd > originalFareVnd) {
            throw new IllegalArgumentException("Refund amount cannot exceed original fare");
        }

        BigDecimal r = BigDecimal.valueOf(refundAmountVnd);
        BigDecimal f = BigDecimal.valueOf(originalFareVnd);
        BigDecimal c = BigDecimal.valueOf(originalCommissionVnd);

        BigDecimal cPrime = r.multiply(c).divide(f, 0, RoundingMode.HALF_UP);
        BigDecimal dPrime = r.subtract(cPrime);

        return new RefundSplit(
                refundAmountVnd,
                cPrime.longValueExact(),
                dPrime.longValueExact()
        );
    }
}


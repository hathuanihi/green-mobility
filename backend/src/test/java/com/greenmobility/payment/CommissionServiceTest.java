package com.greenmobility.payment;

import com.greenmobility.modules.payment.service.CommissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CommissionServiceTest {

    private CommissionService commissionService;

    @BeforeEach
    void setUp() {
        commissionService = new CommissionService();
    }

    @Test
    @DisplayName("R1 & R14: C + D == F (No fractional penny lost)")
    void testCommissionSplitAlwaysBalances() {
        BigDecimal fare = new BigDecimal("76000");
        BigDecimal rate = new BigDecimal("0.20");

        CommissionService.CommissionSplit split = commissionService.calculateSplit(fare, rate);

        assertEquals(76000L, split.fareVnd());
        assertEquals(15200L, split.commissionVnd());
        assertEquals(60800L, split.driverAmountVnd());
        assertEquals(split.fareVnd(), split.commissionVnd() + split.driverAmountVnd());
    }

    @ParameterizedTest
    @CsvSource({
            "10001, 0.20",
            "33333, 0.20",
            "99999, 0.23",
            "1, 0.20",
            "0, 0.20",
            "1500000, 0.15"
    })
    @DisplayName("Odd numbers and rates always guarantee C + D == F")
    void testVariousAmounts(long fare, String rateStr) {
        BigDecimal f = BigDecimal.valueOf(fare);
        BigDecimal r = new BigDecimal(rateStr);

        CommissionService.CommissionSplit split = commissionService.calculateSplit(f, r);
        assertEquals(fare, split.commissionVnd() + split.driverAmountVnd());
    }

    @Test
    @DisplayName("Proportional refund split preserves total refund: C' + D' == R")
    void testRefundSplit() {
        long refundAmount = 25000;
        long originalFare = 100000;
        long originalCommission = 20000;

        CommissionService.RefundSplit split = commissionService.calculateRefundSplit(refundAmount, originalFare, originalCommission);

        assertEquals(refundAmount, split.commissionRefundVnd() + split.driverRefundVnd());
        assertEquals(5000L, split.commissionRefundVnd());
        assertEquals(20000L, split.driverRefundVnd());
    }
}


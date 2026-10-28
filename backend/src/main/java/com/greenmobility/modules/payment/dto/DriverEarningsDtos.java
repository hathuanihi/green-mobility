package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DriverEarningsDtos {

    public record DailySeries(
            String date,
            long netVnd,
            int trips
    ) {}

    public record EarningsSummaryResponse(
            long grossFareVnd,
            long commissionVnd,
            long netEarningsVnd,
            int tripCount,
            long cashCollectedVnd,
            long onlineCollectedVnd,
            long avgPerTripVnd,
            List<DailySeries> series
    ) {}

    public record DriverTripEarningResponse(
            UUID tripId,
            String tripCode,
            long fareVnd,
            long commissionVnd,
            long netIncomeVnd,
            String paymentMethod,
            Instant completedAt
    ) {}

    public record CashEligibilityResponse(
            boolean cashTripsAllowed,
            long cashDebtVnd,
            long cashDebtLimitVnd
    ) {}
}


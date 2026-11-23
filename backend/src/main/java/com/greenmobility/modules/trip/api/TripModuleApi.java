package com.greenmobility.modules.trip.api;

import com.greenmobility.modules.trip.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TripModuleApi {

    Optional<TripSnapshotDto> getTrip(UUID tripId);

    void markPaymentStatus(UUID tripId, PaymentStatus status);

    UUID getDriverUserId(UUID driverProfileId);

    void recordTripCarbonImpact(UUID tripId, BigDecimal co2SavedGrams, BigDecimal pccCredits, int ecoPoints);
}

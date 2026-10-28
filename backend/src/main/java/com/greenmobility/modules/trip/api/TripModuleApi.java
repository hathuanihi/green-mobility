package com.greenmobility.modules.trip.api;

import com.greenmobility.modules.trip.entity.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

public interface TripModuleApi {

    Optional<TripSnapshotDto> getTrip(UUID tripId);

    void markPaymentStatus(UUID tripId, PaymentStatus status);

    UUID getDriverUserId(UUID driverProfileId);
}


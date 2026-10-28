package com.greenmobility.modules.trip.api;

import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TripSnapshotDto(
        UUID id,
        String tripCode,
        UUID customerId,
        UUID driverId,
        UUID driverUserId,
        BigDecimal finalAmount,
        String paymentMethod,
        PaymentStatus paymentStatus,
        TripStatus tripStatus
) {}


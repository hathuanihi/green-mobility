package com.greenmobility.modules.trip.api;

import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TripSnapshotDto(
        UUID id,
        String tripCode,
        UUID customerId,
        UUID driverId,
        UUID driverUserId,
        BigDecimal fareAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String paymentMethod,
        PaymentStatus paymentStatus,
        TripStatus tripStatus,
        String vehicleType,
        Integer actualDistanceM,
        Integer estimatedDistanceM,
        Instant completedAt
) {
    public TripSnapshotDto(
            UUID id,
            String tripCode,
            UUID customerId,
            UUID driverId,
            UUID driverUserId,
            BigDecimal finalAmount,
            String paymentMethod,
            PaymentStatus paymentStatus,
            TripStatus tripStatus
    ) {
        this(id, tripCode, customerId, driverId, driverUserId,
                finalAmount, BigDecimal.ZERO, finalAmount,
                paymentMethod, paymentStatus, tripStatus,
                "ELECTRIC_MOTORBIKE", null, null, null);
    }
}

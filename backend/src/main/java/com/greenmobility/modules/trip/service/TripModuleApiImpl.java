package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TripModuleApiImpl implements TripModuleApi {

    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;

    public TripModuleApiImpl(TripRepository tripRepository, DriverProfileRepository driverProfileRepository) {
        this.tripRepository = tripRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TripSnapshotDto> getTrip(UUID tripId) {
        return tripRepository.findById(tripId).map(this::mapToSnapshot);
    }

    @Override
    @Transactional
    public void markPaymentStatus(UUID tripId, PaymentStatus status) {
        tripRepository.findById(tripId).ifPresent(trip -> {
            trip.setPaymentStatus(status);
            tripRepository.save(trip);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getDriverUserId(UUID driverProfileId) {
        if (driverProfileId == null) return null;
        return driverProfileRepository.findById(driverProfileId)
                .map(DriverProfile::getUserId)
                .orElse(null);
    }

    private TripSnapshotDto mapToSnapshot(Trip trip) {
        UUID driverUserId = null;
        if (trip.getDriverId() != null) {
            driverUserId = driverProfileRepository.findById(trip.getDriverId())
                    .map(DriverProfile::getUserId)
                    .orElse(null);
        }

        return new TripSnapshotDto(
                trip.getId(),
                trip.getTripCode(),
                trip.getCustomerId(),
                trip.getDriverId(),
                driverUserId,
                trip.getFinalAmount(),
                trip.getPaymentMethod() != null ? trip.getPaymentMethod().name() : "CASH",
                trip.getPaymentStatus(),
                trip.getStatus()
        );
    }
}


package com.greenmobility.modules.carbon.service;

import com.greenmobility.modules.carbon.dto.*;
import com.greenmobility.modules.carbon.entity.EmissionFactor;
import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import com.greenmobility.modules.trip.event.TripCompletedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CarbonCalculationService {

    /**
     * Process trip completion event: calculates CO2 savings, creates TripImpactReceipt,
     * updates Trip entity, and publishes CarbonCalculatedEvent to RabbitMQ.
     */
    TripImpactReceipt processTripCompletion(TripCompletedEvent event);

    /**
     * Directly calculate and save trip impact by trip ID.
     */
    TripImpactReceipt calculateForTrip(UUID tripId);

    /**
     * Get Trip Impact Receipt by tripId.
     */
    TripImpactReceiptDto getReceiptByTripId(UUID tripId);

    /**
     * Get public Trip Impact Receipt by shareable slug (no auth needed).
     */
    TripImpactReceiptDto getReceiptByShareableSlug(String slug);

    /**
     * Get aggregated carbon reduction stats for a specific user.
     */
    UserCarbonSummaryDto getUserCarbonSummary(UUID userId);

    /**
     * Run sandbox carbon simulation for given distance and vehicle category.
     */
    CarbonSimulationResponse simulateCarbonSavings(CarbonSimulationRequest request);

    // Admin Emission Factor Management
    List<EmissionFactorDto> getAllEmissionFactors(String vehicleCategory, Boolean isActive);

    EmissionFactorDto getEmissionFactorById(UUID id);

    EmissionFactorDto createEmissionFactor(CreateEmissionFactorRequest request, UUID adminUserId);

    EmissionFactorDto updateEmissionFactor(UUID id, UpdateEmissionFactorRequest request);

    void toggleEmissionFactorStatus(UUID id, boolean active);
}

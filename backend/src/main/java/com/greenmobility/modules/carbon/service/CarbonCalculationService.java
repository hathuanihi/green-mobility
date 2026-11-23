package com.greenmobility.modules.carbon.service;

import com.greenmobility.modules.carbon.dto.*;
import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import com.greenmobility.modules.trip.event.TripCompletedEvent;

import java.util.List;
import java.util.UUID;

public interface CarbonCalculationService {

    TripImpactReceipt processTripCompletion(TripCompletedEvent event);

    TripImpactReceipt calculateForTrip(UUID tripId);

    TripImpactReceiptDto getReceiptByTripId(UUID tripId, UUID requesterId, String requesterRole);

    TripImpactReceiptDto getReceiptByShareableSlug(String slug);

    UserCarbonSummaryDto getUserCarbonSummary(UUID userId);

    CarbonSimulationResponse simulateCarbonSavings(CarbonSimulationRequest request);

    List<EmissionFactorDto> getAllEmissionFactors(String vehicleCategory, Boolean isActive);

    EmissionFactorDto getEmissionFactorById(UUID id);

    EmissionFactorDto createEmissionFactor(CreateEmissionFactorRequest request, UUID adminUserId);

    EmissionFactorDto updateEmissionFactor(UUID id, UpdateEmissionFactorRequest request);

    void toggleEmissionFactorStatus(UUID id, boolean active);
}

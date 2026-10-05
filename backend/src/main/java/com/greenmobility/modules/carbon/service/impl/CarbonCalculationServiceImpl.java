package com.greenmobility.modules.carbon.service.impl;

import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.carbon.dto.*;
import com.greenmobility.modules.carbon.entity.EmissionFactor;
import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import com.greenmobility.modules.carbon.event.CarbonCalculatedEvent;
import com.greenmobility.modules.carbon.repository.EmissionFactorRepository;
import com.greenmobility.modules.carbon.repository.TripImpactReceiptRepository;
import com.greenmobility.modules.carbon.service.CarbonCalculationService;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CarbonCalculationServiceImpl implements CarbonCalculationService {

    private static final Logger log = LoggerFactory.getLogger(CarbonCalculationServiceImpl.class);

    public static final String ROUTING_KEY_CARBON_CALCULATED = "carbon.event.calculated";

    // Standard environmental conversion factors
    private static final BigDecimal GRAMS_PER_TREE_DAY = BigDecimal.valueOf(60.0);
    private static final BigDecimal GRAMS_PER_LED_HOUR = BigDecimal.valueOf(7.221);
    private static final BigDecimal GRAMS_PER_PCC = BigDecimal.valueOf(1000000.0);
    private static final BigDecimal GRAMS_PER_ECO_POINT = BigDecimal.valueOf(100.0);

    private final EmissionFactorRepository emissionFactorRepository;
    private final TripImpactReceiptRepository receiptRepository;
    private final TripRepository tripRepository;
    private final RabbitTemplate rabbitTemplate;

    public CarbonCalculationServiceImpl(
            EmissionFactorRepository emissionFactorRepository,
            TripImpactReceiptRepository receiptRepository,
            TripRepository tripRepository,
            RabbitTemplate rabbitTemplate) {
        this.emissionFactorRepository = emissionFactorRepository;
        this.receiptRepository = receiptRepository;
        this.tripRepository = tripRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public TripImpactReceipt processTripCompletion(TripCompletedEvent event) {
        if (event == null || event.getTripId() == null) {
            log.warn("[CarbonEngine] Received null or empty trip completion event");
            return null;
        }

        UUID tripId = event.getTripId();
        log.info("[CarbonEngine] Processing carbon calculations for completed trip {}", tripId);

        // 1. Idempotency check: if receipt already exists, return it
        Optional<TripImpactReceipt> existing = receiptRepository.findByTripId(tripId);
        if (existing.isPresent()) {
            log.info("[CarbonEngine] TripImpactReceipt already exists for trip {}, returning existing", tripId);
            return existing.get();
        }

        // 2. Determine distance (actual or estimated)
        Integer distanceM = (event.getActualDistanceM() != null && event.getActualDistanceM() > 0)
                ? event.getActualDistanceM()
                : (event.getEstimatedDistanceM() != null ? event.getEstimatedDistanceM() : 0);

        String vehicleType = event.getVehicleType() != null ? event.getVehicleType() : "ELECTRIC_MOTORBIKE";
        LocalDate completionDate = event.getCompletedAt() != null
                ? event.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        return calculateAndSaveReceipt(tripId, event.getTripCode(), event.getCustomerId(), event.getDriverId(), vehicleType, distanceM, completionDate);
    }

    @Override
    @Transactional
    public TripImpactReceipt calculateForTrip(UUID tripId) {
        Optional<TripImpactReceipt> existing = receiptRepository.findByTripId(tripId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        Integer distanceM = (trip.getActualDistanceM() != null && trip.getActualDistanceM() > 0)
                ? trip.getActualDistanceM()
                : (trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 0);

        String vehicleType = trip.getVehicleType() != null ? trip.getVehicleType().name() : "ELECTRIC_MOTORBIKE";
        LocalDate completionDate = trip.getCompletedAt() != null
                ? trip.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        return calculateAndSaveReceipt(trip.getId(), trip.getTripCode(), trip.getCustomerId(), trip.getDriverId(), vehicleType, distanceM, completionDate);
    }

    private TripImpactReceipt calculateAndSaveReceipt(UUID tripId, String tripCode, UUID customerId, UUID driverId,
                                                     String vehicleType, Integer distanceM, LocalDate targetDate) {
        // 1. Find emission factor
        EmissionFactor factor = resolveActiveEmissionFactor(vehicleType, targetDate);

        // 2. Perform scientific carbon calculation
        BigDecimal distanceKm = BigDecimal.valueOf(distanceM)
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);

        BigDecimal baselineCo2Grams = distanceKm
                .multiply(factor.getBaselineGasolineFactorGco2Km())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal evEmittedCo2Grams = distanceKm
                .multiply(factor.getCalculatedEvFactorGco2Km())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawNetSaved = baselineCo2Grams.subtract(evEmittedCo2Grams);
        BigDecimal netSavedGrams = rawNetSaved.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rawNetSaved;

        BigDecimal treeDays = netSavedGrams.divide(GRAMS_PER_TREE_DAY, 2, RoundingMode.HALF_UP);
        BigDecimal ledHours = netSavedGrams.divide(GRAMS_PER_LED_HOUR, 2, RoundingMode.HALF_UP);
        BigDecimal pccCredits = netSavedGrams.divide(GRAMS_PER_PCC, 6, RoundingMode.HALF_UP);
        int ecoPoints = netSavedGrams.divide(GRAMS_PER_ECO_POINT, 0, RoundingMode.FLOOR).intValue();

        // 3. Generate unique shareable slug
        String shortCode = (tripCode != null && !tripCode.isBlank()) ? tripCode.toLowerCase().replace("#", "") : UUID.randomUUID().toString().substring(0, 8);
        String uniqueSlug = "eco-" + shortCode + "-" + UUID.randomUUID().toString().substring(0, 6);

        // 4. Create and persist TripImpactReceipt
        TripImpactReceipt receipt = new TripImpactReceipt();
        receipt.setTripId(tripId);
        receipt.setCo2SavedGrams(netSavedGrams);
        receipt.setBaselineGasolineCo2Grams(baselineCo2Grams);
        receipt.setEvEmittedCo2Grams(evEmittedCo2Grams);
        receipt.setTreeAbsorptionDaysEquiv(treeDays);
        receipt.setLedBulbHoursEquiv(ledHours);
        receipt.setShareableSlug(uniqueSlug);
        receipt.setCreatedAt(Instant.now());

        TripImpactReceipt savedReceipt = receiptRepository.save(receipt);
        log.info("[CarbonEngine] Created TripImpactReceipt {} for trip {}: saved {}g CO2",
                savedReceipt.getId(), tripId, netSavedGrams);

        // 5. Update Trip entity with carbon fields
        tripRepository.findById(tripId).ifPresent(t -> {
            t.setCo2SavedGrams(netSavedGrams);
            t.setCarbonCreditsEarned(pccCredits);
            t.setLoyaltyPointsEarned(ecoPoints);
            tripRepository.save(t);
            log.info("[CarbonEngine] Updated Trip {} with co2SavedGrams={}", tripId, netSavedGrams);
        });

        // 6. Publish CarbonCalculatedEvent to RabbitMQ
        try {
            CarbonCalculatedEvent calcEvent = new CarbonCalculatedEvent(
                    tripId, tripCode, customerId, driverId,
                    netSavedGrams, baselineCo2Grams, evEmittedCo2Grams,
                    pccCredits, ecoPoints, uniqueSlug, Instant.now()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, ROUTING_KEY_CARBON_CALCULATED, calcEvent);
            log.info("[CarbonEngine] Published CarbonCalculatedEvent for trip {} with routing key {}",
                    tripId, ROUTING_KEY_CARBON_CALCULATED);
        } catch (Exception ex) {
            log.warn("[CarbonEngine] Failed to publish CarbonCalculatedEvent to RabbitMQ (non-fatal): {}", ex.getMessage());
        }

        return savedReceipt;
    }

    private EmissionFactor resolveActiveEmissionFactor(String vehicleType, LocalDate targetDate) {
        // Normalize categories: ELECTRIC_MOTORBIKE -> MOTORBIKE or ELECTRIC_MOTORBIKE
        String normalized = vehicleType.trim().toUpperCase();
        Optional<EmissionFactor> factorOpt = emissionFactorRepository.findActiveFactor(normalized, targetDate);

        if (factorOpt.isEmpty()) {
            if (normalized.contains("MOTORBIKE") || normalized.contains("BIKE")) {
                factorOpt = emissionFactorRepository.findActiveFactor("ELECTRIC_MOTORBIKE", targetDate)
                        .or(() -> emissionFactorRepository.findActiveFactor("MOTORBIKE", targetDate));
            } else if (normalized.contains("7SEAT") || normalized.contains("7_SEATS")) {
                factorOpt = emissionFactorRepository.findActiveFactor("ELECTRIC_CAR_7SEAT", targetDate)
                        .or(() -> emissionFactorRepository.findActiveFactor("CAR_7SEATS", targetDate));
            } else {
                factorOpt = emissionFactorRepository.findActiveFactor("ELECTRIC_CAR_4SEAT", targetDate)
                        .or(() -> emissionFactorRepository.findActiveFactor("CAR_4SEATS", targetDate));
            }
        }

        return factorOpt.orElseGet(this::createFallbackEmissionFactor);
    }

    private EmissionFactor createFallbackEmissionFactor() {
        log.warn("[CarbonEngine] No emission factor found in DB, using standard IPCC default fallback");
        EmissionFactor fallback = new EmissionFactor();
        fallback.setVehicleCategory("ELECTRIC_MOTORBIKE");
        fallback.setBaselineGasolineFactorGco2Km(BigDecimal.valueOf(70.00));
        fallback.setEvEnergyConsumptionKwhKm(BigDecimal.valueOf(0.0250));
        fallback.setGridEmissionFactorGco2Kwh(BigDecimal.valueOf(580.00));
        fallback.setCalculatedEvFactorGco2Km(BigDecimal.valueOf(14.50));
        fallback.setNetCo2SavingPerKm(BigDecimal.valueOf(55.50));
        fallback.setRegion("VIETNAM_NATIONAL");
        fallback.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        fallback.setIsActive(true);
        return fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public TripImpactReceiptDto getReceiptByTripId(UUID tripId) {
        TripImpactReceipt receipt = receiptRepository.findByTripId(tripId)
                .orElseGet(() -> calculateForTrip(tripId));

        Trip trip = tripRepository.findById(tripId).orElse(null);
        String tripCode = trip != null ? trip.getTripCode() : null;
        String vehicleType = trip != null && trip.getVehicleType() != null ? trip.getVehicleType().name() : "ELECTRIC_MOTORBIKE";
        Integer distanceM = trip != null ? (trip.getActualDistanceM() != null ? trip.getActualDistanceM() : trip.getEstimatedDistanceM()) : null;
        BigDecimal credits = trip != null ? trip.getCarbonCreditsEarned() : null;
        Integer points = trip != null ? trip.getLoyaltyPointsEarned() : null;

        return TripImpactReceiptDto.fromEntity(receipt, tripCode, vehicleType, distanceM, credits, points);
    }

    @Override
    @Transactional(readOnly = true)
    public TripImpactReceiptDto getReceiptByShareableSlug(String slug) {
        TripImpactReceipt receipt = receiptRepository.findByShareableSlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found for slug: " + slug));

        Trip trip = tripRepository.findById(receipt.getTripId()).orElse(null);
        String tripCode = trip != null ? trip.getTripCode() : null;
        String vehicleType = trip != null && trip.getVehicleType() != null ? trip.getVehicleType().name() : "ELECTRIC_MOTORBIKE";
        Integer distanceM = trip != null ? (trip.getActualDistanceM() != null ? trip.getActualDistanceM() : trip.getEstimatedDistanceM()) : null;
        BigDecimal credits = trip != null ? trip.getCarbonCreditsEarned() : null;
        Integer points = trip != null ? trip.getLoyaltyPointsEarned() : null;

        return TripImpactReceiptDto.fromEntity(receipt, tripCode, vehicleType, distanceM, credits, points);
    }

    @Override
    @Transactional(readOnly = true)
    public UserCarbonSummaryDto getUserCarbonSummary(UUID userId) {
        BigDecimal totalGrams = receiptRepository.sumCo2SavedByCustomerId(userId);
        Long totalTrips = receiptRepository.countTripsByCustomerId(userId);
        return UserCarbonSummaryDto.of(userId, totalGrams, totalTrips);
    }

    @Override
    @Transactional(readOnly = true)
    public CarbonSimulationResponse simulateCarbonSavings(CarbonSimulationRequest request) {
        if (request == null || request.getDistanceKm() == null || request.getVehicleCategory() == null) {
            throw new IllegalArgumentException("Invalid simulation request parameters");
        }

        EmissionFactor factor = resolveActiveEmissionFactor(request.getVehicleCategory(), LocalDate.now());
        BigDecimal km = request.getDistanceKm();

        BigDecimal baselineGasolineGrams = km.multiply(factor.getBaselineGasolineFactorGco2Km()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal evEmittedGrams = km.multiply(factor.getCalculatedEvFactorGco2Km()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSavedGrams = baselineGasolineGrams.subtract(evEmittedGrams);
        if (netSavedGrams.compareTo(BigDecimal.ZERO) < 0) {
            netSavedGrams = BigDecimal.ZERO;
        }

        CarbonSimulationResponse res = new CarbonSimulationResponse();
        res.setVehicleCategory(factor.getVehicleCategory());
        res.setDistanceKm(km);
        res.setBaselineGasolineFactorGco2Km(factor.getBaselineGasolineFactorGco2Km());
        res.setEvEnergyConsumptionKwhKm(factor.getEvEnergyConsumptionKwhKm());
        res.setGridEmissionFactorGco2Kwh(factor.getGridEmissionFactorGco2Kwh());
        res.setCalculatedEvFactorGco2Km(factor.getCalculatedEvFactorGco2Km());
        res.setNetCo2SavingPerKm(factor.getNetCo2SavingPerKm());
        res.setBaselineGasolineCo2Grams(baselineGasolineGrams);
        res.setEvEmittedCo2Grams(evEmittedGrams);
        res.setNetCo2SavedGrams(netSavedGrams);
        res.setNetCo2SavedKg(netSavedGrams.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP));
        res.setTreeAbsorptionDaysEquiv(netSavedGrams.divide(GRAMS_PER_TREE_DAY, 2, RoundingMode.HALF_UP));
        res.setLedBulbHoursEquiv(netSavedGrams.divide(GRAMS_PER_LED_HOUR, 2, RoundingMode.HALF_UP));
        res.setSmartphoneChargesEquiv(netSavedGrams.divide(BigDecimal.valueOf(8.22), 1, RoundingMode.HALF_UP));
        res.setCarbonCreditsEarned(netSavedGrams.divide(GRAMS_PER_PCC, 6, RoundingMode.HALF_UP));
        res.setEcoPoints(netSavedGrams.divide(GRAMS_PER_ECO_POINT, 0, RoundingMode.FLOOR).intValue());

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmissionFactorDto> getAllEmissionFactors(String vehicleCategory, Boolean isActive) {
        List<EmissionFactor> list;
        if (vehicleCategory != null && !vehicleCategory.isBlank()) {
            list = emissionFactorRepository.findByVehicleCategory(vehicleCategory.trim().toUpperCase());
        } else if (Boolean.TRUE.equals(isActive)) {
            list = emissionFactorRepository.findByIsActiveTrue();
        } else {
            list = emissionFactorRepository.findAll();
        }

        return list.stream().map(EmissionFactorDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmissionFactorDto getEmissionFactorById(UUID id) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EmissionFactor not found: " + id));
        return EmissionFactorDto.fromEntity(factor);
    }

    @Override
    @Transactional
    public EmissionFactorDto createEmissionFactor(CreateEmissionFactorRequest request, UUID adminUserId) {
        EmissionFactor factor = new EmissionFactor();
        factor.setVehicleCategory(request.getVehicleCategory().trim().toUpperCase());
        factor.setBaselineGasolineFactorGco2Km(request.getBaselineGasolineFactorGco2Km());
        factor.setEvEnergyConsumptionKwhKm(request.getEvEnergyConsumptionKwhKm());
        factor.setGridEmissionFactorGco2Kwh(request.getGridEmissionFactorGco2Kwh());
        factor.setRegion(request.getRegion() != null ? request.getRegion() : "VIETNAM_NATIONAL");
        factor.setEffectiveFrom(request.getEffectiveFrom());
        factor.setEffectiveTo(request.getEffectiveTo());
        factor.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        factor.setCreatedBy(adminUserId);
        factor.setCreatedAt(Instant.now());

        // Automatically calculate dependent variables
        factor.recalculate();

        EmissionFactor saved = emissionFactorRepository.save(factor);
        log.info("[CarbonEngine] Admin {} created new EmissionFactor {} for {}", adminUserId, saved.getId(), saved.getVehicleCategory());
        return EmissionFactorDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public EmissionFactorDto updateEmissionFactor(UUID id, UpdateEmissionFactorRequest request) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EmissionFactor not found: " + id));

        if (request.getVehicleCategory() != null) factor.setVehicleCategory(request.getVehicleCategory().trim().toUpperCase());
        if (request.getBaselineGasolineFactorGco2Km() != null) factor.setBaselineGasolineFactorGco2Km(request.getBaselineGasolineFactorGco2Km());
        if (request.getEvEnergyConsumptionKwhKm() != null) factor.setEvEnergyConsumptionKwhKm(request.getEvEnergyConsumptionKwhKm());
        if (request.getGridEmissionFactorGco2Kwh() != null) factor.setGridEmissionFactorGco2Kwh(request.getGridEmissionFactorGco2Kwh());
        if (request.getRegion() != null) factor.setRegion(request.getRegion());
        if (request.getEffectiveFrom() != null) factor.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) factor.setEffectiveTo(request.getEffectiveTo());
        if (request.getIsActive() != null) factor.setIsActive(request.getIsActive());

        factor.recalculate();
        EmissionFactor updated = emissionFactorRepository.save(factor);
        log.info("[CarbonEngine] Updated EmissionFactor {}", updated.getId());
        return EmissionFactorDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void toggleEmissionFactorStatus(UUID id, boolean active) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("EmissionFactor not found: " + id));
        factor.setIsActive(active);
        emissionFactorRepository.save(factor);
        log.info("[CarbonEngine] Toggled EmissionFactor {} status to {}", id, active);
    }
}

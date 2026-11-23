package com.greenmobility.modules.carbon.service.impl;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.carbon.api.CarbonModuleApi;
import com.greenmobility.modules.carbon.dto.*;
import com.greenmobility.modules.carbon.entity.EmissionFactor;
import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import com.greenmobility.modules.carbon.event.CarbonCalculatedEvent;
import com.greenmobility.modules.carbon.repository.EmissionFactorRepository;
import com.greenmobility.modules.carbon.repository.TripImpactReceiptRepository;
import com.greenmobility.modules.carbon.service.CarbonCalculationService;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CarbonCalculationServiceImpl implements CarbonCalculationService, CarbonModuleApi {

    private static final Logger log = LoggerFactory.getLogger(CarbonCalculationServiceImpl.class);
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final BigDecimal GRAMS_PER_TREE_DAY = BigDecimal.valueOf(60.0);
    private static final BigDecimal LED_KW_PER_HOUR = new BigDecimal("0.01");

    private final EmissionFactorRepository emissionFactorRepository;
    private final TripImpactReceiptRepository receiptRepository;
    private final TripModuleApi tripModuleApi;
    private final PlatformSettingsService settingsService;
    private final OutboxService outboxService;

    public CarbonCalculationServiceImpl(
            EmissionFactorRepository emissionFactorRepository,
            TripImpactReceiptRepository receiptRepository,
            TripModuleApi tripModuleApi,
            PlatformSettingsService settingsService,
            OutboxService outboxService) {
        this.emissionFactorRepository = emissionFactorRepository;
        this.receiptRepository = receiptRepository;
        this.tripModuleApi = tripModuleApi;
        this.settingsService = settingsService;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public TripImpactReceipt processTripCompletion(TripCompletedEvent event) {
        if (event == null || event.getTripId() == null) {
            log.warn("[CarbonEngine] Received null or empty trip completion event");
            return null;
        }

        UUID tripId = event.getTripId();
        Optional<TripImpactReceipt> existing = receiptRepository.findByTripId(tripId);
        if (existing.isPresent()) {
            log.info("[CarbonEngine] TripImpactReceipt already exists for trip {}, returning existing", tripId);
            return existing.get();
        }

        Integer distanceM = (event.getActualDistanceM() != null && event.getActualDistanceM() > 0)
                ? event.getActualDistanceM()
                : (event.getEstimatedDistanceM() != null ? event.getEstimatedDistanceM() : 0);

        String vehicleType = event.getVehicleType() != null ? event.getVehicleType() : "ELECTRIC_MOTORBIKE";
        LocalDate completionDate = event.getCompletedAt() != null
                ? event.getCompletedAt().atZone(VN_ZONE).toLocalDate()
                : LocalDate.now(VN_ZONE);

        UUID driverUserId = tripModuleApi.getDriverUserId(event.getDriverId());
        return calculateAndSaveReceipt(
                tripId, event.getTripCode(), event.getCustomerId(),
                event.getDriverId(), driverUserId, vehicleType, distanceM, completionDate
        );
    }

    @Override
    @Transactional
    public TripImpactReceipt calculateForTrip(UUID tripId) {
        Optional<TripImpactReceipt> existing = receiptRepository.findByTripId(tripId);
        if (existing.isPresent()) {
            return existing.get();
        }

        TripSnapshotDto trip = tripModuleApi.getTrip(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi: " + tripId));

        Integer distanceM = (trip.actualDistanceM() != null && trip.actualDistanceM() > 0)
                ? trip.actualDistanceM()
                : (trip.estimatedDistanceM() != null ? trip.estimatedDistanceM() : 0);

        String vehicleType = trip.vehicleType() != null ? trip.vehicleType() : "ELECTRIC_MOTORBIKE";
        LocalDate completionDate = trip.completedAt() != null
                ? trip.completedAt().atZone(VN_ZONE).toLocalDate()
                : LocalDate.now(VN_ZONE);

        return calculateAndSaveReceipt(
                trip.id(), trip.tripCode(), trip.customerId(),
                trip.driverId(), trip.driverUserId(), vehicleType, distanceM, completionDate
        );
    }

    private TripImpactReceipt calculateAndSaveReceipt(UUID tripId, String tripCode, UUID customerId,
                                                      UUID driverId, UUID driverUserId,
                                                      String vehicleType, Integer distanceM, LocalDate targetDate) {
        EmissionFactor factor = resolveActiveEmissionFactor(vehicleType, targetDate);

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

        BigDecimal ledHourlyGrams = factor.getGridEmissionFactorGco2Kwh().multiply(LED_KW_PER_HOUR);
        BigDecimal treeDays = netSavedGrams.divide(GRAMS_PER_TREE_DAY, 2, RoundingMode.HALF_UP);
        BigDecimal ledHours = ledHourlyGrams.compareTo(BigDecimal.ZERO) > 0
                ? netSavedGrams.divide(ledHourlyGrams, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal pccGramsPerUnit = settingsService.getPccGramsPerUnit();
        BigDecimal pccCredits = netSavedGrams.divide(pccGramsPerUnit, 4, RoundingMode.HALF_UP);

        BigDecimal ecoPointGramsPerPoint = settingsService.getEcoPointGramsPerPoint();
        int ecoPoints = netSavedGrams.divide(ecoPointGramsPerPoint, 0, RoundingMode.FLOOR).intValue();
        if (netSavedGrams.compareTo(BigDecimal.ZERO) > 0 && ecoPoints < 1) {
            ecoPoints = 1;
        }

        String shortCode = (tripCode != null && !tripCode.isBlank())
                ? tripCode.toLowerCase().replace("#", "")
                : UUID.randomUUID().toString().substring(0, 8);
        String uniqueSlug = "eco-" + shortCode + "-" + UUID.randomUUID().toString().substring(0, 6);

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

        // Update trip carbon metrics cleanly via TripModuleApi
        tripModuleApi.recordTripCarbonImpact(tripId, netSavedGrams, pccCredits, ecoPoints);

        // Emit CarbonCalculatedEvent via Transactional Outbox (R8)
        CarbonCalculatedEvent calcEvent = new CarbonCalculatedEvent(
                tripId, tripCode, customerId, driverId, driverUserId,
                netSavedGrams, baselineCo2Grams, evEmittedCo2Grams,
                pccCredits, ecoPoints, uniqueSlug, Instant.now()
        );
        outboxService.recordEvent("CarbonReceipt", savedReceipt.getId().toString(),
                RabbitMQConfig.ROUTING_KEY_CARBON_CALCULATED, calcEvent);

        log.info("[CarbonEngine] Saved receipt {} for trip {}: {}g CO2, {} PCC, {} EcoPoints",
                savedReceipt.getId(), tripId, netSavedGrams, pccCredits, ecoPoints);
        return savedReceipt;
    }

    @Override
    @Transactional(readOnly = true)
    public EmissionFactorSnapshot getActiveFactor(String vehicleType, LocalDate targetDate) {
        EmissionFactor factor = resolveActiveEmissionFactor(
                vehicleType != null ? vehicleType : "ELECTRIC_MOTORBIKE",
                targetDate != null ? targetDate : LocalDate.now(VN_ZONE)
        );
        return new EmissionFactorSnapshot(
                factor.getVehicleCategory(),
                factor.getBaselineGasolineFactorGco2Km(),
                factor.getEvEnergyConsumptionKwhKm(),
                factor.getGridEmissionFactorGco2Kwh(),
                factor.getCalculatedEvFactorGco2Km(),
                factor.getNetCo2SavingPerKm()
        );
    }

    private EmissionFactor resolveActiveEmissionFactor(String vehicleType, LocalDate targetDate) {
        String normalized = vehicleType != null ? vehicleType.trim().toUpperCase() : "ELECTRIC_MOTORBIKE";
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

        return factorOpt.orElseGet(() -> createFallbackEmissionFactor(normalized));
    }

    private EmissionFactor createFallbackEmissionFactor(String category) {
        log.warn("[CarbonEngine] No emission factor found in DB for {}, using 2024 CV 11232/BNNMT-BĐKH fallback", category);
        EmissionFactor fallback = new EmissionFactor();
        if (category.contains("7SEAT") || category.contains("7_SEATS")) {
            fallback.setVehicleCategory("ELECTRIC_CAR_7SEAT");
            fallback.setBaselineGasolineFactorGco2Km(new BigDecimal("210.00"));
            fallback.setEvEnergyConsumptionKwhKm(new BigDecimal("0.1800"));
        } else if (category.contains("4SEAT") || category.contains("4_SEATS")) {
            fallback.setVehicleCategory("ELECTRIC_CAR_4SEAT");
            fallback.setBaselineGasolineFactorGco2Km(new BigDecimal("152.00"));
            fallback.setEvEnergyConsumptionKwhKm(new BigDecimal("0.1350"));
        } else {
            fallback.setVehicleCategory("ELECTRIC_MOTORBIKE");
            fallback.setBaselineGasolineFactorGco2Km(new BigDecimal("68.50"));
            fallback.setEvEnergyConsumptionKwhKm(new BigDecimal("0.0320"));
        }
        fallback.setGridEmissionFactorGco2Kwh(new BigDecimal("618.70"));
        fallback.setRegion("VIETNAM_NATIONAL");
        fallback.setEffectiveFrom(LocalDate.of(2026, 10, 26));
        fallback.setIsActive(true);
        fallback.recalculate();
        return fallback;
    }

    @Override
    @Transactional
    public TripImpactReceiptDto getReceiptByTripId(UUID tripId, UUID requesterId, String requesterRole) {
        TripSnapshotDto trip = tripModuleApi.getTrip(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi: " + tripId));

        boolean isAdmin = requesterRole != null && (requesterRole.contains("ADMIN") || requesterRole.contains("OPERATOR") || requesterRole.contains("FINANCE"));
        boolean isOwner = requesterId != null && (requesterId.equals(trip.customerId()) || requesterId.equals(trip.driverUserId()));
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Bạn không có quyền xem hóa đơn tác động xanh của chuyến đi này");
        }

        TripImpactReceipt receipt = receiptRepository.findByTripId(tripId)
                .orElseGet(() -> calculateForTrip(tripId));

        Integer distanceM = trip.actualDistanceM() != null ? trip.actualDistanceM() : trip.estimatedDistanceM();
        BigDecimal pcc = receipt.getCo2SavedGrams().divide(settingsService.getPccGramsPerUnit(), 4, RoundingMode.HALF_UP);
        int points = receipt.getCo2SavedGrams().divide(settingsService.getEcoPointGramsPerPoint(), 0, RoundingMode.FLOOR).intValue();
        if (receipt.getCo2SavedGrams().compareTo(BigDecimal.ZERO) > 0 && points < 1) {
            points = 1;
        }

        return TripImpactReceiptDto.fromEntity(receipt, trip.tripCode(), trip.vehicleType(), distanceM, pcc, points);
    }

    @Override
    @Transactional(readOnly = true)
    public TripImpactReceiptDto getReceiptByShareableSlug(String slug) {
        TripImpactReceipt receipt = receiptRepository.findByShareableSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng nhận xanh với mã: " + slug));

        TripSnapshotDto trip = tripModuleApi.getTrip(receipt.getTripId()).orElse(null);
        String tripCode = trip != null ? trip.tripCode() : null;
        String vehicleType = trip != null ? trip.vehicleType() : "ELECTRIC_MOTORBIKE";
        Integer distanceM = trip != null ? (trip.actualDistanceM() != null ? trip.actualDistanceM() : trip.estimatedDistanceM()) : null;

        BigDecimal pcc = receipt.getCo2SavedGrams().divide(settingsService.getPccGramsPerUnit(), 4, RoundingMode.HALF_UP);
        int points = receipt.getCo2SavedGrams().divide(settingsService.getEcoPointGramsPerPoint(), 0, RoundingMode.FLOOR).intValue();
        if (receipt.getCo2SavedGrams().compareTo(BigDecimal.ZERO) > 0 && points < 1) {
            points = 1;
        }

        return TripImpactReceiptDto.fromEntity(receipt, tripCode, vehicleType, distanceM, pcc, points);
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
            throw new BadRequestException("Tham số mô phỏng tính toán phát thải không hợp lệ");
        }

        EmissionFactor factor = resolveActiveEmissionFactor(request.getVehicleCategory(), LocalDate.now(VN_ZONE));
        BigDecimal km = request.getDistanceKm();

        BigDecimal baselineGasolineGrams = km.multiply(factor.getBaselineGasolineFactorGco2Km()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal evEmittedGrams = km.multiply(factor.getCalculatedEvFactorGco2Km()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSavedGrams = baselineGasolineGrams.subtract(evEmittedGrams);
        if (netSavedGrams.compareTo(BigDecimal.ZERO) < 0) {
            netSavedGrams = BigDecimal.ZERO;
        }

        BigDecimal ledHourlyGrams = factor.getGridEmissionFactorGco2Kwh().multiply(LED_KW_PER_HOUR);
        BigDecimal pcc = netSavedGrams.divide(settingsService.getPccGramsPerUnit(), 4, RoundingMode.HALF_UP);
        int ecoPoints = netSavedGrams.divide(settingsService.getEcoPointGramsPerPoint(), 0, RoundingMode.FLOOR).intValue();
        if (netSavedGrams.compareTo(BigDecimal.ZERO) > 0 && ecoPoints < 1) {
            ecoPoints = 1;
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
        res.setLedBulbHoursEquiv(ledHourlyGrams.compareTo(BigDecimal.ZERO) > 0
                ? netSavedGrams.divide(ledHourlyGrams, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        res.setSmartphoneChargesEquiv(netSavedGrams.divide(BigDecimal.valueOf(8.22), 1, RoundingMode.HALF_UP));
        res.setCarbonCreditsEarned(pcc);
        res.setEcoPoints(ecoPoints);

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmissionFactorDto> getAllEmissionFactors(String vehicleCategory, Boolean isActive) {
        List<EmissionFactor> list;
        boolean hasCat = vehicleCategory != null && !vehicleCategory.isBlank();
        if (hasCat && isActive != null) {
            list = emissionFactorRepository.findByVehicleCategoryAndIsActive(vehicleCategory.trim().toUpperCase(), isActive);
        } else if (hasCat) {
            list = emissionFactorRepository.findByVehicleCategory(vehicleCategory.trim().toUpperCase());
        } else if (isActive != null) {
            list = emissionFactorRepository.findByIsActive(isActive);
        } else {
            list = emissionFactorRepository.findAll();
        }
        return list.stream().map(EmissionFactorDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmissionFactorDto getEmissionFactorById(UUID id) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hệ số phát thải: " + id));
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
        factor.recalculate();

        EmissionFactor saved = emissionFactorRepository.save(factor);
        return EmissionFactorDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public EmissionFactorDto updateEmissionFactor(UUID id, UpdateEmissionFactorRequest request) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hệ số phát thải: " + id));

        if (request.getVehicleCategory() != null) factor.setVehicleCategory(request.getVehicleCategory().trim().toUpperCase());
        if (request.getBaselineGasolineFactorGco2Km() != null) factor.setBaselineGasolineFactorGco2Km(request.getBaselineGasolineFactorGco2Km());
        if (request.getEvEnergyConsumptionKwhKm() != null) factor.setEvEnergyConsumptionKwhKm(request.getEvEnergyConsumptionKwhKm());
        if (request.getGridEmissionFactorGco2Kwh() != null) factor.setGridEmissionFactorGco2Kwh(request.getGridEmissionFactorGco2Kwh());
        if (request.getRegion() != null) factor.setRegion(request.getRegion());
        if (request.getEffectiveFrom() != null) factor.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) factor.setEffectiveTo(request.getEffectiveTo());
        if (request.getIsActive() != null) factor.setIsActive(request.getIsActive());

        factor.recalculate();
        return EmissionFactorDto.fromEntity(emissionFactorRepository.save(factor));
    }

    @Override
    @Transactional
    public void toggleEmissionFactorStatus(UUID id, boolean active) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hệ số phát thải: " + id));
        factor.setIsActive(active);
        emissionFactorRepository.save(factor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getReceiptCo2SavedGrams(UUID tripId) {
        return receiptRepository.findByTripId(tripId).map(TripImpactReceipt::getCo2SavedGrams);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCustomerLifetimeCo2SavedGrams(UUID customerId) {
        BigDecimal total = receiptRepository.sumCo2SavedByCustomerId(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }
}

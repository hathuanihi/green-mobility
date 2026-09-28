package com.greenmobility.modules.matching.service;

import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.KycStatus;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.trip.dto.DispatchNotificationDto;
import com.greenmobility.modules.trip.dto.TripStatusUpdateDto;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.event.TripRequestedEvent;
import com.greenmobility.modules.trip.repository.TripRepository;
import com.greenmobility.modules.trip.service.FareCalculationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MatchingEngineService {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineService.class);

    @Value("${green-mobility.matching.tier1-radius-km:1.5}")
    private double tier1RadiusKm;

    @Value("${green-mobility.matching.tier2-radius-km:3.0}")
    private double tier2RadiusKm;

    @Value("${green-mobility.matching.tier3-radius-km:5.0}")
    private double tier3RadiusKm;

    @Value("${green-mobility.matching.driver-response-timeout-seconds:15}")
    private int responseTimeoutSeconds;

    private final DriverGeoRedisRepository driverGeoRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final FareCalculationService fareCalculationService;

    public MatchingEngineService(DriverGeoRedisRepository driverGeoRepository,
                                 DriverProfileRepository driverProfileRepository,
                                 VehicleRepository vehicleRepository,
                                 TripRepository tripRepository,
                                 RedissonClient redissonClient,
                                 SimpMessagingTemplate messagingTemplate,
                                 FareCalculationService fareCalculationService) {
        this.driverGeoRepository = driverGeoRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.redissonClient = redissonClient;
        this.messagingTemplate = messagingTemplate;
        this.fareCalculationService = fareCalculationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRIP_MATCHING_REQUESTS)
    public void handleTripRequested(TripRequestedEvent event) {
        log.info("Received TripRequestedEvent from RabbitMQ: tripId={}, code={}", event.getTripId(), event.getTripCode());
        matchTripAsync(event.getTripId());
    }

    @Async
    public void matchTripAsync(UUID tripId) {
        log.info("Starting matching engine workflow for tripId: {}", tripId);

        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            log.error("Trip not found for matching: {}", tripId);
            return;
        }

        Trip trip = tripOpt.get();
        if (trip.getStatus() != TripStatus.REQUESTED && trip.getStatus() != TripStatus.SEARCHING) {
            log.warn("Trip {} is not in REQUESTED/SEARCHING state, current: {}", tripId, trip.getStatus());
            return;
        }

        // Ensure status is SEARCHING
        trip.setStatus(TripStatus.SEARCHING);
        tripRepository.save(trip);

        double pickupLat = trip.getPickupLat();
        double pickupLng = trip.getPickupLng();
        double tripDistanceKm = (trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 1000) / 1000.0;
        int minBatteryRequired = tripDistanceKm > 15.0 ? 35 : 20;

        double[] tiers = new double[]{tier1RadiusKm, tier2RadiusKm, tier3RadiusKm};
        boolean matched = false;

        for (int tierIdx = 0; tierIdx < tiers.length; tierIdx++) {
            double radiusKm = tiers[tierIdx];
            log.info("Scanning Tier {} (radius {} km) for tripId: {}", tierIdx + 1, radiusKm, tripId);

            List<DriverGeoRedisRepository.DriverCandidateLocation> candidates =
                    driverGeoRepository.searchNearby(trip.getVehicleType(), pickupLat, pickupLng, radiusKm);

            if (candidates.isEmpty()) {
                continue;
            }

            // Score and sort candidates
            List<ScoredDriver> scoredDrivers = scoreAndFilterCandidates(candidates, tripId, radiusKm, minBatteryRequired);
            log.info("Found {} eligible candidate drivers in Tier {}", scoredDrivers.size(), tierIdx + 1);

            for (ScoredDriver candidate : scoredDrivers) {
                // Check if trip was cancelled by customer while we were searching
                Trip currentTripState = tripRepository.findById(tripId).orElse(null);
                if (currentTripState == null || currentTripState.getStatus() == TripStatus.CANCELLED) {
                    log.info("Trip {} was cancelled during matching process", tripId);
                    return;
                }
                if (currentTripState.getStatus() == TripStatus.MATCHED) {
                    log.info("Trip {} is already MATCHED", tripId);
                    return;
                }

                UUID driverId = candidate.driverId;
                String lockKey = "lock:driver:" + driverId;
                RLock lock = redissonClient.getLock(lockKey);

                boolean acquired = false;
                try {
                    acquired = lock.tryLock(0, responseTimeoutSeconds + 2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (!acquired) {
                    log.info("Driver {} is currently locked/handling another request, skipping", driverId);
                    continue;
                }

                try {
                    log.info("Dispatching trip {} to driver {} (Score: {})", tripId, driverId, candidate.score);
                    sendDispatchNotification(trip, candidate, responseTimeoutSeconds);

                    // Wait up to responseTimeoutSeconds for driver response
                    int pollIntervalMs = 500;
                    int maxIterations = (responseTimeoutSeconds * 1000) / pollIntervalMs;

                    for (int i = 0; i < maxIterations; i++) {
                        Thread.sleep(pollIntervalMs);

                        Trip latest = tripRepository.findById(tripId).orElse(null);
                        if (latest != null) {
                            if (latest.getStatus() == TripStatus.MATCHED) {
                                log.info("Driver {} accepted trip {}!", driverId, tripId);
                                matched = true;
                                break;
                            } else if (latest.getStatus() == TripStatus.CANCELLED) {
                                log.info("Trip {} cancelled while waiting for driver {}", tripId, driverId);
                                return;
                            }
                        }

                        // Check if driver declined early
                        if (driverGeoRepository.isDriverInCooldown(driverId, tripId)) {
                            log.info("Driver {} declined trip {} early", driverId, tripId);
                            break;
                        }
                    }

                    if (matched) {
                        return; // Successfully matched, complete engine execution!
                    }

                    // Driver timed out or declined
                    log.info("Driver {} did not accept trip {} within timeout, moving to cooldown", driverId, tripId);
                    driverGeoRepository.setDriverCooldown(driverId, tripId, 60);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    driverGeoRepository.clearPendingDispatch(driverId);
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        }

        // Exhausted all 3 tiers without matching
        Trip finalCheck = tripRepository.findById(tripId).orElse(null);
        if (finalCheck != null && finalCheck.getStatus() == TripStatus.SEARCHING) {
            log.warn("No driver accepted trip {} after scanning all 3 tiers. Cancelling trip.", tripId);
            finalCheck.setStatus(TripStatus.CANCELLED);
            finalCheck.setCancelledBy("SYSTEM");
            finalCheck.setCancelReason("NO_DRIVER_AVAILABLE");
            tripRepository.save(finalCheck);

            TripStatusUpdateDto cancelDto = new TripStatusUpdateDto();
            cancelDto.setTripId(tripId);
            cancelDto.setStatus(TripStatus.CANCELLED);
            cancelDto.setMessage("Không tìm thấy tài xế xe điện xung quanh điểm đón. Vui lòng thử lại sau ít phút!");
            cancelDto.setCancelledBy("SYSTEM");
            messagingTemplate.convertAndSend("/topic/trip/" + tripId, cancelDto);
        }
    }

    private record ScoredDriver(UUID driverId, double score, DriverGeoRedisRepository.DriverCandidateLocation loc) {}

    private List<ScoredDriver> scoreAndFilterCandidates(
            List<DriverGeoRedisRepository.DriverCandidateLocation> candidates,
            UUID tripId, double radiusMaxKm, int minBatteryRequired) {

        List<ScoredDriver> list = new ArrayList<>();

        for (DriverGeoRedisRepository.DriverCandidateLocation cand : candidates) {
            UUID driverId = cand.driverId();

            if (driverGeoRepository.isDriverInCooldown(driverId, tripId)) {
                continue;
            }

            int battery = driverGeoRepository.getDriverBattery(driverId);
            if (battery < minBatteryRequired) {
                log.debug("Skipping driver {} due to battery {}% < {}%", driverId, battery, minBatteryRequired);
                continue;
            }

            Optional<DriverProfile> profileOpt = driverProfileRepository.findById(driverId);
            if (profileOpt.isEmpty()) {
                continue;
            }

            DriverProfile profile = profileOpt.get();
            if (profile.getKycStatus() != KycStatus.APPROVED || !Boolean.TRUE.equals(profile.getIsActiveShift())) {
                continue;
            }

            // S(d) = 0.50 * S_dist + 0.30 * S_bat + 0.20 * S_rate
            double sDist = Math.max(0.0, 1.0 - (cand.distanceKm() / radiusMaxKm));
            double sBat = battery / 100.0;
            double rating = profile.getRatingAvg() != null ? profile.getRatingAvg().doubleValue() : 5.0;
            double sRate = Math.max(0.0, Math.min(1.0, (rating - 1.0) / 4.0));

            double totalScore = (0.50 * sDist) + (0.30 * sBat) + (0.20 * sRate);
            list.add(new ScoredDriver(driverId, totalScore, cand));
        }

        // Sort descending by score
        list.sort((a, b) -> Double.compare(b.score, a.score));
        return list;
    }

    private void sendDispatchNotification(Trip trip, ScoredDriver candidate, int countdownSeconds) {
        DispatchNotificationDto dto = new DispatchNotificationDto();
        dto.setTripId(trip.getId());
        dto.setTripCode(trip.getTripCode());
        dto.setPickupAddress(trip.getPickupAddress());
        dto.setPickupLat(trip.getPickupLat());
        dto.setPickupLng(trip.getPickupLng());
        dto.setDistanceToPickupKm(BigDecimal.valueOf(candidate.loc.distanceKm()).setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setDropoffAddress(trip.getDropoffAddress());
        dto.setDropoffLat(trip.getDropoffLat());
        dto.setDropoffLng(trip.getDropoffLng());

        double tripDistanceKm = (trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 0) / 1000.0;
        dto.setTripDistanceKm(BigDecimal.valueOf(tripDistanceKm).setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setEstimatedDurationMinutes((trip.getEstimatedDurationS() != null ? trip.getEstimatedDurationS() : 0) / 60);

        BigDecimal netIncome = fareCalculationService.calculateDriverIncome(trip.getFinalAmount());
        dto.setEstimatedEarningsVnd(netIncome);
        dto.setCo2SavedGrams(trip.getCo2SavedGrams());
        dto.setCountdownSeconds(countdownSeconds);

        // Store pending dispatch in Redis for reliable driver polling
        driverGeoRepository.savePendingDispatch(candidate.driverId, dto, countdownSeconds);

        // Send to driver's private queue via WebSocket
        messagingTemplate.convertAndSendToUser(candidate.driverId.toString(), "/queue/ride-dispatch", dto);
        driverProfileRepository.findById(candidate.driverId).ifPresent(p -> {
            if (p.getUserId() != null) {
                messagingTemplate.convertAndSendToUser(p.getUserId().toString(), "/queue/ride-dispatch", dto);
            }
        });
    }
}

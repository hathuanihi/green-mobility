package com.greenmobility.modules.trip.service;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.matching.service.MatchingEngineService;
import com.greenmobility.modules.trip.dto.*;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripGpsPoint;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.event.TripCompletedEvent;
import com.greenmobility.modules.trip.event.TripRequestedEvent;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final Random random = new Random();

    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final DriverGeoRedisRepository driverGeoRepository;
    private final RoutingService routingService;
    private final FareCalculationService fareCalculationService;
    private final CarbonEstimateService carbonEstimateService;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchingEngineService matchingEngineService;
    private final GpsTelemetryService gpsTelemetryService;
    private final TripTrackingRedisService tripTrackingRedisService;
    private final GeofenceService geofenceService;
    private final ActualDistanceCalculator actualDistanceCalculator;

    public TripService(TripRepository tripRepository,
                       DriverProfileRepository driverProfileRepository,
                       VehicleRepository vehicleRepository,
                       UserRepository userRepository,
                       DriverGeoRedisRepository driverGeoRepository,
                       RoutingService routingService,
                       FareCalculationService fareCalculationService,
                       CarbonEstimateService carbonEstimateService,
                       RabbitTemplate rabbitTemplate,
                       SimpMessagingTemplate messagingTemplate,
                       MatchingEngineService matchingEngineService) {
        this(tripRepository, driverProfileRepository, vehicleRepository, userRepository,
                driverGeoRepository, routingService, fareCalculationService, carbonEstimateService,
                rabbitTemplate, messagingTemplate, matchingEngineService, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TripService(TripRepository tripRepository,
                       DriverProfileRepository driverProfileRepository,
                       VehicleRepository vehicleRepository,
                       UserRepository userRepository,
                       DriverGeoRedisRepository driverGeoRepository,
                       RoutingService routingService,
                       FareCalculationService fareCalculationService,
                       CarbonEstimateService carbonEstimateService,
                       RabbitTemplate rabbitTemplate,
                       SimpMessagingTemplate messagingTemplate,
                       MatchingEngineService matchingEngineService,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) GpsTelemetryService gpsTelemetryService,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) TripTrackingRedisService tripTrackingRedisService,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) GeofenceService geofenceService,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) ActualDistanceCalculator actualDistanceCalculator) {
        this.tripRepository = tripRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.driverGeoRepository = driverGeoRepository;
        this.routingService = routingService;
        this.fareCalculationService = fareCalculationService;
        this.carbonEstimateService = carbonEstimateService;
        this.rabbitTemplate = rabbitTemplate;
        this.messagingTemplate = messagingTemplate;
        this.matchingEngineService = matchingEngineService;
        this.gpsTelemetryService = gpsTelemetryService;
        this.tripTrackingRedisService = tripTrackingRedisService;
        this.geofenceService = geofenceService;
        this.actualDistanceCalculator = actualDistanceCalculator;
    }

    public TripEstimateResponse estimateTrip(TripEstimateRequest request) {
        RoutingService.RouteInfo route = routingService.calculateRoute(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng(),
                request.getVehicleType() != null ? request.getVehicleType().name() : "ELECTRIC_MOTORBIKE"
        );

        double distanceKm = BigDecimal.valueOf(route.distanceMeters() / 1000.0)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        int durationMinutes = route.durationSeconds() / 60;

        BigDecimal fare = fareCalculationService.calculateFare(request.getVehicleType(), distanceKm, Instant.now());
        CarbonEstimateDto carbon = carbonEstimateService.estimateCarbon(request.getVehicleType(), distanceKm);

        return new TripEstimateResponse(
                request.getVehicleType(),
                route.distanceMeters(),
                distanceKm,
                route.durationSeconds(),
                durationMinutes,
                fare,
                carbon,
                route.polyline()
        );
    }

    @Transactional
    public TripResponseDto createTripRequest(UUID customerId, TripRequestDto request) {
        // Validate customer doesn't have an active pending/matching trip
        List<TripStatus> activeStatuses = List.of(
                TripStatus.REQUESTED,
                TripStatus.SEARCHING,
                TripStatus.MATCHED,
                TripStatus.DRIVER_ARRIVING,
                TripStatus.ARRIVED,
                TripStatus.IN_TRIP
        );
        if (tripRepository.existsActiveTripForCustomer(customerId, activeStatuses)) {
            throw new BadRequestException("Bạn đang có cuốc xe chưa hoàn thành! Vui lòng hoàn tất hoặc hủy trước khi đặt cuốc mới.");
        }

        // Server-side recalculation to prevent client tampering
        RoutingService.RouteInfo route = routingService.calculateRoute(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng(),
                request.getVehicleType() != null ? request.getVehicleType().name() : "ELECTRIC_MOTORBIKE"
        );
        double distanceKm = route.distanceMeters() / 1000.0;
        Instant now = Instant.now();
        BigDecimal fare = fareCalculationService.calculateFare(request.getVehicleType(), distanceKm, now);
        CarbonEstimateDto carbon = carbonEstimateService.estimateCarbon(request.getVehicleType(), distanceKm);

        // Generate Trip Entity
        Trip trip = new Trip();
        trip.setTripCode(generateTripCode());
        trip.setCustomerId(customerId);
        trip.setVehicleType(request.getVehicleType());

        Point pickupPoint = geometryFactory.createPoint(new Coordinate(request.getPickupLng(), request.getPickupLat()));
        Point dropoffPoint = geometryFactory.createPoint(new Coordinate(request.getDropoffLng(), request.getDropoffLat()));
        trip.setPickupGeom(pickupPoint);
        trip.setPickupAddress(request.getPickupAddress());
        trip.setDropoffGeom(dropoffPoint);
        trip.setDropoffAddress(request.getDropoffAddress());

        trip.setStatus(TripStatus.SEARCHING);
        trip.setEstimatedDistanceM(route.distanceMeters());
        trip.setEstimatedDurationS(route.durationSeconds());
        trip.setFareAmount(fare);
        trip.setFinalAmount(fare);
        trip.setPaymentMethod(request.getPaymentMethod());
        trip.setCo2SavedGrams(carbon.getCo2SavedGrams());
        trip.setRequestedAt(Instant.now());

        Trip savedTrip = tripRepository.save(trip);
        log.info("Created new trip request: ID={}, Code={}", savedTrip.getId(), savedTrip.getTripCode());

        // Publish to RabbitMQ
        TripRequestedEvent event = new TripRequestedEvent(
                savedTrip.getId(),
                savedTrip.getTripCode(),
                savedTrip.getCustomerId(),
                savedTrip.getVehicleType(),
                savedTrip.getPickupLat(),
                savedTrip.getPickupLng(),
                savedTrip.getPickupAddress(),
                savedTrip.getDropoffLat(),
                savedTrip.getDropoffLng(),
                savedTrip.getDropoffAddress(),
                savedTrip.getEstimatedDistanceM(),
                savedTrip.getFareAmount()
        );

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_TRIP_REQUESTED, event);
            log.info("Published TripRequestedEvent to RabbitMQ for trip: {}", savedTrip.getId());
        } catch (Exception e) {
            log.warn("RabbitMQ publish failed, fallback directly to asynchronous matching service: {}", e.getMessage());
            matchingEngineService.matchTripAsync(savedTrip.getId());
        }

        return mapToTripResponse(savedTrip);
    }

    public TripResponseDto getTripDetails(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));
        return mapToTripResponse(trip);
    }

    @Transactional
    public TripResponseDto cancelTrip(UUID tripId, UUID userId, String cancelReason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("Chuyến đi đã kết thúc hoặc đã bị hủy trước đó.");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelReason(cancelReason != null ? cancelReason : "Khách hàng hủy chuyến");
        trip.setCancelledBy(trip.getCustomerId().equals(userId) ? "CUSTOMER" : "DRIVER");
        Trip savedTrip = tripRepository.save(trip);

        // Notify via WebSocket STOMP
        TripStatusUpdateDto updateDto = new TripStatusUpdateDto();
        updateDto.setTripId(tripId);
        updateDto.setStatus(TripStatus.CANCELLED);
        updateDto.setMessage("Chuyến đi đã bị hủy: " + trip.getCancelReason());
        updateDto.setCancelledBy(trip.getCancelledBy());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, updateDto);

        return mapToTripResponse(savedTrip);
    }

    @Transactional
    public DriverTripResponseDto acceptTrip(UUID driverId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (trip.getStatus() != TripStatus.SEARCHING && trip.getStatus() != TripStatus.REQUESTED) {
            throw new BadRequestException("Cuốc xe này không còn ở trạng thái tìm kiếm (Trạng thái hiện tại: " + trip.getStatus() + ")");
        }

        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        Vehicle vehicle = vehicleRepository.findByDriverId(driverId)
                .orElseThrow(() -> new BadRequestException("Tài xế chưa đăng ký phương tiện xe điện"));

        trip.setStatus(TripStatus.MATCHED);
        trip.setDriverId(driverId);
        trip.setVehicleId(vehicle.getId());
        trip.setMatchedAt(Instant.now());
        Trip savedTrip = tripRepository.save(trip);

        // Remove driver from Redis GEO available set
        driverGeoRepository.removeLocation(driverId, trip.getVehicleType());

        // Get customer information
        User customer = userRepository.findById(trip.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getFullName() : "Khách hàng";
        String customerPhone = customer != null ? customer.getPhoneNumber() : "";

        // Send STOMP update to customer via /topic/trip/{tripId}
        DriverSummaryDto driverSummary = buildDriverSummary(profile, vehicle);
        TripStatusUpdateDto statusUpdate = new TripStatusUpdateDto(
                tripId,
                TripStatus.MATCHED,
                "Đã tìm thấy tài xế xe điện! Tài xế đang trên đường đến đón bạn.",
                driverSummary
        );
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, statusUpdate);

        // Build driver response
        DriverTripResponseDto response = new DriverTripResponseDto();
        response.setTripId(savedTrip.getId());
        response.setTripCode(savedTrip.getTripCode());
        response.setStatus(savedTrip.getStatus());
        response.setCustomerName(customerName);
        response.setCustomerPhone(customerPhone);
        response.setPickupAddress(savedTrip.getPickupAddress());
        response.setPickupLat(savedTrip.getPickupLat());
        response.setPickupLng(savedTrip.getPickupLng());
        response.setDropoffAddress(savedTrip.getDropoffAddress());
        response.setDropoffLat(savedTrip.getDropoffLat());
        response.setDropoffLng(savedTrip.getDropoffLng());
        response.setEstimatedDistanceKm((savedTrip.getEstimatedDistanceM() != null ? savedTrip.getEstimatedDistanceM() : 0) / 1000.0);
        response.setNetIncomeVnd(fareCalculationService.calculateDriverIncome(savedTrip.getFinalAmount()));
        response.setCo2SavedGrams(savedTrip.getCo2SavedGrams());
        response.setMatchedAt(savedTrip.getMatchedAt());

        driverGeoRepository.clearPendingDispatch(driverId);
        return response;
    }

    public void declineTrip(UUID driverId, UUID tripId, String reason) {
        log.info("Driver {} declined trip {}: {}", driverId, tripId, reason);
        driverGeoRepository.clearPendingDispatch(driverId);
        driverGeoRepository.setDriverCooldown(driverId, tripId, 60);
    }

    public Optional<TripResponseDto> getCurrentActiveTripForDriver(UUID driverId) {
        List<TripStatus> activeStatuses = List.of(
                TripStatus.MATCHED,
                TripStatus.DRIVER_ARRIVING,
                TripStatus.ARRIVED,
                TripStatus.IN_TRIP
        );
        return tripRepository.findFirstByDriverIdAndStatusInOrderByRequestedAtDesc(driverId, activeStatuses)
                .map(this::mapToTripResponse);
    }

    public List<TripResponseDto> getLiveTrips() {
        List<TripStatus> liveStatuses = List.of(
                TripStatus.REQUESTED,
                TripStatus.SEARCHING,
                TripStatus.MATCHED,
                TripStatus.DRIVER_ARRIVING,
                TripStatus.ARRIVED,
                TripStatus.IN_TRIP
        );
        return tripRepository.findByStatusInOrderByRequestedAtDesc(liveStatuses).stream()
                .map(this::mapToTripResponse)
                .toList();
    }

    /**
     * SPRINT 3 PHASE 4: Get active trips enriched with real-time Redis tracking telemetry
     */
    public List<LiveTripAdminDto> getLiveTripsTracking() {
        List<TripStatus> liveStatuses = List.of(
                TripStatus.DRIVER_ARRIVING,
                TripStatus.ARRIVED,
                TripStatus.IN_TRIP
        );

        List<Trip> activeTrips = tripRepository.findByStatusInOrderByRequestedAtDesc(liveStatuses);

        return activeTrips.stream().map(trip -> {
            LiveTripAdminDto dto = new LiveTripAdminDto();
            dto.setTripId(trip.getId());
            dto.setTripCode(trip.getTripCode());
            dto.setStatus(trip.getStatus());
            dto.setVehicleType(trip.getVehicleType());
            dto.setPickupAddress(trip.getPickupAddress());
            dto.setPickupLat(trip.getPickupLat());
            dto.setPickupLng(trip.getPickupLng());
            dto.setDropoffAddress(trip.getDropoffAddress());
            dto.setDropoffLat(trip.getDropoffLat());
            dto.setDropoffLng(trip.getDropoffLng());
            dto.setRequestedAt(trip.getRequestedAt());
            dto.setMatchedAt(trip.getMatchedAt());
            dto.setFareAmountVnd(trip.getFinalAmount());
            dto.setCo2SavedGrams(trip.getCo2SavedGrams());

            double distKm = (trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 0) / 1000.0;
            dto.setEstimatedDistanceKm(BigDecimal.valueOf(distKm).setScale(1, RoundingMode.HALF_UP).doubleValue());
            dto.setEstimatedDurationMinutes((trip.getEstimatedDurationS() != null ? trip.getEstimatedDurationS() : 0) / 60);

            // Customer Info
            if (trip.getCustomerId() != null) {
                userRepository.findById(trip.getCustomerId()).ifPresent(user -> {
                    dto.setCustomerName(user.getFullName());
                    dto.setCustomerPhone(user.getPhoneNumber());
                });
            }

            // Driver Info
            if (trip.getDriverId() != null) {
                driverProfileRepository.findById(trip.getDriverId()).ifPresent(profile -> {
                    Vehicle vehicle = trip.getVehicleId() != null
                            ? vehicleRepository.findById(trip.getVehicleId()).orElse(null)
                            : null;
                    dto.setDriver(buildDriverSummary(profile, vehicle));
                });
            }

            // Tracking info from Redis
            if (tripTrackingRedisService != null) {
                Map<String, String> tracking = tripTrackingRedisService.getTracking(trip.getId());
                if (!tracking.isEmpty()) {
                    if (tracking.containsKey("driverLat")) dto.setDriverLat(Double.parseDouble(tracking.get("driverLat")));
                    if (tracking.containsKey("driverLng")) dto.setDriverLng(Double.parseDouble(tracking.get("driverLng")));
                    if (tracking.containsKey("bearing")) dto.setBearing(Double.parseDouble(tracking.get("bearing")));
                    if (tracking.containsKey("speedKmh")) dto.setSpeedKmh(Double.parseDouble(tracking.get("speedKmh")));
                    if (tracking.containsKey("batteryPercent")) dto.setBatteryPercent(Integer.parseInt(tracking.get("batteryPercent")));
                    if (tracking.containsKey("etaSeconds")) dto.setEtaSeconds(Integer.parseInt(tracking.get("etaSeconds")));
                    if (tracking.containsKey("distanceRemainingM")) dto.setDistanceRemainingM(Integer.parseInt(tracking.get("distanceRemainingM")));
                    if (tracking.containsKey("phase")) dto.setPhase(tracking.get("phase"));
                    if (tracking.containsKey("lastPingEpoch")) dto.setLastPingEpoch(Long.parseLong(tracking.get("lastPingEpoch")));
                }
                String polyline = tripTrackingRedisService.getRoutePolyline(trip.getId());
                dto.setRoutePolyline(polyline);
            }

            // Fallback for driver coordinates if redis has not received a ping yet
            if (dto.getDriverLat() == null && dto.getPickupLat() != null) {
                dto.setDriverLat(dto.getPickupLat());
                dto.setDriverLng(dto.getPickupLng());
            }

            return dto;
        }).toList();
    }

    /**
     * SPRINT 3 PHASE 4: Operations KPI Cards stats for Today
     */
    public TodayTripStatsDto getTodayTripStats() {
        Instant startOfDay = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toInstant();

        List<Trip> tripsToday = tripRepository.findTripsToday(startOfDay);

        long activeCount = tripsToday.stream()
                .filter(t -> t.getStatus() == TripStatus.DRIVER_ARRIVING ||
                             t.getStatus() == TripStatus.ARRIVED ||
                             t.getStatus() == TripStatus.IN_TRIP ||
                             t.getStatus() == TripStatus.MATCHED)
                .count();

        long completedCount = tripsToday.stream()
                .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                .count();

        // Calculate average pickup time (from matchedAt to arrivedPickupAt)
        List<Long> pickupTimesSeconds = tripsToday.stream()
                .filter(t -> t.getMatchedAt() != null && t.getArrivedPickupAt() != null && !t.getArrivedPickupAt().isBefore(t.getMatchedAt()))
                .map(t -> java.time.Duration.between(t.getMatchedAt(), t.getArrivedPickupAt()).getSeconds())
                .filter(s -> s > 0 && s < 3600)
                .toList();

        long avgPickupSec = pickupTimesSeconds.isEmpty() ? 0 : (long) pickupTimesSeconds.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgPickupMin = BigDecimal.valueOf(avgPickupSec / 60.0).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // Calculate total km and CO2
        double totalKm = tripsToday.stream()
                .mapToDouble(t -> {
                    if (t.getActualDistanceM() != null && t.getActualDistanceM() > 0) {
                        return t.getActualDistanceM() / 1000.0;
                    } else if (t.getEstimatedDistanceM() != null) {
                        return t.getEstimatedDistanceM() / 1000.0;
                    }
                    return 0.0;
                })
                .sum();
        totalKm = BigDecimal.valueOf(totalKm).setScale(1, RoundingMode.HALF_UP).doubleValue();

        BigDecimal co2Grams = tripsToday.stream()
                .map(t -> t.getCo2SavedGrams() != null ? t.getCo2SavedGrams() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double co2Kg = co2Grams.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).doubleValue();

        return new TodayTripStatsDto(
                activeCount,
                avgPickupMin,
                avgPickupSec,
                totalKm,
                co2Grams,
                co2Kg,
                completedCount
        );
    }

    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::mapToTripResponse)
                .toList();
    }


    // ==========================================
    // SPRINT 3 PHASE 1: TRIP EXECUTION METHODS
    // ==========================================

    /**
     * 1. Driver starts moving to pickup point (MATCHED -> DRIVER_ARRIVING).
     */
    @Transactional
    public DriverArrivingResponseDto startArriving(UUID driverId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new BadRequestException("Cuốc xe này không thuộc quyền quản lý của bạn.");
        }

        if (trip.getStatus() != TripStatus.MATCHED) {
            throw new BadRequestException("Cuốc xe không ở trạng thái cho phép bắt đầu di chuyển (status hiện tại: " + trip.getStatus() + ")");
        }

        // Determine driver's current coordinates
        Map<String, String> driverLoc = tripTrackingRedisService.getDriverLatestLocation(driverId);
        double driverLat = trip.getPickupLat();
        double driverLng = trip.getPickupLng();

        if (driverLoc.containsKey("lat") && driverLoc.containsKey("lng")) {
            try {
                driverLat = Double.parseDouble(driverLoc.get("lat"));
                driverLng = Double.parseDouble(driverLoc.get("lng"));
            } catch (NumberFormatException ignored) {}
        }

        // Call RoutingService (driverLocation -> pickup)
        RoutingResultDto route = routingService.getRoute(
                driverLat, driverLng,
                trip.getPickupLat(), trip.getPickupLng(),
                trip.getVehicleType().name()
        );

        // Update trip status
        trip.setStatus(TripStatus.DRIVER_ARRIVING);
        tripRepository.save(trip);

        // Ensure driver is removed from available GEO pool
        driverGeoRepository.removeLocation(driverId, trip.getVehicleType());

        // Create Redis tracking hash & save polyline
        tripTrackingRedisService.createTracking(
                tripId, driverId, driverLat, driverLng,
                "DRIVER_ARRIVING", route.getDurationS(), route.getDistanceM()
        );
        if (route.getPolyline() != null && !route.getPolyline().isEmpty()) {
            tripTrackingRedisService.saveRoutePolyline(tripId, route.getPolyline());
        }

        // Get driver & customer info
        DriverProfile profile = driverProfileRepository.findById(driverId).orElse(null);
        Vehicle vehicle = trip.getVehicleId() != null ? vehicleRepository.findById(trip.getVehicleId()).orElse(null) : null;
        DriverSummaryDto driverSummary = profile != null ? buildDriverSummary(profile, vehicle) : null;

        User customer = userRepository.findById(trip.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getFullName() : "Khách hàng";
        String customerPhone = customer != null ? customer.getPhoneNumber() : "";

        // WebSocket broadcast to /topic/trip/{tripId}
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("tripId", tripId);
        wsPayload.put("status", "DRIVER_ARRIVING");
        wsPayload.put("message", "Tài xế đang trên đường đến đón bạn!");
        wsPayload.put("driver", driverSummary);
        wsPayload.put("routing", route);
        wsPayload.put("timestamp", Instant.now());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, wsPayload);

        log.info("Driver {} started arriving for trip {}", driverId, tripId);

        return new DriverArrivingResponseDto(
                tripId,
                "DRIVER_ARRIVING",
                route,
                new DriverArrivingResponseDto.LocationInfo(trip.getPickupAddress(), trip.getPickupLat(), trip.getPickupLng()),
                new DriverArrivingResponseDto.CustomerInfo(customerName, customerPhone)
        );
    }

    /**
     * 2. Driver arrives at pickup location (DRIVER_ARRIVING -> ARRIVED).
     */
    @Transactional
    public DriverArriveResponseDto arriveAtPickup(UUID driverId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new BadRequestException("Cuốc xe này không thuộc quyền quản lý của bạn.");
        }

        if (trip.getStatus() != TripStatus.DRIVER_ARRIVING) {
            throw new BadRequestException("Cuốc xe không ở trạng thái đang đến đón (status hiện tại: " + trip.getStatus() + ")");
        }

        // Verify distance <= 200m if location is available in Redis
        Map<String, String> driverLoc = tripTrackingRedisService.getDriverLatestLocation(driverId);
        if (driverLoc.containsKey("lat") && driverLoc.containsKey("lng")) {
            try {
                double driverLat = Double.parseDouble(driverLoc.get("lat"));
                double driverLng = Double.parseDouble(driverLoc.get("lng"));
                double distance = geofenceService.calculateHaversineDistance(
                        driverLat, driverLng, trip.getPickupLat(), trip.getPickupLng()
                );
                if (distance > GeofenceService.MANUAL_ARRIVE_MAX_DISTANCE_M) {
                    throw new BadRequestException(String.format(
                            "Bạn còn cách điểm đón %.0fm. Vui lòng di chuyển đến gần hơn (≤ 200m) để xác nhận đã đến.",
                            distance
                    ));
                }
            } catch (NumberFormatException ignored) {}
        }

        Instant arrivedAt = Instant.now();
        trip.setStatus(TripStatus.ARRIVED);
        trip.setArrivedPickupAt(arrivedAt);
        tripRepository.save(trip);

        tripTrackingRedisService.updatePhase(tripId, "ARRIVED");

        User customer = userRepository.findById(trip.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getFullName() : "Khách hàng";
        String customerPhone = customer != null ? customer.getPhoneNumber() : "";

        // WebSocket broadcast
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("tripId", tripId);
        wsPayload.put("status", "ARRIVED");
        wsPayload.put("message", "Tài xế đã đến điểm đón! Vui lòng ra xe.");
        wsPayload.put("arrivedAt", arrivedAt);
        wsPayload.put("waitingTimeoutSeconds", 300);
        wsPayload.put("timestamp", Instant.now());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, wsPayload);

        log.info("Driver {} arrived at pickup for trip {}", driverId, tripId);

        return new DriverArriveResponseDto(
                tripId,
                "ARRIVED",
                arrivedAt,
                300,
                customerName,
                customerPhone
        );
    }

    /**
     * Automatic arrival triggered by Geofence (≤ 50m) during location update.
     */
    @Transactional
    public DriverArriveResponseDto autoArriveAtPickup(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (trip.getStatus() != TripStatus.DRIVER_ARRIVING) {
            log.debug("Auto arrive skipped: trip {} is already in status {}", tripId, trip.getStatus());
            return null;
        }

        Instant arrivedAt = Instant.now();
        trip.setStatus(TripStatus.ARRIVED);
        trip.setArrivedPickupAt(arrivedAt);
        tripRepository.save(trip);

        tripTrackingRedisService.updatePhase(tripId, "ARRIVED");

        User customer = userRepository.findById(trip.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getFullName() : "Khách hàng";
        String customerPhone = customer != null ? customer.getPhoneNumber() : "";

        // WebSocket broadcast
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("tripId", tripId);
        wsPayload.put("status", "ARRIVED");
        wsPayload.put("message", "Tài xế đã đến điểm đón! Vui lòng ra xe.");
        wsPayload.put("arrivedAt", arrivedAt);
        wsPayload.put("waitingTimeoutSeconds", 300);
        wsPayload.put("timestamp", Instant.now());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, wsPayload);

        log.info("Geofence auto-arrived triggered for trip {}", tripId);

        return new DriverArriveResponseDto(
                tripId,
                "ARRIVED",
                arrivedAt,
                300,
                customerName,
                customerPhone
        );
    }

    /**
     * 3. Driver starts the trip with customer on board (ARRIVED -> IN_TRIP).
     */
    @Transactional
    public DriverStartTripResponseDto startTrip(UUID driverId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new BadRequestException("Cuốc xe này không thuộc quyền quản lý của bạn.");
        }

        if (trip.getStatus() != TripStatus.ARRIVED) {
            throw new BadRequestException("Cuốc xe chưa ở trạng thái đã đến điểm đón (status hiện tại: " + trip.getStatus() + ")");
        }

        // Calculate route from pickup to dropoff
        RoutingResultDto route = routingService.getRoute(
                trip.getPickupLat(), trip.getPickupLng(),
                trip.getDropoffLat(), trip.getDropoffLng(),
                trip.getVehicleType().name()
        );

        Instant startedAt = Instant.now();
        trip.setStatus(TripStatus.IN_TRIP);
        trip.setStartedTripAt(startedAt);
        tripRepository.save(trip);

        tripTrackingRedisService.updatePhase(tripId, "IN_TRIP");
        if (route.getPolyline() != null && !route.getPolyline().isEmpty()) {
            tripTrackingRedisService.saveRoutePolyline(tripId, route.getPolyline());
        }

        // WebSocket broadcast
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("tripId", tripId);
        wsPayload.put("status", "IN_TRIP");
        wsPayload.put("message", "Chuyến đi đã bắt đầu! Cùng di chuyển xanh nào!");
        wsPayload.put("routing", route);
        wsPayload.put("startedAt", startedAt);
        wsPayload.put("timestamp", Instant.now());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, wsPayload);

        log.info("Driver {} started trip {}", driverId, tripId);

        return new DriverStartTripResponseDto(
                tripId,
                "IN_TRIP",
                startedAt,
                route,
                new DriverStartTripResponseDto.LocationInfo(trip.getDropoffAddress(), trip.getDropoffLat(), trip.getDropoffLng())
        );
    }

    /**
     * 4. Driver completes the trip at dropoff location (IN_TRIP -> COMPLETED).
     */
    @Transactional
    public TripCompleteSummaryDto completeTrip(UUID driverId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new BadRequestException("Cuốc xe này không thuộc quyền quản lý của bạn.");
        }

        if (trip.getStatus() != TripStatus.IN_TRIP) {
            throw new BadRequestException("Cuốc xe không ở trạng thái đang chở khách (status hiện tại: " + trip.getStatus() + ")");
        }

        Instant completedAt = Instant.now();

        // 1. Query MongoDB GPS points for phase IN_TRIP
        List<TripGpsPoint> points = gpsTelemetryService.getPointsByTripAndPhase(tripId.toString(), "IN_TRIP");

        // 2. Calculate actual distance using ActualDistanceCalculator with noise filtering
        int rawDistanceM = actualDistanceCalculator.calculateActualDistance(points);
        int estDistanceM = trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 0;
        int finalDistanceM = rawDistanceM > 0
                ? actualDistanceCalculator.getFallbackDistance(rawDistanceM, estDistanceM)
                : estDistanceM;

        // 3. Calculate actual duration in seconds
        int actualDurationS = trip.getStartedTripAt() != null
                ? (int) Duration.between(trip.getStartedTripAt(), completedAt).getSeconds()
                : (trip.getEstimatedDurationS() != null ? trip.getEstimatedDurationS() : 0);

        // 4. Calculate CO2 saved
        double distanceKm = finalDistanceM / 1000.0;
        CarbonEstimateDto carbon = carbonEstimateService.estimateCarbon(trip.getVehicleType(), distanceKm);
        BigDecimal co2SavedGrams = carbon.getCo2SavedGrams();

        // 5. Update Trip entity
        trip.setActualDistanceM(finalDistanceM);
        trip.setActualDurationS(actualDurationS);
        trip.setCo2SavedGrams(co2SavedGrams);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(completedAt);
        Trip savedTrip = tripRepository.save(trip);

        // 6. Update Driver Profile: total_trips_completed += 1 and total CO2 saved
        DriverProfile driverProfile = driverProfileRepository.findById(driverId).orElse(null);
        String driverName = "Tài xế";
        if (driverProfile != null) {
            int completedCount = driverProfile.getTotalTripsCompleted() != null
                    ? driverProfile.getTotalTripsCompleted() + 1 : 1;
            driverProfile.setTotalTripsCompleted(completedCount);

            BigDecimal co2Kg = co2SavedGrams.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
            BigDecimal totalCo2 = driverProfile.getTotalCo2SavedKg() != null
                    ? driverProfile.getTotalCo2SavedKg().add(co2Kg) : co2Kg;
            driverProfile.setTotalCo2SavedKg(totalCo2);
            driverProfileRepository.save(driverProfile);

            User driverUser = userRepository.findById(driverProfile.getUserId()).orElse(null);
            if (driverUser != null) {
                driverName = driverUser.getFullName();
            }
        }

        // 7. Calculate driver earnings (80% net, 20% platform fee)
        BigDecimal grossAmount = savedTrip.getFinalAmount();
        BigDecimal netEarnings = fareCalculationService.calculateDriverIncome(grossAmount);
        BigDecimal platformFee = grossAmount.subtract(netEarnings);

        // 8. Clean up Redis tracking & restore driver to available GEO pool
        tripTrackingRedisService.deleteTracking(tripId, driverId);
        driverGeoRepository.updateLocation(
                driverId,
                trip.getVehicleType(),
                trip.getDropoffLat(),
                trip.getDropoffLng(),
                100
        );

        // 9. Customer details
        User customer = userRepository.findById(trip.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getFullName() : "Khách hàng";

        // 10. Build summary DTO
        TripCompleteSummaryDto.TripSummaryDetail summaryDetail = new TripCompleteSummaryDto.TripSummaryDetail();
        summaryDetail.setTripCode(savedTrip.getTripCode());
        summaryDetail.setPickupAddress(savedTrip.getPickupAddress());
        summaryDetail.setDropoffAddress(savedTrip.getDropoffAddress());
        summaryDetail.setEstimatedDistanceM(savedTrip.getEstimatedDistanceM());
        summaryDetail.setActualDistanceM(finalDistanceM);
        summaryDetail.setEstimatedDurationS(savedTrip.getEstimatedDurationS());
        summaryDetail.setActualDurationS(actualDurationS);
        summaryDetail.setFareAmountVnd(savedTrip.getFareAmount());
        summaryDetail.setFinalAmountVnd(savedTrip.getFinalAmount());
        summaryDetail.setPaymentMethod(savedTrip.getPaymentMethod() != null ? savedTrip.getPaymentMethod().name() : "CASH");
        summaryDetail.setPaymentStatus(savedTrip.getPaymentStatus() != null ? savedTrip.getPaymentStatus().name() : "PENDING");
        summaryDetail.setCo2SavedGrams(co2SavedGrams);
        summaryDetail.setCustomerName(customerName);
        summaryDetail.setDriverName(driverName);

        TripCompleteSummaryDto.DriverEarningsDetail earningsDetail =
                new TripCompleteSummaryDto.DriverEarningsDetail(grossAmount, platformFee, netEarnings);

        TripCompleteSummaryDto responseDto = new TripCompleteSummaryDto(
                tripId,
                savedTrip.getTripCode(),
                "COMPLETED",
                completedAt,
                summaryDetail,
                earningsDetail
        );

        // 11. WebSocket broadcast to /topic/trip/{tripId}
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("tripId", tripId);
        wsPayload.put("status", "COMPLETED");
        wsPayload.put("message", "Chuyến đi hoàn thành! Cảm ơn bạn đã đi xe điện xanh!");
        wsPayload.put("tripSummary", summaryDetail);
        wsPayload.put("driverEarnings", earningsDetail);
        wsPayload.put("completedAt", completedAt);
        wsPayload.put("timestamp", Instant.now());
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, wsPayload);

        // 12. Publish TripCompletedEvent to RabbitMQ
        TripCompletedEvent completedEvent = new TripCompletedEvent(
                savedTrip.getId(),
                savedTrip.getTripCode(),
                savedTrip.getCustomerId(),
                savedTrip.getDriverId(),
                savedTrip.getVehicleId(),
                savedTrip.getVehicleType().name(),
                savedTrip.getEstimatedDistanceM(),
                savedTrip.getActualDistanceM(),
                savedTrip.getEstimatedDurationS(),
                savedTrip.getActualDurationS(),
                savedTrip.getFareAmount(),
                savedTrip.getFinalAmount(),
                savedTrip.getPaymentMethod() != null ? savedTrip.getPaymentMethod().name() : "CASH",
                savedTrip.getCo2SavedGrams(),
                savedTrip.getStartedTripAt(),
                savedTrip.getCompletedAt()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_TRIP_COMPLETED,
                    completedEvent
            );
            log.info("Published TripCompletedEvent to RabbitMQ for trip {}", tripId);
        } catch (Exception e) {
            log.warn("Failed to publish TripCompletedEvent to RabbitMQ: {}", e.getMessage());
        }

        log.info("Trip {} completed successfully. Actual dist: {}m, CO2 saved: {}g", tripId, finalDistanceM, co2SavedGrams);
        return responseDto;
    }

    /**
     * 5. Driver cancels the trip (DRIVER_ARRIVING or ARRIVED -> CANCELLED).
     */
    @Transactional
    public DriverCancelResponseDto cancelByDriver(UUID driverId, UUID tripId, String reason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        if (!driverId.equals(trip.getDriverId())) {
            throw new BadRequestException("Cuốc xe này không thuộc quyền quản lý của bạn.");
        }

        if (trip.getStatus() == TripStatus.IN_TRIP) {
            throw new BadRequestException("Không thể hủy cuốc xe khi đang chở khách.");
        }

        if (trip.getStatus() != TripStatus.DRIVER_ARRIVING && trip.getStatus() != TripStatus.ARRIVED) {
            throw new BadRequestException("Cuốc xe không ở trạng thái cho phép hủy (status hiện tại: " + trip.getStatus() + ")");
        }

        boolean isPenalized = trip.getStatus() == TripStatus.DRIVER_ARRIVING;

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledBy("DRIVER");
        trip.setCancelReason(reason != null ? reason : "Tài xế hủy cuốc");
        tripRepository.save(trip);

        // Clean up Redis & return driver to pool
        tripTrackingRedisService.deleteTracking(tripId, driverId);
        driverGeoRepository.updateLocation(
                driverId,
                trip.getVehicleType(),
                trip.getPickupLat(),
                trip.getPickupLng(),
                100
        );

        // WebSocket broadcast
        TripStatusUpdateDto updateDto = new TripStatusUpdateDto();
        updateDto.setTripId(tripId);
        updateDto.setStatus(TripStatus.CANCELLED);
        updateDto.setMessage("Chuyến đi đã bị hủy bởi tài xế: " + trip.getCancelReason());
        updateDto.setCancelledBy("DRIVER");
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, updateDto);

        log.info("Driver {} cancelled trip {}: {}", driverId, tripId, reason);

        return new DriverCancelResponseDto(
                tripId,
                "CANCELLED",
                "DRIVER",
                trip.getCancelReason(),
                isPenalized
        );
    }

    /**
     * Get current trip tracking state (used by Customer REST fallback).
     */
    public TripTrackingDto getTripTrackingState(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        TripTrackingDto dto = new TripTrackingDto();
        dto.setTripId(trip.getId().toString());
        dto.setTripCode(trip.getTripCode());
        dto.setStatus(trip.getStatus().name());

        if (trip.getDriverId() != null) {
            DriverProfile profile = driverProfileRepository.findById(trip.getDriverId()).orElse(null);
            Vehicle vehicle = trip.getVehicleId() != null ? vehicleRepository.findById(trip.getVehicleId()).orElse(null) : null;
            if (profile != null) {
                dto.setDriver(buildDriverSummary(profile, vehicle));
            }
        }

        Map<String, String> tracking = tripTrackingRedisService.getTracking(tripId);
        TripTrackingDto.TrackingData trackingData = new TripTrackingDto.TrackingData();
        if (!tracking.isEmpty()) {
            if (tracking.containsKey("driverLat")) trackingData.setDriverLat(Double.parseDouble(tracking.get("driverLat")));
            if (tracking.containsKey("driverLng")) trackingData.setDriverLng(Double.parseDouble(tracking.get("driverLng")));
            if (tracking.containsKey("bearing")) trackingData.setBearing(Double.parseDouble(tracking.get("bearing")));
            if (tracking.containsKey("speedKmh")) trackingData.setSpeedKmh(Double.parseDouble(tracking.get("speedKmh")));
            if (tracking.containsKey("batteryPercent")) trackingData.setBatteryPercent(Integer.parseInt(tracking.get("batteryPercent")));
            if (tracking.containsKey("etaSeconds")) trackingData.setEtaSeconds(Integer.parseInt(tracking.get("etaSeconds")));
            if (tracking.containsKey("distanceRemainingM")) trackingData.setDistanceRemainingM(Integer.parseInt(tracking.get("distanceRemainingM")));
            if (tracking.containsKey("phase")) trackingData.setPhase(tracking.get("phase"));
        } else {
            // Default fallback if tracking not yet in Redis
            trackingData.setDriverLat(trip.getPickupLat());
            trackingData.setDriverLng(trip.getPickupLng());
            trackingData.setPhase(trip.getStatus().name());
        }

        String polyline = tripTrackingRedisService.getRoutePolyline(tripId);
        trackingData.setRoutePolyline(polyline);
        trackingData.setLastUpdatedAt(Instant.now());
        dto.setTracking(trackingData);

        dto.setPickup(new TripTrackingDto.LocationDto(trip.getPickupAddress(), trip.getPickupLat(), trip.getPickupLng()));
        dto.setDropoff(new TripTrackingDto.LocationDto(trip.getDropoffAddress(), trip.getDropoffLat(), trip.getDropoffLng()));

        return dto;
    }

    /**
     * Get route polyline and navigation steps for a trip.
     */
    public TripRouteResponseDto getTripRoute(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        boolean isArriving = trip.getStatus() == TripStatus.MATCHED || trip.getStatus() == TripStatus.DRIVER_ARRIVING;

        double originLat;
        double originLng;
        String originLabel;
        double destLat;
        double destLng;
        String destLabel;

        if (isArriving) {
            Map<String, String> driverLoc = trip.getDriverId() != null
                    ? tripTrackingRedisService.getDriverLatestLocation(trip.getDriverId())
                    : Collections.emptyMap();

            if (driverLoc.containsKey("lat") && driverLoc.containsKey("lng")) {
                originLat = Double.parseDouble(driverLoc.get("lat"));
                originLng = Double.parseDouble(driverLoc.get("lng"));
            } else {
                originLat = trip.getPickupLat();
                originLng = trip.getPickupLng();
            }
            originLabel = "Vị trí tài xế";
            destLat = trip.getPickupLat();
            destLng = trip.getPickupLng();
            destLabel = trip.getPickupAddress();
        } else {
            originLat = trip.getPickupLat();
            originLng = trip.getPickupLng();
            originLabel = trip.getPickupAddress();
            destLat = trip.getDropoffLat();
            destLng = trip.getDropoffLng();
            destLabel = trip.getDropoffAddress();
        }

        RoutingResultDto route = routingService.getRoute(
                originLat, originLng, destLat, destLng, trip.getVehicleType().name()
        );

        String cachedPolyline = tripTrackingRedisService.getRoutePolyline(tripId);
        String finalPolyline = (cachedPolyline != null && !cachedPolyline.isEmpty()) ? cachedPolyline : route.getPolyline();

        return new TripRouteResponseDto(
                tripId,
                trip.getStatus().name(),
                new TripRouteResponseDto.EndpointInfo(originLat, originLng, originLabel),
                new TripRouteResponseDto.EndpointInfo(destLat, destLng, destLabel),
                finalPolyline,
                route.getDistanceM(),
                route.getDurationS(),
                route.getSteps()
        );
    }

    private TripResponseDto mapToTripResponse(Trip trip) {
        TripResponseDto dto = new TripResponseDto();
        dto.setTripId(trip.getId());
        dto.setTripCode(trip.getTripCode());
        dto.setStatus(trip.getStatus());
        dto.setVehicleType(trip.getVehicleType());
        dto.setPickupAddress(trip.getPickupAddress());
        dto.setPickupLat(trip.getPickupLat());
        dto.setPickupLng(trip.getPickupLng());
        dto.setDropoffAddress(trip.getDropoffAddress());
        dto.setDropoffLat(trip.getDropoffLat());
        dto.setDropoffLng(trip.getDropoffLng());
        dto.setFareAmountVnd(trip.getFareAmount());
        dto.setDiscountAmountVnd(trip.getDiscountAmount());
        dto.setFinalAmountVnd(trip.getFinalAmount());
        dto.setPaymentMethod(trip.getPaymentMethod());
        dto.setPaymentStatus(trip.getPaymentStatus());

        double distKm = (trip.getEstimatedDistanceM() != null ? trip.getEstimatedDistanceM() : 0) / 1000.0;
        dto.setEstimatedDistanceKm(BigDecimal.valueOf(distKm).setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setEstimatedDurationMinutes((trip.getEstimatedDurationS() != null ? trip.getEstimatedDurationS() : 0) / 60);
        dto.setCo2SavedGrams(trip.getCo2SavedGrams());
        dto.setCancelReason(trip.getCancelReason());
        dto.setCancelledBy(trip.getCancelledBy());
        dto.setRequestedAt(trip.getRequestedAt());
        dto.setMatchedAt(trip.getMatchedAt());
        dto.setArrivedPickupAt(trip.getArrivedPickupAt());
        dto.setStartedTripAt(trip.getStartedTripAt());
        dto.setCompletedAt(trip.getCompletedAt());
        dto.setActualDistanceM(trip.getActualDistanceM());
        dto.setActualDurationS(trip.getActualDurationS());

        if (trip.getDriverId() != null) {
            DriverProfile profile = driverProfileRepository.findById(trip.getDriverId()).orElse(null);
            Vehicle vehicle = trip.getVehicleId() != null ? vehicleRepository.findById(trip.getVehicleId()).orElse(null) : null;
            if (profile != null) {
                dto.setDriver(buildDriverSummary(profile, vehicle));
            }
        }

        return dto;
    }

    private DriverSummaryDto buildDriverSummary(DriverProfile profile, Vehicle vehicle) {
        User driverUser = userRepository.findById(profile.getUserId()).orElse(null);
        String name = driverUser != null ? driverUser.getFullName() : "Tài xế";
        String phone = driverUser != null ? driverUser.getPhoneNumber() : "";
        String avatar = driverUser != null ? driverUser.getAvatarUrl() : profile.getFacePortraitUrl();

        String model = vehicle != null ? (vehicle.getMake() + " " + vehicle.getModel()) : "Xe điện";
        String plate = vehicle != null ? vehicle.getLicensePlate() : "";

        return new DriverSummaryDto(
                profile.getId(),
                name,
                phone,
                avatar,
                profile.getRatingAvg(),
                model,
                plate,
                null,
                null
        );
    }

    private String generateTripCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + random.nextInt(9000);
        return "GM-" + date + "-" + rand;
    }
}

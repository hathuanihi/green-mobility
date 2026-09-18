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
import com.greenmobility.modules.trip.entity.TripStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

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
    }

    public TripEstimateResponse estimateTrip(TripEstimateRequest request) {
        RoutingService.RouteInfo route = routingService.calculateRoute(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng()
        );

        double distanceKm = BigDecimal.valueOf(route.distanceMeters() / 1000.0)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        int durationMinutes = route.durationSeconds() / 60;

        BigDecimal fare = fareCalculationService.calculateFare(request.getVehicleType(), distanceKm);
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
                request.getDropoffLat(), request.getDropoffLng()
        );
        double distanceKm = route.distanceMeters() / 1000.0;
        BigDecimal fare = fareCalculationService.calculateFare(request.getVehicleType(), distanceKm);
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

    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::mapToTripResponse)
                .toList();
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

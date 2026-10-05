package com.greenmobility.trip;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.matching.service.MatchingEngineService;
import com.greenmobility.modules.trip.dto.*;
import com.greenmobility.modules.trip.entity.PaymentMethod;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripGpsPoint;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.repository.TripRepository;
import com.greenmobility.modules.trip.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Sprint3Phase1ExecutionTest {

    @Mock private TripRepository tripRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverGeoRedisRepository driverGeoRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private MatchingEngineService matchingEngineService;
    @Mock private GpsTelemetryService gpsTelemetryService;
    @Mock private TripTrackingRedisService tripTrackingRedisService;

    private RoutingService routingService;
    private FareCalculationService fareCalculationService;
    private CarbonEstimateService carbonEstimateService;
    private GeofenceService geofenceService;
    private ActualDistanceCalculator actualDistanceCalculator;
    private TripService tripService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private UUID tripId;
    private UUID driverId;
    private UUID customerId;
    private UUID vehicleId;
    private Trip mockTrip;
    private DriverProfile mockDriverProfile;
    private Vehicle mockVehicle;
    private User mockCustomer;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService();
        fareCalculationService = new FareCalculationService();
        carbonEstimateService = new CarbonEstimateService();
        geofenceService = new GeofenceService();
        actualDistanceCalculator = new ActualDistanceCalculator(geofenceService);

        tripService = new TripService(
                tripRepository,
                driverProfileRepository,
                vehicleRepository,
                userRepository,
                driverGeoRepository,
                routingService,
                fareCalculationService,
                carbonEstimateService,
                rabbitTemplate,
                messagingTemplate,
                matchingEngineService,
                gpsTelemetryService,
                tripTrackingRedisService,
                geofenceService,
                actualDistanceCalculator
        );

        tripId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();

        mockTrip = new Trip();
        mockTrip.setId(tripId);
        mockTrip.setTripCode("GM-20261019-9981");
        mockTrip.setCustomerId(customerId);
        mockTrip.setDriverId(driverId);
        mockTrip.setVehicleId(vehicleId);
        mockTrip.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        mockTrip.setStatus(TripStatus.MATCHED);
        mockTrip.setPickupGeom(geometryFactory.createPoint(new Coordinate(106.700981, 10.776530)));
        mockTrip.setPickupAddress("Nhà hát Thành phố, Quận 1, TP.HCM");
        mockTrip.setDropoffGeom(geometryFactory.createPoint(new Coordinate(106.803054, 10.870020)));
        mockTrip.setDropoffAddress("ĐH CNTT, TP. Thủ Đức, TP.HCM");
        mockTrip.setEstimatedDistanceM(16200);
        mockTrip.setEstimatedDurationS(1920);
        mockTrip.setFareAmount(BigDecimal.valueOf(76000));
        mockTrip.setFinalAmount(BigDecimal.valueOf(76000));
        mockTrip.setPaymentMethod(PaymentMethod.CASH);

        UUID driverUserId = UUID.randomUUID();
        mockDriverProfile = new DriverProfile();
        mockDriverProfile.setId(driverId);
        mockDriverProfile.setUserId(driverUserId);
        mockDriverProfile.setTotalTripsCompleted(10);
        mockDriverProfile.setTotalCo2SavedKg(BigDecimal.valueOf(15.5));

        mockVehicle = new Vehicle();
        mockVehicle.setId(vehicleId);
        mockVehicle.setDriverId(driverId);
        mockVehicle.setMake("VinFast");
        mockVehicle.setModel("Feliz S");
        mockVehicle.setLicensePlate("59-P1 987.65");
        mockVehicle.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);

        mockCustomer = new User();
        mockCustomer.setId(customerId);
        mockCustomer.setFullName("Phạm Hà Anh Thư");
        mockCustomer.setPhoneNumber("0901234567");

        User mockDriverUser = new User();
        mockDriverUser.setId(driverUserId);
        mockDriverUser.setFullName("Nguyễn Minh Thiện");
        mockDriverUser.setPhoneNumber("0987654321");

        lenient().when(userRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        lenient().when(userRepository.findById(driverUserId)).thenReturn(Optional.of(mockDriverUser));
    }

    @Test
    @DisplayName("AC-01: Tài xế bắt đầu di chuyển đến đón khách (MATCHED -> DRIVER_ARRIVING)")
    void testStartArriving_Success() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        when(driverProfileRepository.findById(driverId)).thenReturn(Optional.of(mockDriverProfile));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        when(tripTrackingRedisService.getDriverLatestLocation(driverId)).thenReturn(
                Map.of("lat", "10.777120", "lng", "106.701540")
        );

        DriverArrivingResponseDto response = tripService.startArriving(driverId, tripId);

        assertNotNull(response);
        assertEquals("DRIVER_ARRIVING", response.getStatus());
        assertEquals(TripStatus.DRIVER_ARRIVING, mockTrip.getStatus());
        assertNotNull(response.getRouting());
        assertTrue(response.getRouting().getDistanceM() > 0);
        assertEquals("Phạm Hà Anh Thư", response.getCustomer().getFullName());

        verify(tripTrackingRedisService).createTracking(eq(tripId), eq(driverId), anyDouble(), anyDouble(), eq("DRIVER_ARRIVING"), anyInt(), anyInt());
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/" + tripId), any(Map.class));
    }

    @Test
    @DisplayName("AC-04: Tài xế xác nhận đã đến điểm đón (≤ 200m) thành công")
    void testArriveAtPickup_Success() {
        mockTrip.setStatus(TripStatus.DRIVER_ARRIVING);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        // Tọa độ cách pickup ~50m
        when(tripTrackingRedisService.getDriverLatestLocation(driverId)).thenReturn(
                Map.of("lat", "10.776600", "lng", "106.701000")
        );

        DriverArriveResponseDto response = tripService.arriveAtPickup(driverId, tripId);

        assertNotNull(response);
        assertEquals("ARRIVED", response.getStatus());
        assertEquals(TripStatus.ARRIVED, mockTrip.getStatus());
        assertNotNull(mockTrip.getArrivedPickupAt());
        assertEquals(300, response.getNoShowTimeoutSeconds());

        verify(tripTrackingRedisService).updatePhase(tripId, "ARRIVED");
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/" + tripId), any(Map.class));
    }

    @Test
    @DisplayName("AC-05: Tài xế bấm 'Đã đến' khi còn quá xa (> 200m) -> Ném lỗi BadRequestException")
    void testArriveAtPickup_TooFar() {
        mockTrip.setStatus(TripStatus.DRIVER_ARRIVING);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        // Cách xa > 1km
        when(tripTrackingRedisService.getDriverLatestLocation(driverId)).thenReturn(
                Map.of("lat", "10.790000", "lng", "106.720000")
        );

        assertThrows(BadRequestException.class, () -> tripService.arriveAtPickup(driverId, tripId));
    }

    @Test
    @DisplayName("AC-06: Tài xế bắt đầu chuyến đi (ARRIVED -> IN_TRIP)")
    void testStartTrip_Success() {
        mockTrip.setStatus(TripStatus.ARRIVED);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        DriverStartTripResponseDto response = tripService.startTrip(driverId, tripId);

        assertNotNull(response);
        assertEquals("IN_TRIP", response.getStatus());
        assertEquals(TripStatus.IN_TRIP, mockTrip.getStatus());
        assertNotNull(mockTrip.getStartedTripAt());
        assertNotNull(response.getRouting());
        assertTrue(response.getRouting().getDistanceM() >= 14000);

        verify(tripTrackingRedisService).updatePhase(tripId, "IN_TRIP");
        verify(messagingTemplate).convertAndSend(eq("/topic/trip/" + tripId), any(Map.class));
    }

    @Test
    @DisplayName("AC-07: Tài xế hoàn thành chuyến đi (IN_TRIP -> COMPLETED), tính cước và RabbitMQ event")
    void testCompleteTrip_Success() {
        mockTrip.setStatus(TripStatus.IN_TRIP);
        mockTrip.setStartedTripAt(Instant.now().minusSeconds(2000));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        when(driverProfileRepository.findById(driverId)).thenReturn(Optional.of(mockDriverProfile));

        // Mock MongoDB GPS points
        List<TripGpsPoint> points = new ArrayList<>();
        Instant t0 = mockTrip.getStartedTripAt();
        points.add(new TripGpsPoint("t", "d", "v", "IN_TRIP", 106.700981, 10.776530, 25.0, 0.0, 10.0, 5.0, 80, false, t0));
        points.add(new TripGpsPoint("t", "d", "v", "IN_TRIP", 106.750000, 10.820000, 30.0, 0.0, 10.0, 5.0, 80, false, t0.plusSeconds(1000)));
        points.add(new TripGpsPoint("t", "d", "v", "IN_TRIP", 106.803054, 10.870020, 28.0, 0.0, 10.0, 5.0, 80, false, t0.plusSeconds(2000)));
        when(gpsTelemetryService.getPointsByTripAndPhase(tripId.toString(), "IN_TRIP")).thenReturn(points);

        TripCompleteSummaryDto summary = tripService.completeTrip(driverId, tripId);

        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertEquals(TripStatus.COMPLETED, mockTrip.getStatus());
        assertNotNull(mockTrip.getCompletedAt());
        assertTrue(mockTrip.getActualDistanceM() > 0);
        assertTrue(mockTrip.getActualDurationS() >= 1900);
        assertTrue(mockTrip.getCo2SavedGrams().doubleValue() > 0);

        // Verify driver earnings: 80% of 76,000 = 60,800
        assertEquals(BigDecimal.valueOf(60800), summary.getDriverEarnings().getNetEarningsVnd());
        assertEquals(BigDecimal.valueOf(15200), summary.getDriverEarnings().getPlatformFeeVnd());

        // Verify Redis cleanup & GEO pool restoration
        verify(tripTrackingRedisService).deleteTracking(tripId, driverId);
        verify(driverGeoRepository).updateLocation(eq(driverId), eq(mockTrip.getVehicleType()), eq(mockTrip.getDropoffLat()), eq(mockTrip.getDropoffLng()), eq(100));

        // Verify RabbitMQ publish
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_TRIP_COMPLETED), any(com.greenmobility.modules.trip.event.TripCompletedEvent.class));
    }

    @Test
    @DisplayName("AC-11: Tài xế hủy cuốc xe khi đang đến đón (DRIVER_ARRIVING -> CANCELLED)")
    void testCancelByDriver_Success() {
        mockTrip.setStatus(TripStatus.DRIVER_ARRIVING);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        DriverCancelResponseDto response = tripService.cancelByDriver(driverId, tripId, "Xe gặp sự cố thủng lốp");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        assertEquals("DRIVER", response.getCancelledBy());
        assertEquals(TripStatus.CANCELLED, mockTrip.getStatus());

        verify(tripTrackingRedisService).deleteTracking(tripId, driverId);
        verify(driverGeoRepository).updateLocation(eq(driverId), eq(mockTrip.getVehicleType()), eq(mockTrip.getPickupLat()), eq(mockTrip.getPickupLng()), eq(100));
    }

    @Test
    @DisplayName("Không cho phép tài xế hủy khi đang chở khách (IN_TRIP -> Ném BadRequestException)")
    void testCancelByDriver_ForbiddenWhenInTrip() {
        mockTrip.setStatus(TripStatus.IN_TRIP);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

        assertThrows(BadRequestException.class, () -> tripService.cancelByDriver(driverId, tripId, "Khách yêu cầu xuống xe"));
    }
}

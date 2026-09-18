package com.greenmobility.trip;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.KycStatus;
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
import com.greenmobility.modules.trip.entity.PaymentStatus;
import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.repository.TripRepository;
import com.greenmobility.modules.trip.service.CarbonEstimateService;
import com.greenmobility.modules.trip.service.FareCalculationService;
import com.greenmobility.modules.trip.service.RoutingService;
import com.greenmobility.modules.trip.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Sprint2AcceptanceCriteriaTest {

    @Mock private TripRepository tripRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverGeoRedisRepository driverGeoRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    private RoutingService routingService;
    private FareCalculationService fareCalculationService;
    private CarbonEstimateService carbonEstimateService;
    private TripService tripService;
    private MatchingEngineService matchingEngineService;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
    private final UUID customerId = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID tripId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        routingService = new RoutingService();
        fareCalculationService = new FareCalculationService();
        carbonEstimateService = new CarbonEstimateService();
        ReflectionTestUtils.setField(carbonEstimateService, "gridEmissionFactorGco2Kwh", 722.1);
        ReflectionTestUtils.setField(carbonEstimateService, "treeAbsorptionDailyKg", 0.06);
        ReflectionTestUtils.setField(carbonEstimateService, "ledBulbHourlyGco2", 7.221);

        matchingEngineService = new MatchingEngineService(
                driverGeoRepository,
                driverProfileRepository,
                vehicleRepository,
                tripRepository,
                redissonClient,
                messagingTemplate,
                fareCalculationService
        );
        ReflectionTestUtils.setField(matchingEngineService, "tier1RadiusKm", 1.5);
        ReflectionTestUtils.setField(matchingEngineService, "tier2RadiusKm", 3.0);
        ReflectionTestUtils.setField(matchingEngineService, "tier3RadiusKm", 5.0);
        ReflectionTestUtils.setField(matchingEngineService, "responseTimeoutSeconds", 1);

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
                matchingEngineService
        );
    }

    @Test
    @DisplayName("AC-01: Ước tính cước phí & CO2 giảm cho chặng đường 16.2km")
    void testAC01_EstimateTrip() {
        TripEstimateRequest req = new TripEstimateRequest();
        req.setPickupLat(10.776530);
        req.setPickupLng(106.700981);
        req.setPickupAddress("Nhà hát Thành phố, Quận 1");
        req.setDropoffLat(10.870020);
        req.setDropoffLng(106.803054);
        req.setDropoffAddress("Trường ĐH Công nghệ Thông tin");
        req.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);

        TripEstimateResponse res = tripService.estimateTrip(req);

        assertNotNull(res);
        assertTrue(res.getDistanceKm() >= 14.0 && res.getDistanceKm() <= 20.0);
        assertTrue(res.getFareAmountVnd().compareTo(BigDecimal.valueOf(50000)) > 0);
        assertTrue(res.getCarbonEstimate().getCo2SavedGrams().compareTo(BigDecimal.valueOf(700)) > 0);
    }

    @Test
    @DisplayName("AC-02: Khách hàng tạo yêu cầu đặt xe thành công khi chưa có cuốc dở dang")
    void testAC02_CreateTripRequest_Success() {
        when(tripRepository.existsActiveTripForCustomer(eq(customerId), any())).thenReturn(false);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(tripId);
            return t;
        });

        TripRequestDto req = new TripRequestDto();
        req.setPickupLat(10.776530);
        req.setPickupLng(106.700981);
        req.setPickupAddress("Nhà hát Thành phố, Quận 1");
        req.setDropoffLat(10.870020);
        req.setDropoffLng(106.803054);
        req.setDropoffAddress("Trường ĐH Công nghệ Thông tin");
        req.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        req.setPaymentMethod(PaymentMethod.CASH);

        TripResponseDto res = tripService.createTripRequest(customerId, req);

        assertNotNull(res);
        assertEquals(TripStatus.SEARCHING, res.getStatus());
        assertTrue(res.getTripCode().startsWith("GM-"));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("AC-03: Khách hàng tạo cuốc khi đang có cuốc dở dang -> ném BadRequestException")
    void testAC03_CreateTripRequest_Conflict() {
        when(tripRepository.existsActiveTripForCustomer(eq(customerId), any())).thenReturn(true);

        TripRequestDto req = new TripRequestDto();
        req.setPickupLat(10.776530);
        req.setPickupLng(106.700981);
        req.setPickupAddress("Nhà hát Thành phố, Quận 1");
        req.setDropoffLat(10.870020);
        req.setDropoffLng(106.803054);
        req.setDropoffAddress("Trường ĐH Công nghệ Thông tin");
        req.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        req.setPaymentMethod(PaymentMethod.CASH);

        assertThrows(BadRequestException.class, () -> tripService.createTripRequest(customerId, req));
        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-04 & AC-05: Tài xế chấp nhận nhận cuốc xe trong 15s")
    void testAC04_AcceptTrip_Success() {
        Trip trip = new Trip();
        trip.setId(tripId);
        trip.setTripCode("GM-20261005-1234");
        trip.setCustomerId(customerId);
        trip.setStatus(TripStatus.SEARCHING);
        trip.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        trip.setPickupAddress("Điểm đón");
        trip.setPickupGeom(gf.createPoint(new Coordinate(106.7, 10.7)));
        trip.setDropoffAddress("Điểm trả");
        trip.setDropoffGeom(gf.createPoint(new Coordinate(106.8, 10.8)));
        trip.setEstimatedDistanceM(10000);
        trip.setFareAmount(BigDecimal.valueOf(50000));
        trip.setFinalAmount(BigDecimal.valueOf(50000));
        trip.setCo2SavedGrams(BigDecimal.valueOf(500));

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        DriverProfile profile = new DriverProfile();
        profile.setId(driverId);
        profile.setUserId(UUID.randomUUID());
        profile.setKycStatus(KycStatus.APPROVED);
        profile.setIsActiveShift(true);
        when(driverProfileRepository.findById(driverId)).thenReturn(Optional.of(profile));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setDriverId(driverId);
        vehicle.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        vehicle.setLicensePlate("59-P1 999.99");
        when(vehicleRepository.findByDriverId(driverId)).thenReturn(Optional.of(vehicle));

        DriverTripResponseDto res = tripService.acceptTrip(driverId, tripId);

        assertNotNull(res);
        assertEquals(TripStatus.MATCHED, trip.getStatus());
        assertEquals(driverId, trip.getDriverId());
        assertEquals(vehicleId, trip.getVehicleId());
        verify(driverGeoRepository, times(1)).removeLocation(driverId, VehicleType.ELECTRIC_MOTORBIKE);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/trip/" + tripId), any(Object.class));
    }

    @Test
    @DisplayName("AC-06: Tài xế từ chối cuốc xe -> lưu cooldown 60s")
    void testAC06_DeclineTrip() {
        tripService.declineTrip(driverId, tripId, "Bận");
        verify(driverGeoRepository, times(1)).setDriverCooldown(driverId, tripId, 60);
    }

    @Test
    @DisplayName("AC-07: Khách hàng hủy cuốc xe thành công")
    void testAC07_CancelTrip() {
        Trip trip = new Trip();
        trip.setId(tripId);
        trip.setCustomerId(customerId);
        trip.setStatus(TripStatus.SEARCHING);
        trip.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        trip.setPickupGeom(gf.createPoint(new Coordinate(106.7, 10.7)));
        trip.setPickupAddress("Đón");
        trip.setDropoffGeom(gf.createPoint(new Coordinate(106.8, 10.8)));
        trip.setDropoffAddress("Trả");
        trip.setFareAmount(BigDecimal.valueOf(50000));
        trip.setFinalAmount(BigDecimal.valueOf(50000));

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        TripResponseDto res = tripService.cancelTrip(tripId, customerId, "Đổi ý");

        assertNotNull(res);
        assertEquals(TripStatus.CANCELLED, trip.getStatus());
        assertEquals("CUSTOMER", trip.getCancelledBy());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/trip/" + tripId), any(Object.class));
    }
}

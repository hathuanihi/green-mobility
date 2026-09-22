package com.greenmobility.drivervehicle;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.FaceVerificationFailedException;
import com.greenmobility.common.security.JwtTokenProvider;
import com.greenmobility.common.service.FileStorageService;
import com.greenmobility.modules.drivervehicle.dto.*;
import com.greenmobility.modules.drivervehicle.entity.*;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.FaceVerificationLogRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.drivervehicle.service.AdminDriverService;
import com.greenmobility.modules.drivervehicle.service.DriverPublicService;
import com.greenmobility.modules.drivervehicle.service.DriverService;
import com.greenmobility.modules.drivervehicle.service.FaceVerificationService;
import com.greenmobility.modules.identity.dto.AuthResponse;
import com.greenmobility.modules.identity.dto.LoginRequest;
import com.greenmobility.modules.identity.dto.RegisterRequest;
import com.greenmobility.modules.identity.dto.UserPublicDto;
import com.greenmobility.modules.identity.entity.Role;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import com.greenmobility.modules.identity.service.AuthService;
import com.greenmobility.modules.identity.service.UserPublicService;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Sprint1AcceptanceCriteriaTest {

    // Identity Mocks
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    // Driver & Vehicle Mocks
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private FaceVerificationLogRepository faceVerificationLogRepository;
    @Mock private UserPublicService userPublicService;
    @Mock private FileStorageService fileStorageService;
    @Mock private FaceVerificationService faceVerificationService;

    @Mock private DriverGeoRedisRepository driverGeoRepository;
    @Mock private TripRepository tripRepository;

    private DriverPublicService driverPublicService;
    private DriverService driverService;
    private AdminDriverService adminDriverService;

    private final UUID testUserId = UUID.randomUUID();
    private final UUID testDriverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                86400
        );
        driverPublicService = new DriverPublicService(driverProfileRepository);
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider, driverPublicService);

        driverService = new DriverService(
                driverProfileRepository,
                vehicleRepository,
                faceVerificationLogRepository,
                userPublicService,
                fileStorageService,
                faceVerificationService,
                driverGeoRepository,
                tripRepository
        );

        adminDriverService = new AdminDriverService(
                driverProfileRepository,
                vehicleRepository,
                userPublicService,
                faceVerificationLogRepository
        );
    }

    @Test
    @DisplayName("AC-01: Đăng ký tài khoản khách hàng / tài xế với SĐT hợp lệ")
    void testAC01_RegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "0901234567",
                "Password123@",
                "Phạm Hà Anh Thư",
                "anhthu@greenmobility.vn",
                Role.ROLE_DRIVER
        );

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByEmail("anhthu@greenmobility.vn")).thenReturn(false);
        when(passwordEncoder.encode("Password123@")).thenReturn("hashedPassword");

        User savedUser = new User("0901234567", "anhthu@greenmobility.vn", "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_DRIVER);
        savedUser.setId(testUserId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        assertEquals("0901234567", response.getPhoneNumber());
        assertEquals("ROLE_DRIVER", response.getRole());
        assertNotNull(response.getToken());
        assertEquals(86400, response.getExpiresIn());
    }

    @Test
    @DisplayName("AC-02: Đăng ký trùng số điện thoại đã tồn tại")
    void testAC02_RegisterDuplicatePhoneNumber() {
        RegisterRequest request = new RegisterRequest(
                "0901234567", "Password123@", "Phạm Hà Anh Thư", null, Role.ROLE_CUSTOMER
        );

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Số điện thoại này đã được đăng ký"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-03: Đăng nhập đúng mật khẩu, trả về Token 24h và kycStatus nếu là tài xế")
    void testAC03_LoginSuccess() {
        LoginRequest request = new LoginRequest("0901234567", "Password123@");

        User user = new User("0901234567", null, "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_DRIVER);
        user.setId(testUserId);

        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setKycStatus(KycStatus.PENDING);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123@", "hashedPassword")).thenReturn(true);
        when(driverProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(profile));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("ROLE_DRIVER", response.getRole());
        assertEquals("PENDING", response.getKycStatus());
        assertNotNull(response.getToken());
    }

    @Test
    @DisplayName("AC-04: Đăng nhập sai mật khẩu ném BadCredentialsException")
    void testAC04_LoginWrongPassword() {
        LoginRequest request = new LoginRequest("0901234567", "WrongPassword!");

        User user = new User("0901234567", null, "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_DRIVER);
        user.setId(testUserId);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("AC-05: Tài xế nộp hồ sơ KYC kèm đầy đủ 5 ảnh, lưu 4 URL vào DB và trả về KycSubmitResponse")
    void testAC05_SubmitKycWith5Images() {
        UserPublicDto userDto = new UserPublicDto(testUserId, "0901234567", "Phạm Hà Anh Thư", null, "ROLE_DRIVER", "ACTIVE");
        when(userPublicService.findById(testUserId)).thenReturn(Optional.of(userDto));

        KycSubmissionRequest request = new KycSubmissionRequest();
        request.setCitizenId("079203001234");
        request.setLicenseNumber("790123456789");
        request.setLicenseClass("A1");
        request.setVehicleType(VehicleType.ELECTRIC_MOTORBIKE);
        request.setMake("VinFast");
        request.setModel("Feliz S");
        request.setLicensePlate("59-P1 987.65");
        request.setColor("Xanh Rêu");
        request.setBatteryCapacityKwh(BigDecimal.valueOf(3.5));
        request.setRangePerChargeKm(198);
        request.setInspectionExpiryDate(LocalDate.of(2027, 12, 31));

        MockMultipartFile front = new MockMultipartFile("citizenFrontImage", "front.jpg", "image/jpeg", "front".getBytes());
        MockMultipartFile back = new MockMultipartFile("citizenBackImage", "back.jpg", "image/jpeg", "back".getBytes());
        MockMultipartFile license = new MockMultipartFile("licenseImage", "license.jpg", "image/jpeg", "license".getBytes());
        MockMultipartFile vehicleReg = new MockMultipartFile("vehicleRegistrationImage", "reg.jpg", "image/jpeg", "reg".getBytes());
        MockMultipartFile portrait = new MockMultipartFile("facePortraitImage", "portrait.jpg", "image/jpeg", "portrait".getBytes());

        request.setCitizenFrontImage(front);
        request.setCitizenBackImage(back);
        request.setLicenseImage(license);
        request.setVehicleRegistrationImage(vehicleReg);
        request.setFacePortraitImage(portrait);

        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);

        when(driverProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        when(fileStorageService.storeFile(eq(front), any(), eq("citizen_card_front"))).thenReturn("/uploads/kyc/" + testDriverId + "/citizen_card_front_1.jpg");
        when(fileStorageService.storeFile(eq(back), any(), eq("citizen_card_back"))).thenReturn("/uploads/kyc/" + testDriverId + "/citizen_card_back_1.jpg");
        when(fileStorageService.storeFile(eq(license), any(), eq("driver_license"))).thenReturn("/uploads/kyc/" + testDriverId + "/driver_license_1.jpg");
        when(fileStorageService.storeFile(eq(vehicleReg), any(), eq("vehicle_registration"))).thenReturn("/uploads/kyc/" + testDriverId + "/vehicle_registration_1.jpg");
        when(fileStorageService.storeFile(eq(portrait), any(), eq("face_reference"))).thenReturn("/uploads/kyc/" + testDriverId + "/face_reference_1.jpg");

        Double[] mockVector = new Double[512];
        Arrays.fill(mockVector, 0.1);
        when(faceVerificationService.extractFaceEmbedding(portrait)).thenReturn(mockVector);

        Vehicle vehicle = new Vehicle(testDriverId, VehicleType.ELECTRIC_MOTORBIKE, "VinFast", "Feliz S", "59-P1 987.65", "Xanh", BigDecimal.valueOf(3.5), 198, LocalDate.now());
        when(vehicleRepository.findByDriverId(testDriverId)).thenReturn(Optional.of(vehicle));

        KycSubmitResponse response = driverService.submitKyc(testUserId, request);

        assertNotNull(response);
        assertEquals(testDriverId, response.getDriverId());
        assertEquals("PENDING", response.getKycStatus());
        assertNotNull(response.getSubmittedAt());

        verify(driverProfileRepository).save(argThat(p ->
                p.getCitizenCardFrontUrl() != null && p.getCitizenCardFrontUrl().startsWith("/uploads/kyc/" + testDriverId) &&
                p.getCitizenCardBackUrl() != null && p.getCitizenCardBackUrl().startsWith("/uploads/kyc/" + testDriverId) &&
                p.getDriverLicenseUrl() != null && p.getDriverLicenseUrl().startsWith("/uploads/kyc/" + testDriverId) &&
                p.getFacePortraitUrl() != null && p.getFacePortraitUrl().startsWith("/uploads/kyc/" + testDriverId)
        ));
    }

    @Test
    @DisplayName("AC-06: Admin xem danh sách hồ sơ PENDING và lọc theo trạng thái")
    void testAC06_AdminGetPendingDrivers() {
        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);
        profile.setKycStatus(KycStatus.PENDING);

        when(driverProfileRepository.findByKycStatusOrderByCreatedAtDesc(KycStatus.PENDING))
                .thenReturn(List.of(profile));

        UserPublicDto userDto = new UserPublicDto(testUserId, "0901234567", "Phạm Hà Anh Thư", null, "ROLE_DRIVER", "ACTIVE");
        when(userPublicService.findUsersByIds(any())).thenReturn(Map.of(testUserId, userDto));

        Vehicle vehicle = new Vehicle(testDriverId, VehicleType.ELECTRIC_MOTORBIKE, "VinFast", "Feliz S", "59-P1 987.65", "Xanh", BigDecimal.valueOf(3.5), 198, LocalDate.now());
        when(vehicleRepository.findByDriverIdIn(any())).thenReturn(List.of(vehicle));

        List<AdminDriverResponse> pendingList = adminDriverService.getPendingDrivers();

        assertNotNull(pendingList);
        assertEquals(1, pendingList.size());
        assertEquals("Phạm Hà Anh Thư", pendingList.get(0).getFullName());
        assertEquals("VinFast Feliz S", pendingList.get(0).getVehicleModel());
        assertEquals("59-P1 987.65", pendingList.get(0).getLicensePlate());
        assertEquals("PENDING", pendingList.get(0).getKycStatus());
    }

    @Test
    @DisplayName("AC-07: Admin phê duyệt hồ sơ tài xế (APPROVED, is_verified = true)")
    void testAC07_AdminApproveKyc() {
        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);
        profile.setKycStatus(KycStatus.PENDING);

        Vehicle vehicle = new Vehicle(testDriverId, VehicleType.ELECTRIC_MOTORBIKE, "VinFast", "Feliz S", "59-P1 987.65", "Xanh", BigDecimal.valueOf(3.5), 198, LocalDate.now());
        vehicle.setIsVerified(false);

        when(driverProfileRepository.findById(testDriverId)).thenReturn(Optional.of(profile));
        when(vehicleRepository.findByDriverId(testDriverId)).thenReturn(Optional.of(vehicle));

        AdminKycActionResponse response = adminDriverService.approveKyc(testDriverId);

        assertNotNull(response);
        assertEquals(testDriverId, response.getDriverId());
        assertEquals("APPROVED", response.getKycStatus());
        assertEquals(KycStatus.APPROVED, profile.getKycStatus());
        assertTrue(vehicle.getIsVerified());
    }

    @Test
    @DisplayName("AC-08: Admin từ chối hồ sơ kèm lý do (REJECTED, kyc_rejection_reason)")
    void testAC08_AdminRejectKyc() {
        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);
        profile.setKycStatus(KycStatus.PENDING);

        Vehicle vehicle = new Vehicle(testDriverId, VehicleType.ELECTRIC_MOTORBIKE, "VinFast", "Feliz S", "59-P1 987.65", "Xanh", BigDecimal.valueOf(3.5), 198, LocalDate.now());

        when(driverProfileRepository.findById(testDriverId)).thenReturn(Optional.of(profile));
        when(vehicleRepository.findByDriverId(testDriverId)).thenReturn(Optional.of(vehicle));

        String reason = "Ảnh Giấy phép lái xe bị mờ không đọc rõ số hiệu";
        AdminKycActionResponse response = adminDriverService.rejectKyc(testDriverId, reason);

        assertNotNull(response);
        assertEquals(testDriverId, response.getDriverId());
        assertEquals("REJECTED", response.getKycStatus());
        assertEquals(KycStatus.REJECTED, profile.getKycStatus());
        assertEquals(reason, profile.getKycRejectionReason());
        assertFalse(profile.getIsActiveShift());
    }

    @Test
    @DisplayName("AC-09: Tài xế đã duyệt KYC chụp selfie khớp mặt (>= 0.75), bật ca thành công")
    void testAC09_ShiftFaceVerificationSuccess() {
        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);
        profile.setKycStatus(KycStatus.APPROVED);

        Double[] refVector = new Double[512];
        Arrays.fill(refVector, 0.1);
        profile.setFaceEncodingVector(refVector);

        when(driverProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(profile));

        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "selfie.jpg", "image/jpeg", "selfieData".getBytes());
        when(fileStorageService.storeFile(eq(selfie), any(), eq("selfie"))).thenReturn("/uploads/shifts/" + testDriverId + "/selfie_1.jpg");

        Double[] selfieVector = new Double[512];
        Arrays.fill(selfieVector, 0.1);
        when(faceVerificationService.extractFaceEmbedding(selfie)).thenReturn(selfieVector);
        when(faceVerificationService.calculateCosineSimilarity(refVector, selfieVector)).thenReturn(0.8642);

        FaceVerifyResponse response = driverService.verifyShiftFace(testUserId, selfie);

        assertNotNull(response);
        assertTrue(response.isPassed());
        assertTrue(response.isActiveShift());
        assertEquals(BigDecimal.valueOf(0.8642), response.getSimilarityScore());
        assertTrue(profile.getIsActiveShift());

        verify(faceVerificationLogRepository).save(argThat(log ->
                log.getIsPassed() &&
                log.getDriverId().equals(testDriverId) &&
                log.getSimilarityScore().compareTo(BigDecimal.valueOf(0.75)) >= 0
        ));
    }

    @Test
    @DisplayName("AC-10: Chưa duyệt KYC ném AccessDeniedException (HTTP 403), không khớp mặt ném FaceVerificationFailedException (HTTP 400)")
    void testAC10_ShiftFaceVerificationFailureCases() {
        // Trường hợp 1: Tài xế chưa được duyệt KYC -> Phải từ chối với AccessDeniedException (HTTP 403)
        DriverProfile pendingProfile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        pendingProfile.setId(testDriverId);
        pendingProfile.setKycStatus(KycStatus.PENDING);

        when(driverProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(pendingProfile));

        MockMultipartFile selfie = new MockMultipartFile("selfieImage", "selfie.jpg", "image/jpeg", "selfieData".getBytes());
        assertThrows(AccessDeniedException.class, () -> driverService.verifyShiftFace(testUserId, selfie));

        // Trường hợp 2: Đã duyệt KYC nhưng khuôn mặt không khớp (< 0.75) -> Ném FaceVerificationFailedException kèm data log
        pendingProfile.setKycStatus(KycStatus.APPROVED);
        Double[] refVector = new Double[512];
        Arrays.fill(refVector, 0.1);
        pendingProfile.setFaceEncodingVector(refVector);

        when(fileStorageService.storeFile(eq(selfie), any(), eq("selfie"))).thenReturn("/uploads/shifts/" + testDriverId + "/selfie_fail.jpg");

        Double[] differentVector = new Double[512];
        Arrays.fill(differentVector, -0.1);
        when(faceVerificationService.extractFaceEmbedding(selfie)).thenReturn(differentVector);
        when(faceVerificationService.calculateCosineSimilarity(refVector, differentVector)).thenReturn(0.5210);

        FaceVerificationFailedException ex = assertThrows(FaceVerificationFailedException.class,
                () -> driverService.verifyShiftFace(testUserId, selfie));

        assertNotNull(ex.getData());
        assertFalse(ex.getData().isPassed());
        assertFalse(ex.getData().isActiveShift());
        assertEquals(0, ex.getData().getSimilarityScore().compareTo(new BigDecimal("0.5210")));
        assertFalse(pendingProfile.getIsActiveShift());

        verify(faceVerificationLogRepository).save(argThat(log ->
                !log.getIsPassed() &&
                log.getDriverId().equals(testDriverId)
        ));
    }

    @Test
    @DisplayName("Tắt ca làm việc: Cập nhật isActiveShift=false và xóa vị trí khỏi Redis GEO")
    void testEndShift_Success() {
        DriverProfile profile = new DriverProfile(testUserId, "079203001234", "790123456789", "A1");
        profile.setId(testDriverId);
        profile.setIsActiveShift(true);

        Vehicle vehicle = new Vehicle(testDriverId, VehicleType.ELECTRIC_MOTORBIKE, "VinFast", "Feliz S", "59-P1 999.99", "Đen",
                new BigDecimal("3.5"), 120, LocalDate.now().plusYears(1));

        when(driverProfileRepository.findByUserId(testUserId)).thenReturn(Optional.of(profile));
        when(tripRepository.findFirstByDriverIdAndStatusInOrderByRequestedAtDesc(eq(testDriverId), any())).thenReturn(Optional.empty());
        when(vehicleRepository.findByDriverId(testDriverId)).thenReturn(Optional.of(vehicle));

        driverService.endShift(testUserId);

        assertFalse(profile.getIsActiveShift());
        verify(driverProfileRepository).save(profile);
        verify(driverGeoRepository).removeLocation(testDriverId, VehicleType.ELECTRIC_MOTORBIKE);
        verify(driverGeoRepository).clearPendingDispatch(testDriverId);
    }
}

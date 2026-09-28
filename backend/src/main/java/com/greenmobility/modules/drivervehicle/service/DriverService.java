package com.greenmobility.modules.drivervehicle.service;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.FaceVerificationFailedException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.service.FileStorageService;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
import com.greenmobility.modules.drivervehicle.dto.FaceVerifyResponse;
import com.greenmobility.modules.drivervehicle.dto.KycSubmissionRequest;
import com.greenmobility.modules.drivervehicle.dto.KycSubmitResponse;
import com.greenmobility.modules.drivervehicle.dto.VehicleResponse;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.FaceVerificationLog;
import com.greenmobility.modules.drivervehicle.entity.KycStatus;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.FaceVerificationLogRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.identity.dto.UserPublicDto;
import com.greenmobility.modules.identity.service.UserPublicService;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.trip.entity.TripStatus;
import com.greenmobility.modules.trip.repository.TripRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final FaceVerificationLogRepository faceVerificationLogRepository;
    private final UserPublicService userPublicService;
    private final FileStorageService fileStorageService;
    private final FaceVerificationService faceVerificationService;
    private final DriverGeoRedisRepository driverGeoRepository;
    private final TripRepository tripRepository;

    public DriverService(
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            FaceVerificationLogRepository faceVerificationLogRepository,
            UserPublicService userPublicService,
            FileStorageService fileStorageService,
            FaceVerificationService faceVerificationService,
            DriverGeoRedisRepository driverGeoRepository,
            TripRepository tripRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.faceVerificationLogRepository = faceVerificationLogRepository;
        this.userPublicService = userPublicService;
        this.fileStorageService = fileStorageService;
        this.faceVerificationService = faceVerificationService;
        this.driverGeoRepository = driverGeoRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public KycSubmitResponse submitKyc(UUID userId, KycSubmissionRequest request) {
        UserPublicDto user = userPublicService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 1. Kiểm tra đủ 5 ảnh minh chứng bắt buộc theo AC-05
        if (request.getCitizenFrontImage() == null || request.getCitizenFrontImage().isEmpty() ||
            request.getCitizenBackImage() == null || request.getCitizenBackImage().isEmpty() ||
            request.getLicenseImage() == null || request.getLicenseImage().isEmpty() ||
            request.getVehicleRegistrationImage() == null || request.getVehicleRegistrationImage().isEmpty() ||
            request.getFacePortraitImage() == null || request.getFacePortraitImage().isEmpty()) {
            throw new BadRequestException("Vui lòng tải lên đầy đủ 5 ảnh tài liệu minh chứng (CCCD mặt trước, mặt sau, GPLX, Cà vẹt xe và Ảnh chân dung chuẩn)");
        }

        // 2. Kiểm tra trùng lặp thông tin
        driverProfileRepository.findByCitizenId(request.getCitizenId()).ifPresent(existing -> {
            if (!existing.getUserId().equals(userId)) {
                throw new BadRequestException("Số CCCD này đã được đăng ký bởi tài xế khác");
            }
        });

        driverProfileRepository.findByDriverLicenseNumber(request.getLicenseNumber()).ifPresent(existing -> {
            if (!existing.getUserId().equals(userId)) {
                throw new BadRequestException("Số GPLX này đã được đăng ký bởi tài xế khác");
            }
        });

        vehicleRepository.findByLicensePlate(request.getLicensePlate()).ifPresent(existing -> {
            driverProfileRepository.findByUserId(userId).ifPresent(p -> {
                if (!existing.getDriverId().equals(p.getId())) {
                    throw new BadRequestException("Biển số xe này đã được đăng ký trong hệ thống");
                }
            });
        });

        // 3. Khởi tạo hoặc lấy DriverProfile để có driverId cho đường dẫn ảnh
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElse(new DriverProfile(userId, request.getCitizenId(), request.getLicenseNumber(), request.getLicenseClass()));

        if (profile.getId() == null) {
            profile = driverProfileRepository.save(profile);
        }
        UUID driverId = profile.getId();

        // 4. Lưu 5 ảnh tài liệu với Key Prefix chuẩn SPRINT_1_SPEC mục 3
        String subDir = "kyc/" + driverId;
        String citizenFrontUrl = fileStorageService.storeFile(request.getCitizenFrontImage(), subDir, "citizen_card_front");
        String citizenBackUrl = fileStorageService.storeFile(request.getCitizenBackImage(), subDir, "citizen_card_back");
        String licenseUrl = fileStorageService.storeFile(request.getLicenseImage(), subDir, "driver_license");
        String vehicleRegUrl = fileStorageService.storeFile(request.getVehicleRegistrationImage(), subDir, "vehicle_registration");
        String facePortraitUrl = fileStorageService.storeFile(request.getFacePortraitImage(), subDir, "face_reference");

        // 5. Trích xuất Face Vector 512 chiều từ ảnh chân dung chuẩn
        Double[] faceVector = faceVerificationService.extractFaceEmbedding(request.getFacePortraitImage());

        // 6. Cập nhật và lưu DriverProfile
        profile.setCitizenId(request.getCitizenId());
        profile.setDriverLicenseNumber(request.getLicenseNumber());
        profile.setLicenseClass(request.getLicenseClass());
        profile.setKycStatus(KycStatus.PENDING);
        profile.setKycRejectionReason(null);
        profile.setCitizenCardFrontUrl(citizenFrontUrl);
        profile.setCitizenCardBackUrl(citizenBackUrl);
        profile.setDriverLicenseUrl(licenseUrl);
        profile.setFacePortraitUrl(facePortraitUrl);
        if (faceVector != null) {
            profile.setFaceEncodingVector(faceVector);
        }

        DriverProfile savedProfile = driverProfileRepository.save(profile);

        // 7. Cập nhật và lưu Vehicle
        Vehicle vehicle = vehicleRepository.findByDriverId(savedProfile.getId())
                .orElse(new Vehicle(savedProfile.getId(), request.getVehicleType(), request.getMake(),
                        request.getModel(), request.getLicensePlate(), request.getColor(),
                        request.getBatteryCapacityKwh(), request.getRangePerChargeKm(), request.getInspectionExpiryDate()));

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setColor(request.getColor());
        vehicle.setBatteryCapacityKwh(request.getBatteryCapacityKwh());
        vehicle.setRangePerChargeKm(request.getRangePerChargeKm());
        vehicle.setInspectionExpiryDate(request.getInspectionExpiryDate());
        if (vehicleRegUrl != null) {
            vehicle.setRegistrationCertificateUrl(vehicleRegUrl);
        }
        vehicle.setIsVerified(false);

        vehicleRepository.save(vehicle);

        return new KycSubmitResponse(savedProfile.getId(), savedProfile.getKycStatus().name(), savedProfile.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getProfile(UUID userId) {
        UserPublicDto user = userPublicService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có thông tin hồ sơ tài xế cho tài khoản này"));

        Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);

        return mapToProfileResponse(user, profile, vehicle);
    }

    @Transactional
    public FaceVerifyResponse verifyShiftFace(UUID userId, MultipartFile selfieImage) {
        if (selfieImage == null || selfieImage.isEmpty()) {
            throw new BadRequestException("Vui lòng tải lên ảnh chụp selfie khuôn mặt");
        }

        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        // Quy tắc 1 (Mục 4.6): Kiểm tra KYC == APPROVED, nếu chưa duyệt trả 403 Forbidden
        if (profile.getKycStatus() != KycStatus.APPROVED) {
            throw new AccessDeniedException("Hồ sơ tài xế chưa được phê duyệt KYC. Trạng thái hiện tại: " + profile.getKycStatus() + ". Vui lòng chờ Admin duyệt trước khi bật ca làm việc!");
        }

        if (profile.getFaceEncodingVector() == null || profile.getFaceEncodingVector().length == 0) {
            throw new BadRequestException("Chưa có dữ liệu khuôn mặt mẫu trong hồ sơ KYC để đối chiếu");
        }

        // Lưu ảnh selfie ca làm việc theo quy ước: shifts/{driverId}/{date}/selfie_{timestamp}.jpg
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String selfieUrl = fileStorageService.storeFile(selfieImage, "shifts/" + profile.getId() + "/" + today, "selfie");

        // Trích xuất vector từ selfie và so khớp Cosine
        Double[] selfieVector = faceVerificationService.extractFaceEmbedding(selfieImage);
        double similarity = faceVerificationService.calculateCosineSimilarity(profile.getFaceEncodingVector(), selfieVector);
        BigDecimal score = BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);

        boolean isPassed = similarity >= FaceVerificationService.SIMILARITY_THRESHOLD;
        Instant verifiedAt = Instant.now();

        // Lưu nhật ký xác thực khuôn mặt
        FaceVerificationLog log = new FaceVerificationLog(profile.getId(), selfieUrl != null ? selfieUrl : "", score, isPassed);
        faceVerificationLogRepository.save(log);

        if (isPassed) {
            profile.setIsActiveShift(true);
            driverProfileRepository.save(profile);
            return new FaceVerifyResponse(true, score, true, verifiedAt);
        } else {
            profile.setIsActiveShift(false);
            driverProfileRepository.save(profile);
            vehicleRepository.findByDriverId(profile.getId()).ifPresent(vehicle -> {
                driverGeoRepository.removeLocation(profile.getId(), vehicle.getVehicleType());
            });
            driverGeoRepository.clearPendingDispatch(profile.getId());
            BigDecimal percentage = score.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            throw new FaceVerificationFailedException(
                    "Xác thực khuôn mặt thất bại (Độ khớp: " + percentage + "%). Khuôn mặt không trùng khớp với hồ sơ đăng ký tài xế!",
                    new FaceVerifyResponse(false, score, false, verifiedAt)
            );
        }
    }

    @Transactional
    public void endShift(UUID userId) {
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        if (!Boolean.TRUE.equals(profile.getIsActiveShift())) {
            return;
        }

        // Validate driver has no active ongoing trips
        List<TripStatus> activeStatuses = List.of(
                TripStatus.MATCHED,
                TripStatus.DRIVER_ARRIVING,
                TripStatus.ARRIVED,
                TripStatus.IN_TRIP
        );
        if (tripRepository.findFirstByDriverIdAndStatusInOrderByRequestedAtDesc(profile.getId(), activeStatuses).isPresent()) {
            throw new BadRequestException("Bạn đang có cuốc xe chưa hoàn thành! Không thể tắt ca làm việc khi đang phục vụ khách.");
        }

        profile.setIsActiveShift(false);
        driverProfileRepository.save(profile);

        // Remove location from Redis GEO available set and clear pending dispatches
        vehicleRepository.findByDriverId(profile.getId()).ifPresent(vehicle -> {
            driverGeoRepository.removeLocation(profile.getId(), vehicle.getVehicleType());
        });
        driverGeoRepository.clearPendingDispatch(profile.getId());
    }

    private DriverProfileResponse mapToProfileResponse(UserPublicDto user, DriverProfile profile, Vehicle vehicle) {
        VehicleResponse vehicleResponse = null;
        if (vehicle != null) {
            vehicleResponse = new VehicleResponse(
                    vehicle.getId(),
                    vehicle.getVehicleType().name(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getLicensePlate(),
                    vehicle.getColor(),
                    vehicle.getBatteryCapacityKwh(),
                    vehicle.getRangePerChargeKm(),
                    vehicle.getRegistrationCertificateUrl(),
                    vehicle.getInspectionExpiryDate(),
                    vehicle.getIsVerified()
            );
        }

        return new DriverProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                profile.getCitizenId(),
                profile.getDriverLicenseNumber(),
                profile.getLicenseClass(),
                profile.getKycStatus().name(),
                profile.getKycRejectionReason(),
                profile.getCitizenCardFrontUrl(),
                profile.getCitizenCardBackUrl(),
                profile.getDriverLicenseUrl(),
                profile.getFacePortraitUrl(),
                profile.getIsActiveShift(),
                profile.getRatingAvg(),
                profile.getTotalTripsCompleted(),
                profile.getTotalCo2SavedKg(),
                vehicleResponse,
                profile.getCreatedAt()
        );
    }
}

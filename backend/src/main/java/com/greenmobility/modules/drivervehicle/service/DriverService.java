package com.greenmobility.modules.drivervehicle.service;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.service.FileStorageService;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
import com.greenmobility.modules.drivervehicle.dto.FaceVerifyResponse;
import com.greenmobility.modules.drivervehicle.dto.KycSubmissionRequest;
import com.greenmobility.modules.drivervehicle.dto.VehicleResponse;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.FaceVerificationLog;
import com.greenmobility.modules.drivervehicle.entity.KycStatus;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.FaceVerificationLogRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final FaceVerificationLogRepository faceVerificationLogRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FaceVerificationService faceVerificationService;

    public DriverService(
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            FaceVerificationLogRepository faceVerificationLogRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            FaceVerificationService faceVerificationService) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.faceVerificationLogRepository = faceVerificationLogRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.faceVerificationService = faceVerificationService;
    }

    @Transactional
    public DriverProfileResponse submitKyc(UUID userId, KycSubmissionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Kiểm tra trùng lặp thông tin
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

        // 1. Lưu file ảnh tài liệu lên Storage
        String citizenFrontUrl = fileStorageService.storeFile(request.getCitizenFrontImage(), "kyc/" + userId);
        String citizenBackUrl = fileStorageService.storeFile(request.getCitizenBackImage(), "kyc/" + userId);
        String licenseUrl = fileStorageService.storeFile(request.getLicenseImage(), "kyc/" + userId);
        String vehicleRegUrl = fileStorageService.storeFile(request.getVehicleRegistrationImage(), "kyc/" + userId);
        String facePortraitUrl = fileStorageService.storeFile(request.getFacePortraitImage(), "kyc/" + userId);

        // 2. Trích xuất Face Vector 512 chiều từ ảnh chân dung chuẩn
        Double[] faceVector = null;
        if (request.getFacePortraitImage() != null && !request.getFacePortraitImage().isEmpty()) {
            faceVector = faceVerificationService.extractFaceEmbedding(request.getFacePortraitImage());
        }

        // 3. Cập nhật hoặc tạo mới DriverProfile
        DriverProfile profile = driverProfileRepository.findByUserId(userId)
                .orElse(new DriverProfile(userId, request.getCitizenId(), request.getLicenseNumber(), request.getLicenseClass()));

        profile.setCitizenId(request.getCitizenId());
        profile.setDriverLicenseNumber(request.getLicenseNumber());
        profile.setLicenseClass(request.getLicenseClass());
        profile.setKycStatus(KycStatus.PENDING);
        profile.setKycRejectionReason(null);
        if (faceVector != null) {
            profile.setFaceEncodingVector(faceVector);
        }

        DriverProfile savedProfile = driverProfileRepository.save(profile);

        // 4. Cập nhật hoặc tạo mới Vehicle
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

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return mapToProfileResponse(user, savedProfile, savedVehicle);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
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

        if (profile.getKycStatus() != KycStatus.APPROVED) {
            throw new BadRequestException("Hồ sơ tài xế chưa được phê duyệt KYC. Trạng thái hiện tại: " + profile.getKycStatus());
        }

        if (profile.getFaceEncodingVector() == null || profile.getFaceEncodingVector().length == 0) {
            throw new BadRequestException("Chưa có dữ liệu khuôn mặt mẫu trong hồ sơ KYC để đối chiếu");
        }

        // Lưu ảnh selfie ca làm việc
        String selfieUrl = fileStorageService.storeFile(selfieImage, "shifts/" + profile.getId());

        // Trích xuất vector từ selfie và so khớp Cosine
        Double[] selfieVector = faceVerificationService.extractFaceEmbedding(selfieImage);
        double similarity = faceVerificationService.calculateCosineSimilarity(profile.getFaceEncodingVector(), selfieVector);
        BigDecimal score = BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);

        boolean isPassed = similarity >= FaceVerificationService.SIMILARITY_THRESHOLD;

        // Lưu nhật ký xác thực khuôn mặt
        FaceVerificationLog log = new FaceVerificationLog(profile.getId(), selfieUrl != null ? selfieUrl : "", score, isPassed);
        faceVerificationLogRepository.save(log);

        if (isPassed) {
            profile.setIsActiveShift(true);
            driverProfileRepository.save(profile);
            return new FaceVerifyResponse(true, score, true, Instant.now());
        } else {
            profile.setIsActiveShift(false);
            driverProfileRepository.save(profile);
            throw new BadRequestException("Xác thực khuôn mặt thất bại (Độ tương đồng: " + 
                    score.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + 
                    "%). Khuôn mặt không khớp với hồ sơ đăng ký!");
        }
    }

    private DriverProfileResponse mapToProfileResponse(User user, DriverProfile profile, Vehicle vehicle) {
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
                profile.getIsActiveShift(),
                profile.getRatingAvg(),
                profile.getTotalTripsCompleted(),
                profile.getTotalCo2SavedKg(),
                vehicleResponse,
                profile.getCreatedAt()
        );
    }
}

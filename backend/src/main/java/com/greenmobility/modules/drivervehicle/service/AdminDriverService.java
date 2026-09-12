package com.greenmobility.modules.drivervehicle.service;

import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.modules.drivervehicle.dto.AdminDriverResponse;
import com.greenmobility.modules.drivervehicle.dto.AdminKycActionResponse;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserPublicService userPublicService;
    private final FaceVerificationLogRepository faceVerificationLogRepository;

    public AdminDriverService(
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            UserPublicService userPublicService,
            FaceVerificationLogRepository faceVerificationLogRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.userPublicService = userPublicService;
        this.faceVerificationLogRepository = faceVerificationLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDriverResponse> getPendingDrivers() {
        List<DriverProfile> pendingProfiles = driverProfileRepository.findByKycStatusOrderByCreatedAtDesc(KycStatus.PENDING);
        return mapProfilesToAdminResponses(pendingProfiles);
    }

    @Transactional(readOnly = true)
    public List<AdminDriverResponse> getAllDrivers(KycStatus status) {
        List<DriverProfile> profiles;
        if (status != null) {
            profiles = driverProfileRepository.findByKycStatusOrderByCreatedAtDesc(status);
        } else {
            profiles = driverProfileRepository.findAllByOrderByCreatedAtDesc();
        }
        return mapProfilesToAdminResponses(profiles);
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverDetail(UUID driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + driverId));

        UserPublicDto user = userPublicService.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng tương ứng"));

        Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);
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

    @Transactional
    public AdminKycActionResponse approveKyc(UUID driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + driverId));

        profile.setKycStatus(KycStatus.APPROVED);
        profile.setKycRejectionReason(null);
        driverProfileRepository.save(profile);

        vehicleRepository.findByDriverId(profile.getId()).ifPresent(vehicle -> {
            vehicle.setIsVerified(true);
            vehicleRepository.save(vehicle);
        });

        return new AdminKycActionResponse(profile.getId(), KycStatus.APPROVED.name());
    }

    @Transactional
    public AdminKycActionResponse rejectKyc(UUID driverId, String reason) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + driverId));

        profile.setKycStatus(KycStatus.REJECTED);
        profile.setKycRejectionReason(reason);
        profile.setIsActiveShift(false);
        driverProfileRepository.save(profile);

        vehicleRepository.findByDriverId(profile.getId()).ifPresent(vehicle -> {
            vehicle.setIsVerified(false);
            vehicleRepository.save(vehicle);
        });

        return new AdminKycActionResponse(profile.getId(), KycStatus.REJECTED.name());
    }

    @Transactional(readOnly = true)
    public List<FaceVerificationLog> getFaceLogs(UUID driverId) {
        return faceVerificationLogRepository.findByDriverIdOrderByVerifiedAtDesc(driverId);
    }

    private List<AdminDriverResponse> mapProfilesToAdminResponses(List<DriverProfile> profiles) {
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> userIds = profiles.stream().map(DriverProfile::getUserId).toList();
        List<UUID> driverIds = profiles.stream().map(DriverProfile::getId).toList();

        Map<UUID, UserPublicDto> userMap = userPublicService.findUsersByIds(userIds);
        Map<UUID, Vehicle> vehicleMap = vehicleRepository.findByDriverIdIn(driverIds)
                .stream().collect(Collectors.toMap(Vehicle::getDriverId, v -> v, (v1, v2) -> v1));

        List<AdminDriverResponse> responseList = new ArrayList<>();
        for (DriverProfile profile : profiles) {
            UserPublicDto user = userMap.get(profile.getUserId());
            Vehicle vehicle = vehicleMap.get(profile.getId());

            responseList.add(new AdminDriverResponse(
                    profile.getId(),
                    user != null ? user.getFullName() : "",
                    user != null ? user.getPhoneNumber() : "",
                    profile.getCitizenId(),
                    profile.getDriverLicenseNumber(),
                    vehicle != null ? vehicle.getMake() + " " + vehicle.getModel() : "",
                    vehicle != null ? vehicle.getLicensePlate() : "",
                    vehicle != null ? vehicle.getBatteryCapacityKwh() : null,
                    profile.getKycStatus().name(),
                    profile.getCreatedAt()
            ));
        }

        return responseList;
    }
}

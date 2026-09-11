package com.greenmobility.modules.drivervehicle.service;

import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.modules.drivervehicle.dto.AdminDriverResponse;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminDriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final FaceVerificationLogRepository faceVerificationLogRepository;

    public AdminDriverService(
            DriverProfileRepository driverProfileRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            FaceVerificationLogRepository faceVerificationLogRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.faceVerificationLogRepository = faceVerificationLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDriverResponse> getPendingDrivers() {
        List<DriverProfile> pendingProfiles = driverProfileRepository.findByKycStatusOrderByCreatedAtDesc(KycStatus.PENDING);
        List<AdminDriverResponse> responseList = new ArrayList<>();

        for (DriverProfile profile : pendingProfiles) {
            User user = userRepository.findById(profile.getUserId()).orElse(null);
            Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId()).orElse(null);

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

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverDetail(UUID driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + driverId));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng tương ứng"));

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
                profile.getIsActiveShift(),
                profile.getRatingAvg(),
                profile.getTotalTripsCompleted(),
                profile.getTotalCo2SavedKg(),
                vehicleResponse,
                profile.getCreatedAt()
        );
    }

    @Transactional
    public void approveKyc(UUID driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + driverId));

        profile.setKycStatus(KycStatus.APPROVED);
        profile.setKycRejectionReason(null);
        driverProfileRepository.save(profile);

        vehicleRepository.findByDriverId(profile.getId()).ifPresent(vehicle -> {
            vehicle.setIsVerified(true);
            vehicleRepository.save(vehicle);
        });
    }

    @Transactional
    public void rejectKyc(UUID driverId, String reason) {
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
    }

    @Transactional(readOnly = true)
    public List<FaceVerificationLog> getFaceLogs(UUID driverId) {
        return faceVerificationLogRepository.findByDriverIdOrderByVerifiedAtDesc(driverId);
    }
}

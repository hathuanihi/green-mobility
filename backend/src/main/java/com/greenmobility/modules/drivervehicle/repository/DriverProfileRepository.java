package com.greenmobility.modules.drivervehicle.repository;

import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUserId(UUID userId);
    Optional<DriverProfile> findByCitizenId(String citizenId);
    Optional<DriverProfile> findByDriverLicenseNumber(String driverLicenseNumber);
    List<DriverProfile> findByKycStatusOrderByCreatedAtDesc(KycStatus kycStatus);
    boolean existsByCitizenId(String citizenId);
    boolean existsByDriverLicenseNumber(String driverLicenseNumber);
}

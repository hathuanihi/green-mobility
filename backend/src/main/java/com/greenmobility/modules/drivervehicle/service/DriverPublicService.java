package com.greenmobility.modules.drivervehicle.service;

import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class DriverPublicService {

    private final DriverProfileRepository driverProfileRepository;

    public DriverPublicService(DriverProfileRepository driverProfileRepository) {
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> getKycStatusByUserId(UUID userId) {
        if (userId == null) return Optional.empty();
        return driverProfileRepository.findByUserId(userId)
                .map(profile -> profile.getKycStatus().name());
    }
}

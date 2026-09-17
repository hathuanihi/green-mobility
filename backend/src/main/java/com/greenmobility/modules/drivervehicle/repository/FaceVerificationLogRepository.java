package com.greenmobility.modules.drivervehicle.repository;

import com.greenmobility.modules.drivervehicle.entity.FaceVerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FaceVerificationLogRepository extends JpaRepository<FaceVerificationLog, UUID> {
    List<FaceVerificationLog> findByDriverIdOrderByVerifiedAtDesc(UUID driverId);
}

package com.greenmobility.modules.fraud.repository;

import com.greenmobility.modules.fraud.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    List<FraudAlert> findByOrderByCreatedAtDesc();
    List<FraudAlert> findByResolutionStatusOrderByCreatedAtDesc(String resolutionStatus);
    long countByResolutionStatus(String resolutionStatus);
}

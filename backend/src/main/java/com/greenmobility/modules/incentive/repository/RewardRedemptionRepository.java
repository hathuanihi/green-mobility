package com.greenmobility.modules.incentive.repository;

import com.greenmobility.modules.incentive.domain.RewardRedemption;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RewardRedemption r WHERE r.id = :id")
    Optional<RewardRedemption> findByIdWithLock(@Param("id") UUID id);

    Optional<RewardRedemption> findByRedemptionCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RewardRedemption r WHERE r.redemptionCode = :code")
    Optional<RewardRedemption> findByRedemptionCodeWithLock(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RewardRedemption r WHERE r.appliedTripId = :tripId AND r.status = 'RESERVED'")
    Optional<RewardRedemption> findReservedByAppliedTripIdWithLock(@Param("tripId") UUID tripId);

    long countByUserIdAndRewardIdAndStatusNotIn(UUID userId, UUID rewardId, Collection<String> excludedStatuses);

    Page<RewardRedemption> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT r FROM RewardRedemption r WHERE r.userId = :userId " +
            "AND (:status IS NULL OR :status = '' OR r.status = :status) " +
            "ORDER BY r.createdAt DESC")
    Page<RewardRedemption> findByUserIdFiltered(@Param("userId") UUID userId,
                                                @Param("status") String status,
                                                Pageable pageable);

    @Query("SELECT r FROM RewardRedemption r WHERE r.status IN ('AVAILABLE', 'ISSUED') " +
            "AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<RewardRedemption> findExpiredAvailableRedemptions(@Param("now") Instant now, Pageable pageable);
}

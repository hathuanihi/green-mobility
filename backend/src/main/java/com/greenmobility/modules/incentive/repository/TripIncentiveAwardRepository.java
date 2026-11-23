package com.greenmobility.modules.incentive.repository;

import com.greenmobility.modules.incentive.domain.TripIncentiveAward;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripIncentiveAwardRepository extends JpaRepository<TripIncentiveAward, UUID> {

    @Modifying
    @Query(value = "INSERT INTO trip_incentive_awards (trip_id, customer_id, driver_user_id, status) " +
            "VALUES (:tripId, :customerId, :driverUserId, 'PENDING_CONDITIONS') " +
            "ON CONFLICT (trip_id) DO NOTHING", nativeQuery = true)
    void insertIfNotExists(@Param("tripId") UUID tripId,
                           @Param("customerId") UUID customerId,
                           @Param("driverUserId") UUID driverUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TripIncentiveAward a WHERE a.tripId = :tripId")
    Optional<TripIncentiveAward> findByTripIdWithLock(@Param("tripId") UUID tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM TripIncentiveAward a WHERE a.id = :id")
    Optional<TripIncentiveAward> findByIdWithLock(@Param("id") UUID id);

    Optional<TripIncentiveAward> findByTripId(UUID tripId);

    @Query("SELECT a FROM TripIncentiveAward a WHERE a.status = 'PENDING_CONDITIONS' AND a.createdAt < :cutoff ORDER BY a.createdAt ASC")
    List<TripIncentiveAward> findStuckPendingAwards(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("SELECT a FROM TripIncentiveAward a WHERE (:status IS NULL OR :status = '' OR a.status = :status) ORDER BY a.createdAt DESC")
    Page<TripIncentiveAward> findByStatusFiltered(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);
}

package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.WithdrawalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WithdrawalRequest w WHERE w.id = :id")
    Optional<WithdrawalRequest> findByIdWithLock(@Param("id") UUID id);

    List<WithdrawalRequest> findByDriverUserIdOrderByCreatedAtDesc(UUID driverUserId, Pageable pageable);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.amountVnd), 0) FROM WithdrawalRequest w " +
            "WHERE w.driverUserId = :driverUserId " +
            "AND w.status IN ('PENDING', 'APPROVED_PAID') " +
            "AND w.createdAt >= :startOfDay")
    Long calculateDailyWithdrawnAmount(@Param("driverUserId") UUID driverUserId, @Param("startOfDay") Instant startOfDay);
}


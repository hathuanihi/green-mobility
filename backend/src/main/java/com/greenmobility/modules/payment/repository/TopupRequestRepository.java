package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.TopupRequest;
import jakarta.persistence.LockModeType;
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
public interface TopupRequestRepository extends JpaRepository<TopupRequest, UUID> {

    Optional<TopupRequest> findByGatewayTxnRef(String gatewayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TopupRequest t WHERE t.id = :id")
    Optional<TopupRequest> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TopupRequest t WHERE t.gatewayTxnRef = :gatewayTxnRef")
    Optional<TopupRequest> findByGatewayTxnRefWithLock(@Param("gatewayTxnRef") String gatewayTxnRef);

    long countByUserIdAndStatus(UUID userId, String status);

    List<TopupRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT t FROM TopupRequest t WHERE t.status = 'PENDING' AND t.expiresAt < :now")
    List<TopupRequest> findExpiredPendingTopups(@Param("now") Instant now);
}


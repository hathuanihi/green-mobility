package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByTripId(UUID tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.tripId = :tripId AND p.status IN ('INITIATED', 'PENDING', 'PAID', 'PARTIALLY_REFUNDED')")
    Optional<Payment> findActivePaymentByTripIdWithLock(@Param("tripId") UUID tripId);

    Optional<Payment> findByGatewayAndGatewayTxnRef(String gateway, String gatewayTxnRef);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.method IN ('VNPAY', 'MOMO') AND p.createdAt < :cutoff")
    List<Payment> findPendingOnlinePaymentsOlderThan(@Param("cutoff") Instant cutoff);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.method = 'CASH' AND p.createdAt < :cutoff")
    List<Payment> findPendingCashPaymentsOlderThan(@Param("cutoff") Instant cutoff);

    @Query("SELECT p FROM Payment p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:method IS NULL OR p.method = :method) AND " +
            "(:fromTime IS NULL OR p.createdAt >= :fromTime) AND " +
            "(:toTime IS NULL OR p.createdAt <= :toTime)")
    Page<Payment> searchPaymentsForAdmin(@Param("status") String status,
                                        @Param("method") String method,
                                        @Param("fromTime") Instant fromTime,
                                        @Param("toTime") Instant toTime,
                                        Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM Payment p WHERE p.status = 'PAID'")
    Long calculateTotalGmv();

    @Query("SELECT COALESCE(SUM(p.commissionVnd), 0) FROM Payment p WHERE p.status = 'PAID'")
    Long calculateTotalCommission();

    long countByStatus(String status);
}


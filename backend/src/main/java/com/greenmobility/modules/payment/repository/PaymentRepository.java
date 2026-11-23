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

    Optional<Payment> findFirstByTripIdOrderByCreatedAtDesc(UUID tripId);

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

    @Query("SELECT p FROM Payment p WHERE p.driverUserId = :driverUserId AND p.status = 'PAID' " +
            "AND p.paidAt >= :fromTime AND p.paidAt <= :toTime ORDER BY p.paidAt DESC")
    List<Payment> findDriverPaidPaymentsInWindow(@Param("driverUserId") UUID driverUserId,
                                                 @Param("fromTime") Instant fromTime,
                                                 @Param("toTime") Instant toTime);

    @Query(value = "SELECT * FROM payments p WHERE p.driver_user_id = :driverUserId AND p.status = 'PAID' " +
            "AND (CAST(:cursorTime AS TIMESTAMPTZ) IS NULL OR (p.paid_at, p.id) < (CAST(:cursorTime AS TIMESTAMPTZ), CAST(:cursorId AS UUID))) " +
            "ORDER BY p.paid_at DESC NULLS LAST, p.id DESC LIMIT :limit", nativeQuery = true)
    List<Payment> findDriverPaidPaymentsCursor(@Param("driverUserId") UUID driverUserId,
                                               @Param("cursorTime") Instant cursorTime,
                                               @Param("cursorId") UUID cursorId,
                                               @Param("limit") int limit);

    @Query(value = "SELECT p.trip_id FROM payments p " +
            "JOIN trips t ON t.id = p.trip_id " +
            "WHERE p.customer_id = :customerId " +
            "AND t.status = 'COMPLETED' " +
            "AND t.payment_status IN ('FAILED', 'PENDING', 'PROCESSING') " +
            "AND p.status IN ('FAILED', 'PENDING', 'INITIATED') " +
            "AND (p.method <> 'CASH' OR p.status = 'FAILED') " +
            "AND p.created_at < :cutoff " +
            "AND NOT EXISTS (SELECT 1 FROM payments p2 WHERE p2.trip_id = p.trip_id AND p2.status = 'PAID') " +
            "ORDER BY p.created_at ASC LIMIT 1", nativeQuery = true)
    Optional<UUID> findFirstOutstandingTripId(@Param("customerId") UUID customerId, @Param("cutoff") Instant cutoff);

    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM Payment p WHERE p.status IN ('PAID', 'PARTIALLY_REFUNDED')")
    Long calculateTotalGmv();

    @Query("SELECT COALESCE(SUM(p.commissionVnd), 0) FROM Payment p WHERE p.status IN ('PAID', 'PARTIALLY_REFUNDED')")
    Long calculateTotalCommission();

    @Query("SELECT p.method, COUNT(p), COALESCE(SUM(p.amountVnd), 0) FROM Payment p GROUP BY p.method")
    List<Object[]> aggregateByMethod();

    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    List<Object[]> aggregateCountByStatus();

    @Query(value = "SELECT TO_CHAR(p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') AS day_key, " +
            "COALESCE(SUM(p.amount_vnd), 0) AS gmv_vnd, " +
            "COALESCE(SUM(p.commission_vnd), 0) AS commission_vnd, " +
            "COALESCE(SUM(p.driver_amount_vnd), 0) AS driver_vnd " +
            "FROM payments p " +
            "WHERE p.status IN ('PAID', 'PARTIALLY_REFUNDED') " +
            "AND p.paid_at >= :fromTime " +
            "GROUP BY day_key ORDER BY day_key ASC", nativeQuery = true)
    List<Object[]> aggregateDailyRevenueSince(@Param("fromTime") Instant fromTime);

    long countByStatus(String status);
}

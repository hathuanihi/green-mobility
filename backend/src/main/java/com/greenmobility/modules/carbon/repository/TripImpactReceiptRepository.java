package com.greenmobility.modules.carbon.repository;

import com.greenmobility.modules.carbon.entity.TripImpactReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripImpactReceiptRepository extends JpaRepository<TripImpactReceipt, UUID> {

    Optional<TripImpactReceipt> findByTripId(UUID tripId);

    Optional<TripImpactReceipt> findByShareableSlug(String shareableSlug);

    boolean existsByTripId(UUID tripId);

    @Query("SELECT COALESCE(SUM(r.co2SavedGrams), 0) FROM TripImpactReceipt r " +
           "JOIN com.greenmobility.modules.trip.entity.Trip t ON t.id = r.tripId " +
           "WHERE t.customerId = :customerId")
    BigDecimal sumCo2SavedByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT COUNT(r) FROM TripImpactReceipt r " +
           "JOIN com.greenmobility.modules.trip.entity.Trip t ON t.id = r.tripId " +
           "WHERE t.customerId = :customerId")
    Long countTripsByCustomerId(@Param("customerId") UUID customerId);
}

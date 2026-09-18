package com.greenmobility.modules.trip.repository;

import com.greenmobility.modules.trip.entity.Trip;
import com.greenmobility.modules.trip.entity.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    Optional<Trip> findByTripCode(String tripCode);

    Optional<Trip> findFirstByCustomerIdAndStatusInOrderByRequestedAtDesc(UUID customerId, Collection<TripStatus> statuses);

    Optional<Trip> findFirstByDriverIdAndStatusInOrderByRequestedAtDesc(UUID driverId, Collection<TripStatus> statuses);

    List<Trip> findByCustomerIdOrderByRequestedAtDesc(UUID customerId);

    List<Trip> findByDriverIdOrderByRequestedAtDesc(UUID driverId);

    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    List<Trip> findByStatusInOrderByRequestedAtDesc(Collection<TripStatus> statuses);

    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.customerId = :customerId AND t.status IN :statuses")
    boolean existsActiveTripForCustomer(@Param("customerId") UUID customerId, @Param("statuses") Collection<TripStatus> statuses);
}

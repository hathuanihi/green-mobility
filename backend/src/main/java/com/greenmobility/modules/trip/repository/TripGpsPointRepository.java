package com.greenmobility.modules.trip.repository;

import com.greenmobility.modules.trip.entity.TripGpsPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB repository for GPS telemetry points collected during trip execution.
 */
@Repository
public interface TripGpsPointRepository extends MongoRepository<TripGpsPoint, String> {

    /**
     * Find all GPS points for a given trip and phase, ordered by timestamp ascending.
     * Used for actual distance calculation at trip completion.
     */
    List<TripGpsPoint> findByTripIdAndPhaseOrderByTimestampAsc(String tripId, String phase);

    /**
     * Find all GPS points for a given trip, ordered by timestamp ascending.
     */
    List<TripGpsPoint> findByTripIdOrderByTimestampAsc(String tripId);

    /**
     * Find the latest GPS point for a given trip.
     */
    TripGpsPoint findFirstByTripIdOrderByTimestampDesc(String tripId);

    /**
     * Find the latest GPS point for a driver.
     */
    TripGpsPoint findFirstByDriverIdOrderByTimestampDesc(String driverId);

    /**
     * Check if a GPS point already exists at this exact timestamp for duplicate filtering (offline sync).
     */
    boolean existsByTripIdAndTimestamp(String tripId, Instant timestamp);

    /**
     * Count total GPS points for a trip in a given phase.
     */
    long countByTripIdAndPhase(String tripId, String phase);
}

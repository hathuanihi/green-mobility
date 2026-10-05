package com.greenmobility.modules.trip.service;

import com.greenmobility.modules.trip.entity.TripGpsPoint;
import com.greenmobility.modules.trip.repository.TripGpsPointRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing GPS telemetry data in MongoDB.
 * Handles saving GPS points during trip execution, querying them for distance calculation,
 * and initializing MongoDB indexes on startup.
 */
@Service
public class GpsTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(GpsTelemetryService.class);

    private final TripGpsPointRepository gpsPointRepository;

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    public GpsTelemetryService(TripGpsPointRepository gpsPointRepository) {
        this.gpsPointRepository = gpsPointRepository;
    }

    /**
     * Ensure MongoDB indexes are created when the application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initIndexes() {
        if (mongoTemplate != null) {
            try {
                mongoTemplate.indexOps(TripGpsPoint.class).ensureIndex(
                        new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE)
                );
                mongoTemplate.indexOps(TripGpsPoint.class).ensureIndex(
                        new CompoundIndexDefinition(new Document("tripId", 1).append("timestamp", 1))
                                .named("idx_trip_timeline")
                );
                mongoTemplate.indexOps(TripGpsPoint.class).ensureIndex(
                        new CompoundIndexDefinition(new Document("driverId", 1).append("timestamp", -1))
                                .named("idx_driver_latest")
                );
                log.info("MongoDB indexes for TripGpsPoint initialized successfully");
            } catch (Exception e) {
                log.warn("Could not initialize MongoDB indexes automatically: {}", e.getMessage());
            }
        }
    }

    /**
     * Save a single GPS point to MongoDB.
     */
    public TripGpsPoint saveGpsPoint(String tripId, String driverId, String vehicleType,
                                     String phase, double lat, double lng,
                                     Double speedKmh, Double bearing, Double altitude,
                                     Double accuracy, Integer batteryPercent,
                                     Boolean isMockLocation, Instant timestamp) {
        TripGpsPoint point = new TripGpsPoint(
                tripId, driverId, vehicleType, phase,
                lng, lat, speedKmh, bearing, altitude, accuracy,
                batteryPercent, isMockLocation,
                timestamp != null ? timestamp : Instant.now()
        );
        TripGpsPoint saved = gpsPointRepository.save(point);
        log.debug("GPS point saved: trip={}, phase={}, lat={}, lng={}", tripId, phase, lat, lng);
        return saved;
    }

    /**
     * Batch save GPS points (for offline sync).
     * Filters out duplicates by checking existing timestamps.
     *
     * @return number of points actually saved (excluding duplicates)
     */
    public int saveGpsPointsBatch(String tripId, String driverId, String vehicleType,
                                  String phase, List<GpsPointData> points) {
        int saved = 0;
        for (GpsPointData p : points) {
            Instant ts = p.timestamp() != null ? p.timestamp() : Instant.now();
            if (!gpsPointRepository.existsByTripIdAndTimestamp(tripId, ts)) {
                saveGpsPoint(tripId, driverId, vehicleType, phase,
                        p.lat(), p.lng(), p.speedKmh(), p.bearing(),
                        p.altitude(), p.accuracy(), p.batteryPercent(),
                        p.isMockLocation(), ts);
                saved++;
            }
        }
        log.info("GPS batch sync: trip={}, total={}, saved={}, duplicates={}",
                tripId, points.size(), saved, points.size() - saved);
        return saved;
    }

    /**
     * Get all GPS points for a trip in a specific phase, ordered chronologically.
     */
    public List<TripGpsPoint> getPointsByTripAndPhase(String tripId, String phase) {
        return gpsPointRepository.findByTripIdAndPhaseOrderByTimestampAsc(tripId, phase);
    }

    /**
     * Get all GPS points for a trip, ordered chronologically.
     */
    public List<TripGpsPoint> getAllPointsByTrip(String tripId) {
        return gpsPointRepository.findByTripIdOrderByTimestampAsc(tripId);
    }

    /**
     * Get the latest GPS point for a trip.
     */
    public TripGpsPoint getLatestPoint(String tripId) {
        return gpsPointRepository.findFirstByTripIdOrderByTimestampDesc(tripId);
    }

    /**
     * Count GPS points for a trip phase.
     */
    public long countPoints(String tripId, String phase) {
        return gpsPointRepository.countByTripIdAndPhase(tripId, phase);
    }

    /**
     * Lightweight record for batch GPS point data.
     */
    public record GpsPointData(
            double lat, double lng, Double speedKmh, Double bearing,
            Double altitude, Double accuracy, Integer batteryPercent,
            Boolean isMockLocation, Instant timestamp
    ) {}
}

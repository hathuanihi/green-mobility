package com.greenmobility.modules.matching.repository;

import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

@Repository
public class DriverGeoRedisRepository {

    private static final Logger log = LoggerFactory.getLogger(DriverGeoRedisRepository.class);

    private static final String GEO_PREFIX = "drivers:geo:available:";
    private static final String META_PREFIX = "driver:meta:";
    private static final String COOLDOWN_PREFIX = "driver:cooldown:";
    private static final String PENDING_DISPATCH_PREFIX = "driver:dispatch:pending:";

    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public DriverGeoRedisRepository(StringRedisTemplate redisTemplate, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public record DriverCandidateLocation(UUID driverId, double distanceKm, double lat, double lng) {}

    public void updateLocation(UUID driverId, VehicleType vehicleType, double lat, double lng, int batteryPercent) {
        String geoKey = GEO_PREFIX + vehicleType.name();
        String driverIdStr = driverId.toString();

        // Add to Redis GEO (Longitude = X, Latitude = Y)
        redisTemplate.opsForGeo().add(geoKey, new Point(lng, lat), driverIdStr);

        // Update driver metadata Hash
        String metaKey = META_PREFIX + driverIdStr;
        Map<String, String> meta = new HashMap<>();
        meta.put("battery", String.valueOf(batteryPercent));
        meta.put("lat", String.valueOf(lat));
        meta.put("lng", String.valueOf(lng));
        meta.put("updatedAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(metaKey, meta);
        redisTemplate.expire(metaKey, Duration.ofHours(24));
    }

    public void removeLocation(UUID driverId, VehicleType vehicleType) {
        String geoKey = GEO_PREFIX + vehicleType.name();
        redisTemplate.opsForZSet().remove(geoKey, driverId.toString());
    }

    public List<DriverCandidateLocation> searchNearby(VehicleType vehicleType, double pickupLat, double pickupLng, double radiusKm) {
        String geoKey = GEO_PREFIX + vehicleType.name();

        try {
            RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                    .includeCoordinates()
                    .includeDistance()
                    .sortAscending()
                    .limit(20);

            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                    geoKey,
                    GeoReference.fromCoordinate(new Point(pickupLng, pickupLat)),
                    new Distance(radiusKm, Metrics.KILOMETERS),
                    args
            );

            if (results == null || results.getContent().isEmpty()) {
                return Collections.emptyList();
            }

            List<DriverCandidateLocation> candidates = new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : results) {
                try {
                    UUID driverId = UUID.fromString(item.getContent().getName());
                    double distanceKm = item.getDistance().getValue();
                    Point point = item.getContent().getPoint();
                    double lng = point != null ? point.getX() : pickupLng;
                    double lat = point != null ? point.getY() : pickupLat;
                    candidates.add(new DriverCandidateLocation(driverId, distanceKm, lat, lng));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID in Redis GEO set: {}", item.getContent().getName());
                }
            }
            return candidates;
        } catch (Exception e) {
            log.error("Error performing GEOSEARCH for {}: {}", geoKey, e.getMessage());
            return Collections.emptyList();
        }
    }

    public int getDriverBattery(UUID driverId) {
        String metaKey = META_PREFIX + driverId.toString();
        Object val = redisTemplate.opsForHash().get(metaKey, "battery");
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return 100; // Mặc định 100% nếu chưa có dữ liệu ping
    }

    public boolean isDriverInCooldown(UUID driverId, UUID tripId) {
        String key = COOLDOWN_PREFIX + driverId.toString() + ":" + tripId.toString();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setDriverCooldown(UUID driverId, UUID tripId, long ttlSeconds) {
        String key = COOLDOWN_PREFIX + driverId.toString() + ":" + tripId.toString();
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    public void savePendingDispatch(UUID driverId, com.greenmobility.modules.trip.dto.DispatchNotificationDto dto, long ttlSeconds) {
        String key = PENDING_DISPATCH_PREFIX + driverId.toString();
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("Error saving pending dispatch to Redis for driver {}: {}", driverId, e.getMessage());
        }
    }

    public com.greenmobility.modules.trip.dto.DispatchNotificationDto getPendingDispatch(UUID driverId) {
        String key = PENDING_DISPATCH_PREFIX + driverId.toString();
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, com.greenmobility.modules.trip.dto.DispatchNotificationDto.class);
            } catch (Exception e) {
                log.error("Error reading pending dispatch from Redis for driver {}: {}", driverId, e.getMessage());
            }
        }
        return null;
    }

    public void clearPendingDispatch(UUID driverId) {
        String key = PENDING_DISPATCH_PREFIX + driverId.toString();
        redisTemplate.delete(key);
    }
}

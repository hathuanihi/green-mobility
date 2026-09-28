package com.greenmobility.modules.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.util.PolylineUtil;
import com.greenmobility.modules.map.dto.GeocodeResultDto;
import com.greenmobility.modules.map.dto.MapConfigDto;
import com.greenmobility.modules.map.dto.PlacePredictionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoongMapService {

    private static final Logger log = LoggerFactory.getLogger(GoongMapService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double URBAN_ROAD_FACTOR = 1.25;

    private final String apiKey;
    private final String maptilesKey;
    private final String baseUrl;
    private final String tilesUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public record RouteResult(int distanceMeters, int durationSeconds, String polyline, boolean isFromGoong) {}

    public GoongMapService() {
        this(System.getenv().getOrDefault("GOONG_API_KEY", ""),
                System.getenv().getOrDefault("GOONG_MAPTILES_KEY", ""),
                System.getenv().getOrDefault("GOONG_BASE_URL", "https://rsapi.goong.io"),
                System.getenv().getOrDefault("GOONG_TILES_URL", "https://tiles.goong.io"),
                new RestTemplateBuilder(), new ObjectMapper());
    }

    public GoongMapService(
            @Value("${green-mobility.map.goong.api-key:}") String apiKey,
            @Value("${green-mobility.map.goong.maptiles-key:}") String maptilesKey,
            @Value("${green-mobility.map.goong.base-url:https://rsapi.goong.io}") String baseUrl,
            @Value("${green-mobility.map.goong.tiles-url:https://tiles.goong.io}") String tilesUrl,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.maptilesKey = maptilesKey != null ? maptilesKey.trim() : "";
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.tilesUrl = tilesUrl.replaceAll("/$", "");
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Lấy cấu hình bản đồ Goong cho Frontend & Mobile
     */
    public MapConfigDto getMapConfig() {
        String key = !maptilesKey.isEmpty() ? maptilesKey : apiKey;
        return MapConfigDto.builder()
                .provider("GOONG")
                .maptilesKey(key)
                .webStyleUrl(String.format("%s/assets/goong_map_web.json?api_key=%s", tilesUrl, key))
                .darkStyleUrl(String.format("%s/assets/goong_map_dark.json?api_key=%s", tilesUrl, key))
                .navNightStyleUrl(String.format("%s/assets/navigation_night.json?api_key=%s", tilesUrl, key))
                .navDayStyleUrl(String.format("%s/assets/navigation_day.json?api_key=%s", tilesUrl, key))
                .defaultLat(10.776530)
                .defaultLng(106.700981)
                .defaultZoom(15.0)
                .build();
    }

    /**
     * Tính toán lộ trình định tuyến (Direction) bằng Goong API kèm Fallback
     */
    public RouteResult calculateRoute(double startLat, double startLng, double endLat, double endLng, String vehicleType) {
        String goongVehicle = mapVehicleTypeToGoong(vehicleType);

        if (!apiKey.isEmpty()) {
            try {
                String url = String.format(
                        "%s/Direction?origin=%f,%f&destination=%f,%f&vehicle=%s&api_key=%s",
                        baseUrl, startLat, startLng, endLat, endLng, goongVehicle, apiKey
                );

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode routes = root.path("routes");
                    if (routes.isArray() && !routes.isEmpty()) {
                        JsonNode firstRoute = routes.get(0);
                        JsonNode legs = firstRoute.path("legs");
                        if (legs.isArray() && !legs.isEmpty()) {
                            JsonNode firstLeg = legs.get(0);
                            int distanceMeters = firstLeg.path("distance").path("value").asInt();
                            int durationSeconds = firstLeg.path("duration").path("value").asInt();
                            String polyline = firstRoute.path("overview_polyline").path("points").asText();

                            log.info("Goong Direction API thành công: {}m, {}s", distanceMeters, durationSeconds);
                            return new RouteResult(distanceMeters, durationSeconds, polyline, true);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể gọi Goong Direction API ({}), chuyển sang thuật toán Fallback: {}", e.getMessage(), e.getClass().getSimpleName());
            }
        }

        // Fallback: Haversine distance * urban factor + generated smooth urban polyline
        return calculateFallbackRoute(startLat, startLng, endLat, endLng, goongVehicle);
    }

    /**
     * Tự động gợi ý địa điểm (Places Autocomplete)
     */
    public List<PlacePredictionDto> autocompletePlaces(String input, Double lat, Double lng) {
        if (input == null || input.trim().isEmpty()) {
            return getPresetLocations();
        }

        if (!apiKey.isEmpty()) {
            try {
                String encodedInput = URLEncoder.encode(input.trim(), StandardCharsets.UTF_8);
                String locationParam = (lat != null && lng != null) ? String.format("&location=%f,%f", lat, lng) : "";
                String url = String.format("%s/Place/AutoComplete?input=%s%s&api_key=%s", baseUrl, encodedInput, locationParam, apiKey);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode predictions = root.path("predictions");
                    if (predictions.isArray() && !predictions.isEmpty()) {
                        List<PlacePredictionDto> results = new ArrayList<>();
                        for (JsonNode item : predictions) {
                            results.add(PlacePredictionDto.builder()
                                    .placeId(item.path("place_id").asText())
                                    .description(item.path("description").asText())
                                    .mainText(item.path("structured_formatting").path("main_text").asText())
                                    .secondaryText(item.path("structured_formatting").path("secondary_text").asText())
                                    .build());
                        }
                        return results;
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi Goong Place AutoComplete ({}), nạp kết quả danh mục mặc định", e.getMessage());
            }
        }

        // Fallback: Lọc từ danh bạ địa điểm tiêu biểu TP.HCM
        return filterPresetLocations(input);
    }

    /**
     * Tra cứu tọa độ địa lý từ địa chỉ (Geocode)
     */
    public GeocodeResultDto geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        if (!apiKey.isEmpty()) {
            try {
                String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
                String url = String.format("%s/geocode?address=%s&api_key=%s", baseUrl, encoded, apiKey);
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode results = root.path("results");
                    if (results.isArray() && !results.isEmpty()) {
                        JsonNode first = results.get(0);
                        JsonNode location = first.path("geometry").path("location");
                        return GeocodeResultDto.builder()
                                .formattedAddress(first.path("formatted_address").asText())
                                .lat(location.path("lat").asDouble())
                                .lng(location.path("lng").asDouble())
                                .placeId(first.path("place_id").asText())
                                .build();
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi Goong Geocode API ({})", e.getMessage());
            }
        }

        // Fallback matched preset
        for (PlacePredictionDto preset : getPresetLocations()) {
            if (preset.getDescription().toLowerCase().contains(address.toLowerCase())
                    || address.toLowerCase().contains(preset.getMainText().toLowerCase())) {
                return GeocodeResultDto.builder()
                        .formattedAddress(preset.getDescription())
                        .lat(preset.getLat())
                        .lng(preset.getLng())
                        .placeId(preset.getPlaceId())
                        .build();
            }
        }

        return GeocodeResultDto.builder()
                .formattedAddress(address)
                .lat(10.776530)
                .lng(106.700981)
                .placeId("default_sg")
                .build();
    }

    /**
     * Tra cứu địa chỉ từ tọa độ (Reverse Geocode)
     */
    public GeocodeResultDto reverseGeocode(double lat, double lng) {
        if (!apiKey.isEmpty()) {
            try {
                String url = String.format("%s/geocode?latlng=%f,%f&api_key=%s", baseUrl, lat, lng, apiKey);
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode results = root.path("results");
                    if (results.isArray() && !results.isEmpty()) {
                        JsonNode first = results.get(0);
                        return GeocodeResultDto.builder()
                                .formattedAddress(first.path("formatted_address").asText())
                                .lat(lat)
                                .lng(lng)
                                .placeId(first.path("place_id").asText())
                                .build();
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi Goong Reverse Geocode API ({})", e.getMessage());
            }
        }

        return GeocodeResultDto.builder()
                .formattedAddress(String.format("Vị trí [%.5f, %.5f], TP. Hồ Chí Minh", lat, lng))
                .lat(lat)
                .lng(lng)
                .placeId("rev_coord")
                .build();
    }

    private RouteResult calculateFallbackRoute(double startLat, double startLng, double endLat, double endLng, String vehicle) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLng = Math.toRadians(endLng - startLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightDistance = EARTH_RADIUS_METERS * c;

        int distanceMeters = (int) Math.round(straightDistance * URBAN_ROAD_FACTOR);
        if (distanceMeters < 500) {
            distanceMeters = 500;
        }

        // Tốc độ đô thị: bike ~ 25 km/h (6.94 m/s), car ~ 30 km/h (8.33 m/s)
        double speedMps = "bike".equalsIgnoreCase(vehicle) ? 6.94 : 8.33;
        int durationSeconds = (int) Math.round(distanceMeters / speedMps) + 60;

        String polyline = PolylineUtil.generateUrbanPolyline(startLat, startLng, endLat, endLng);
        return new RouteResult(distanceMeters, durationSeconds, polyline, false);
    }

    private String mapVehicleTypeToGoong(String vehicleType) {
        if (vehicleType == null) return "bike";
        return switch (vehicleType.toUpperCase()) {
            case "ELECTRIC_CAR_4SEAT", "ELECTRIC_CAR_7SEAT" -> "car";
            default -> "bike";
        };
    }

    private List<PlacePredictionDto> getPresetLocations() {
        return List.of(
                PlacePredictionDto.builder()
                        .placeId("preset_ben_thanh")
                        .mainText("Chợ Bến Thành")
                        .secondaryText("Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM")
                        .description("Chợ Bến Thành, Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh")
                        .lat(10.7725).lng(106.6980)
                        .build(),
                PlacePredictionDto.builder()
                        .placeId("preset_landmark81")
                        .mainText("Landmark 81")
                        .secondaryText("720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM")
                        .description("Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP. Hồ Chí Minh")
                        .lat(10.7951).lng(106.7218)
                        .build(),
                PlacePredictionDto.builder()
                        .placeId("preset_nha_hat_tp")
                        .mainText("Nhà hát Thành phố")
                        .secondaryText("07 Công Trường Lam Sơn, Bến Nghé, Quận 1, TP.HCM")
                        .description("Nhà hát Thành phố, 07 Công Trường Lam Sơn, Bến Nghé, Quận 1, TP.HCM")
                        .lat(10.776530).lng(106.700981)
                        .build(),
                PlacePredictionDto.builder()
                        .placeId("preset_tan_son_nhat")
                        .mainText("Sân bay Quốc tế Tân Sơn Nhất")
                        .secondaryText("Trường Sơn, Phường 2, Tân Bình, TP.HCM")
                        .description("Sân bay Quốc tế Tân Sơn Nhất (Ga T1), Trường Sơn, Tân Bình, TP. Hồ Chí Minh")
                        .lat(10.8184).lng(106.6588)
                        .build(),
                PlacePredictionDto.builder()
                        .placeId("preset_dh_cntt")
                        .mainText("Trường ĐH Công nghệ Thông tin (UIT)")
                        .secondaryText("Khu phố 6, Linh Trung, TP. Thủ Đức, TP.HCM")
                        .description("Trường ĐH Công nghệ Thông tin, TP. Thủ Đức, TP. Hồ Chí Minh")
                        .lat(10.870020).lng(106.803054)
                        .build()
        );
    }

    private List<PlacePredictionDto> filterPresetLocations(String query) {
        String lower = query.toLowerCase();
        List<PlacePredictionDto> filtered = new ArrayList<>();
        for (PlacePredictionDto loc : getPresetLocations()) {
            if (loc.getMainText().toLowerCase().contains(lower) || loc.getDescription().toLowerCase().contains(lower)) {
                filtered.add(loc);
            }
        }
        if (filtered.isEmpty()) {
            filtered.add(PlacePredictionDto.builder()
                    .placeId("custom_search")
                    .mainText(query)
                    .secondaryText("Khu vực TP. Hồ Chí Minh")
                    .description(query + ", TP. Hồ Chí Minh")
                    .lat(10.776530).lng(106.700981)
                    .build());
        }
        return filtered;
    }
}

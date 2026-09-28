package com.greenmobility.modules.map.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.map.dto.GeocodeResultDto;
import com.greenmobility.modules.map.dto.MapConfigDto;
import com.greenmobility.modules.map.dto.PlacePredictionDto;
import com.greenmobility.modules.map.service.GoongMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/map")
@Tag(name = "Map & Routing API", description = "Các endpoint tích hợp Goong Map: Cấu hình Tiles, Định tuyến Direction, Tìm kiếm Địa điểm & Geocoding")
public class MapController {

    private final GoongMapService goongMapService;

    public MapController(GoongMapService goongMapService) {
        this.goongMapService = goongMapService;
    }

    @GetMapping("/config")
    @Operation(summary = "Lấy cấu hình bản đồ Goong (MapTiles key, styles sáng/tối cho Web & Mobile SDK)")
    public ResponseEntity<ApiResponse<MapConfigDto>> getMapConfig() {
        MapConfigDto config = goongMapService.getMapConfig();
        return ResponseEntity.ok(ApiResponse.ok("Lấy cấu hình bản đồ Goong thành công", config));
    }

    @GetMapping("/direction")
    @Operation(summary = "Tính toán lộ trình đường đi thực tế qua Goong Direction API")
    public ResponseEntity<ApiResponse<GoongMapService.RouteResult>> getDirection(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng,
            @RequestParam(defaultValue = "ELECTRIC_MOTORBIKE") String vehicleType) {
        GoongMapService.RouteResult result = goongMapService.calculateRoute(originLat, originLng, destLat, destLng, vehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Tính toán lộ trình thành công", result));
    }

    @GetMapping("/places/autocomplete")
    @Operation(summary = "Tự động gợi ý địa chỉ tìm kiếm (Places Autocomplete)")
    public ResponseEntity<ApiResponse<List<PlacePredictionDto>>> autocomplete(
            @RequestParam String input,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        List<PlacePredictionDto> predictions = goongMapService.autocompletePlaces(input, lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(predictions));
    }

    @GetMapping("/geocode")
    @Operation(summary = "Chuyển đổi địa chỉ văn bản thành tọa độ (Geocoding)")
    public ResponseEntity<ApiResponse<GeocodeResultDto>> geocode(@RequestParam String address) {
        GeocodeResultDto result = goongMapService.geocode(address);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/reverse-geocode")
    @Operation(summary = "Tra cứu địa chỉ văn bản từ tọa độ GPS (Reverse Geocoding)")
    public ResponseEntity<ApiResponse<GeocodeResultDto>> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {
        GeocodeResultDto result = goongMapService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}

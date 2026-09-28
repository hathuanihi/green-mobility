package com.greenmobility.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.util.PolylineUtil;
import com.greenmobility.modules.map.dto.MapConfigDto;
import com.greenmobility.modules.map.dto.PlacePredictionDto;
import com.greenmobility.modules.map.service.GoongMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoongMapServiceTest {

    private GoongMapService goongMapService;

    @BeforeEach
    void setUp() {
        goongMapService = new GoongMapService(
                "kfP52IpuOKUJhBXQWCoDzz635fdaS2j3FIRGqaTi",
                "kfP52IpuOKUJhBXQWCoDzz635fdaS2j3FIRGqaTi",
                "https://rsapi.goong.io",
                "https://tiles.goong.io",
                new RestTemplateBuilder(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Lấy cấu hình Goong Map Tiles thành công với style Dark và Web")
    void testGetMapConfig() {
        MapConfigDto config = goongMapService.getMapConfig();
        assertNotNull(config);
        assertEquals("GOONG", config.getProvider());
        assertEquals("kfP52IpuOKUJhBXQWCoDzz635fdaS2j3FIRGqaTi", config.getMaptilesKey());
        assertTrue(config.getDarkStyleUrl().contains("goong_map_dark.json"));
        assertTrue(config.getWebStyleUrl().contains("goong_map_web.json"));
        assertEquals(10.776530, config.getDefaultLat());
        assertEquals(106.700981, config.getDefaultLng());
    }

    @Test
    @DisplayName("Tính toán lộ trình xe máy điện trả về khoảng cách, thời gian và encoded polyline")
    void testCalculateRouteMotorbike() {
        // Nhà hát TP (10.77653, 106.700981) -> ĐH CNTT (10.87002, 106.803054)
        GoongMapService.RouteResult route = goongMapService.calculateRoute(
                10.776530, 106.700981,
                10.870020, 106.803054,
                "ELECTRIC_MOTORBIKE"
        );

        assertNotNull(route);
        assertTrue(route.distanceMeters() > 10000);
        assertTrue(route.durationSeconds() > 1000);
        assertNotNull(route.polyline());
        assertFalse(route.polyline().isEmpty());

        // Kiểm tra Polyline có thể giải mã thành các điểm tọa độ hợp lệ
        List<PolylineUtil.LatLngPoint> decoded = PolylineUtil.decode(route.polyline());
        assertTrue(decoded.size() >= 2);
    }

    @Test
    @DisplayName("Autocomplete địa điểm tìm kiếm tại TP.HCM")
    void testAutocompletePlaces() {
        List<PlacePredictionDto> results = goongMapService.autocompletePlaces("Landmark", 10.77653, 106.70098);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getDescription().contains("Landmark")));
    }
}

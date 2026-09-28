package com.greenmobility.modules.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin cấu hình bản đồ Goong cho Frontend & Mobile")
public class MapConfigDto {

    @Schema(description = "Tên nhà cung cấp bản đồ", example = "GOONG")
    private String provider;

    @Schema(description = "MapTiles Key dùng để render Vector/Raster Tiles trên SDK", example = "kfP52IpuOKUJhBXQWCoDzz635fdaS2j3FIRGqaTi")
    private String maptilesKey;

    @Schema(description = "Style URL giao diện sáng (Web)", example = "https://tiles.goong.io/assets/goong_map_web.json?api_key=...")
    private String webStyleUrl;

    @Schema(description = "Style URL giao diện tối (Dark Mode - Khuyên dùng cho Green Mobility)", example = "https://tiles.goong.io/assets/goong_map_dark.json?api_key=...")
    private String darkStyleUrl;

    @Schema(description = "Style URL điều hướng ban đêm", example = "https://tiles.goong.io/assets/navigation_night.json?api_key=...")
    private String navNightStyleUrl;

    @Schema(description = "Style URL điều hướng ban ngày", example = "https://tiles.goong.io/assets/navigation_day.json?api_key=...")
    private String navDayStyleUrl;

    @Schema(description = "Tọa độ vĩ độ mặc định (TP.HCM)", example = "10.776530")
    private Double defaultLat;

    @Schema(description = "Tọa độ kinh độ mặc định (TP.HCM)", example = "106.700981")
    private Double defaultLng;

    @Schema(description = "Mức độ thu phóng bản đồ mặc định", example = "15.0")
    private Double defaultZoom;
}

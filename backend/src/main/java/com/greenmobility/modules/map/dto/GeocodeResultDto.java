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
@Schema(description = "Kết quả định vị tọa độ địa lý (Geocoding / Reverse Geocoding)")
public class GeocodeResultDto {

    @Schema(description = "Địa chỉ chuẩn hóa", example = "Nhà hát Thành phố, 07 Công Trường Lam Sơn, Bến Nghé, Quận 1, TP. Hồ Chí Minh")
    private String formattedAddress;

    @Schema(description = "Vĩ độ (Latitude)", example = "10.776530")
    private Double lat;

    @Schema(description = "Kinh độ (Longitude)", example = "106.700981")
    private Double lng;

    @Schema(description = "Mã Goong place_id", example = "ChIJ...")
    private String placeId;
}

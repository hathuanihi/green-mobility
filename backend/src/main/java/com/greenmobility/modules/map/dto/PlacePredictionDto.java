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
@Schema(description = "Gợi ý địa điểm tìm kiếm từ Goong Places")
public class PlacePredictionDto {

    @Schema(description = "Mã định danh địa điểm (Goong place_id)", example = "ChIJs...")
    private String placeId;

    @Schema(description = "Mô tả đầy đủ địa chỉ", example = "Landmark 81, Vinhomes Central Park, Bình Thạnh, TP.HCM")
    private String description;

    @Schema(description = "Tên địa điểm chính", example = "Landmark 81")
    private String mainText;

    @Schema(description = "Địa chỉ phụ / Quận huyện", example = "Bình Thạnh, TP. Hồ Chí Minh")
    private String secondaryText;

    @Schema(description = "Vĩ độ (nếu có sẵn)")
    private Double lat;

    @Schema(description = "Kinh độ (nếu có sẵn)")
    private Double lng;
}

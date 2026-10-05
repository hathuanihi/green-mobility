package com.greenmobility.modules.carbon.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.carbon.dto.TripImpactReceiptDto;
import com.greenmobility.modules.carbon.dto.UserCarbonSummaryDto;
import com.greenmobility.modules.carbon.service.CarbonCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "4. Carbon Impact & Receipts", description = "Các API tra cứu Hóa đơn Tác động Xanh, Chứng nhận CO2 và Thống kê phát thải cá nhân")
@RestController
@RequestMapping("/carbon")
public class CarbonReceiptController {

    private final CarbonCalculationService carbonCalculationService;

    public CarbonReceiptController(CarbonCalculationService carbonCalculationService) {
        this.carbonCalculationService = carbonCalculationService;
    }

    @Operation(summary = "Xem Hóa đơn Tác động Xanh của chuyến đi", description = "Tra cứu lượng CO2 giảm, các chỉ số sinh thái tương đương (cây xanh, đèn LED, sạc pin) theo Trip ID")
    @GetMapping("/receipts/trip/{tripId}")
    public ResponseEntity<ApiResponse<TripImpactReceiptDto>> getReceiptByTripId(
            @PathVariable UUID tripId) {
        TripImpactReceiptDto dto = carbonCalculationService.getReceiptByTripId(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Tra cứu hóa đơn tác động xanh thành công", dto));
    }

    @Operation(summary = "Xem Chứng nhận Tác động Xanh công khai", description = "Truy cập chứng nhận xanh công khai qua shareable slug (không cần xác thực)")
    @GetMapping("/receipts/share/{slug}")
    public ResponseEntity<ApiResponse<TripImpactReceiptDto>> getReceiptByShareableSlug(
            @PathVariable String slug) {
        TripImpactReceiptDto dto = carbonCalculationService.getReceiptByShareableSlug(slug);
        return ResponseEntity.ok(ApiResponse.ok("Tra cứu chứng nhận xanh công khai thành công", dto));
    }

    @Operation(summary = "Thống kê giảm phát thải Carbon của người dùng hiện tại", description = "Lấy tổng lượng CO2 tiết kiệm, tổng số chuyến xanh, số ngày cây xanh và tín chỉ carbon cá nhân")
    @GetMapping("/user/summary")
    public ResponseEntity<ApiResponse<UserCarbonSummaryDto>> getUserCarbonSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserCarbonSummaryDto summary = carbonCalculationService.getUserCarbonSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Lấy thống kê carbon người dùng thành công", summary));
    }
}

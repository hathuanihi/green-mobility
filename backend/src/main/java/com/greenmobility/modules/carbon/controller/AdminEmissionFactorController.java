package com.greenmobility.modules.carbon.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.carbon.dto.*;
import com.greenmobility.modules.carbon.service.CarbonCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "4. Admin Emission Factor Management", description = "Quản lý Ma trận Hệ số Phát thải Carbon và Sandbox mô phỏng phát thải")
@RestController
@RequestMapping("/admin/emissions")
public class AdminEmissionFactorController {

    private final CarbonCalculationService carbonCalculationService;

    public AdminEmissionFactorController(CarbonCalculationService carbonCalculationService) {
        this.carbonCalculationService = carbonCalculationService;
    }

    @Operation(summary = "Lấy danh sách hệ số phát thải", description = "Lọc theo loại xe hoặc trạng thái kích hoạt")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmissionFactorDto>>> getAllEmissionFactors(
            @RequestParam(required = false) String vehicleCategory,
            @RequestParam(required = false) Boolean isActive) {
        List<EmissionFactorDto> list = carbonCalculationService.getAllEmissionFactors(vehicleCategory, isActive);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách hệ số phát thải thành công", list));
    }

    @Operation(summary = "Xem chi tiết hệ số phát thải", description = "Lấy thông số hệ số phát thải theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmissionFactorDto>> getEmissionFactorById(@PathVariable UUID id) {
        EmissionFactorDto dto = carbonCalculationService.getEmissionFactorById(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @Operation(summary = "Thêm mới hệ số phát thải", description = "Admin cấu hình hệ số phát thải mới (tự động tính calculated_ev_factor và net_co2_saving_per_km)")
    @PostMapping
    public ResponseEntity<ApiResponse<EmissionFactorDto>> createEmissionFactor(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateEmissionFactorRequest request) {
        UUID adminId = currentUser != null ? currentUser.getId() : null;
        EmissionFactorDto created = carbonCalculationService.createEmissionFactor(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo hệ số phát thải thành công", created));
    }

    @Operation(summary = "Cập nhật hệ số phát thải", description = "Admin chỉnh sửa các tham số hệ số phát thải")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmissionFactorDto>> updateEmissionFactor(
            @PathVariable UUID id,
            @RequestBody UpdateEmissionFactorRequest request) {
        EmissionFactorDto updated = carbonCalculationService.updateEmissionFactor(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hệ số phát thải thành công", updated));
    }

    @Operation(summary = "Bật/tắt trạng thái hệ số phát thải", description = "Kích hoạt hoặc vô hiệu hóa hệ số phát thải")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        carbonCalculationService.toggleEmissionFactorStatus(id, active);
        return ResponseEntity.ok(ApiResponse.ok("Đã thay đổi trạng thái hệ số phát thải thành công", null));
    }

    @Operation(summary = "Sandbox Mô phỏng Tính toán Phát thải Carbon", description = "Admin / Chuyên gia ESG mô phỏng tính toán nhanh lượng CO2 giảm theo khoảng cách và danh mục xe")
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<CarbonSimulationResponse>> simulate(
            @Valid @RequestBody CarbonSimulationRequest request) {
        CarbonSimulationResponse res = carbonCalculationService.simulateCarbonSavings(request);
        return ResponseEntity.ok(ApiResponse.ok("Mô phỏng tính toán phát thải thành công", res));
    }
}

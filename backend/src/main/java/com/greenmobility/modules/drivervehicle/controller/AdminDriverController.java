package com.greenmobility.modules.drivervehicle.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.drivervehicle.dto.AdminDriverResponse;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
import com.greenmobility.modules.drivervehicle.dto.KycRejectRequest;
import com.greenmobility.modules.drivervehicle.entity.FaceVerificationLog;
import com.greenmobility.modules.drivervehicle.service.AdminDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "3. Admin Driver Management", description = "Các API dành cho Quản trị viên: Duyệt hồ sơ KYC tài xế và kiểm tra nhật ký xác thực khuôn mặt")
@RestController
@RequestMapping("/admin/drivers")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
public class AdminDriverController {

    private final AdminDriverService adminDriverService;

    public AdminDriverController(AdminDriverService adminDriverService) {
        this.adminDriverService = adminDriverService;
    }

    @Operation(summary = "Lấy danh sách tài xế chờ duyệt KYC", description = "Danh sách tất cả hồ sơ tài xế đang ở trạng thái PENDING kèm thông tin xe điện")
    @GetMapping("/kyc/pending")
    public ResponseEntity<ApiResponse<List<AdminDriverResponse>>> getPendingDrivers() {
        List<AdminDriverResponse> pendingList = adminDriverService.getPendingDrivers();
        return ResponseEntity.ok(ApiResponse.ok(pendingList));
    }

    @Operation(summary = "Xem chi tiết hồ sơ tài xế", description = "Lấy toàn bộ thông tin giấy tờ, ảnh chụp CCCD, GPLX, Cà vẹt xe và ảnh selfie gốc")
    @GetMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getDriverDetail(@PathVariable UUID driverId) {
        DriverProfileResponse response = adminDriverService.getDriverDetail(driverId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Phê duyệt hồ sơ KYC tài xế", description = "Chuyển trạng thái hồ sơ sang APPROVED, cho phép tài xế bật ca làm việc")
    @PostMapping("/{driverId}/kyc/approve")
    public ResponseEntity<ApiResponse<Void>> approveKyc(@PathVariable UUID driverId) {
        adminDriverService.approveKyc(driverId);
        return ResponseEntity.ok(ApiResponse.ok("Hồ sơ tài xế và xe điện đã được phê duyệt thành công", null));
    }

    @Operation(summary = "Từ chối hồ sơ KYC tài xế", description = "Chuyển trạng thái hồ sơ sang REJECTED kèm lý do để tài xế cập nhật lại")
    @PostMapping("/{driverId}/kyc/reject")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @PathVariable UUID driverId,
            @Valid @RequestBody KycRejectRequest request) {
        adminDriverService.rejectKyc(driverId, request.getRejectionReason());
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối hồ sơ tài xế và gửi thông báo bổ sung", null));
    }

    @Operation(summary = "Xem lịch sử xác thực khuôn mặt", description = "Xem danh sách các lần so khớp khuôn mặt bật ca của tài xế kèm độ tương đồng Cosine")
    @GetMapping("/{driverId}/face-logs")
    public ResponseEntity<ApiResponse<List<FaceVerificationLog>>> getFaceLogs(@PathVariable UUID driverId) {
        List<FaceVerificationLog> logs = adminDriverService.getFaceLogs(driverId);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}

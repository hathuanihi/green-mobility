package com.greenmobility.modules.drivervehicle.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.drivervehicle.dto.DriverProfileResponse;
import com.greenmobility.modules.drivervehicle.dto.FaceVerifyResponse;
import com.greenmobility.modules.drivervehicle.dto.KycSubmissionRequest;
import com.greenmobility.modules.drivervehicle.dto.KycSubmitResponse;
import com.greenmobility.modules.drivervehicle.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "2. Driver & Vehicle Operations", description = "Các API dành riêng cho Tài xế xe điện: Nộp hồ sơ KYC, kiểm tra trạng thái và xác thực khuôn mặt")
@RestController
@RequestMapping("/driver")
@PreAuthorize("hasAuthority('ROLE_DRIVER')")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @Operation(summary = "Tài xế nộp hồ sơ KYC xe điện", description = "Gửi thông tin CCCD, GPLX, biển số xe điện, dung lượng pin kWh cùng 5 ảnh minh chứng (Multipart Form)")
    @PostMapping(value = "/kyc/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycSubmitResponse>> submitKyc(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @ModelAttribute KycSubmissionRequest request) {
        KycSubmitResponse response = driverService.submitKyc(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Hồ sơ đăng ký tài xế và xe điện đã được gửi thành công, vui lòng chờ duyệt", response));
    }

    @Operation(summary = "Xem hồ sơ tài xế và trạng thái KYC", description = "Lấy thông tin chi tiết hồ sơ tài xế và xe điện hiện tại")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        DriverProfileResponse response = driverService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Xác thực khuôn mặt sinh trắc học bật ca", description = "Chụp ảnh selfie khuôn mặt để so khớp Cosine Similarity (>= 0.75) với ảnh hồ sơ KYC đã duyệt trước khi bật ca trực tuyến")
    @PostMapping(value = "/shift/face-verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FaceVerifyResponse>> verifyShiftFace(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("selfieImage") MultipartFile selfieImage) {
        FaceVerifyResponse response = driverService.verifyShiftFace(currentUser.getId(), selfieImage);
        return ResponseEntity.ok(ApiResponse.ok("Xác thực khuôn mặt thành công. Ca làm việc đã được kích hoạt!", response));
    }
}

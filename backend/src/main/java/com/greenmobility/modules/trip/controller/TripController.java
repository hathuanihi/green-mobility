package com.greenmobility.modules.trip.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.trip.dto.TripCancelRequest;
import com.greenmobility.modules.trip.dto.TripEstimateRequest;
import com.greenmobility.modules.trip.dto.TripEstimateResponse;
import com.greenmobility.modules.trip.dto.TripRequestDto;
import com.greenmobility.modules.trip.dto.TripResponseDto;
import com.greenmobility.modules.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "3. Trip Operations", description = "Các API Đặt xe, Dự toán cước phí & CO2, và Quản lý vòng đời chuyến đi")
@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @Operation(summary = "Dự toán cước phí & lượng CO2 giảm", description = "Tính toán khoảng cách, thời gian, cước phí và lượng phát thải giảm trước khi đặt xe")
    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<TripEstimateResponse>> estimateTrip(
            @Valid @RequestBody TripEstimateRequest request) {
        TripEstimateResponse response = tripService.estimateTrip(request);
        return ResponseEntity.ok(ApiResponse.ok("Dự toán lộ trình thành công", response));
    }

    @Operation(summary = "Tạo yêu cầu đặt xe điện", description = "Khách hàng xác nhận lộ trình và tạo chuyến xe mới, kích hoạt Matching Engine tìm tài xế")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<TripResponseDto>> requestTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TripRequestDto request) {
        TripResponseDto response = tripService.createTripRequest(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Yêu cầu đặt xe đã được tạo, hệ thống đang kết nối tài xế gần nhất", response));
    }

    @Operation(summary = "Xem chi tiết chuyến xe", description = "Tra cứu thông tin trạng thái, lộ trình, cước phí và tài xế theo ID chuyến đi")
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponseDto>> getTripDetails(
            @PathVariable UUID tripId) {
        TripResponseDto response = tripService.getTripDetails(tripId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Hủy chuyến xe", description = "Khách hàng hoặc tài xế hủy chuyến xe kèm lý do")
    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<ApiResponse<TripResponseDto>> cancelTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @RequestBody(required = false) TripCancelRequest request) {
        String reason = request != null ? request.getCancelReason() : "Người dùng hủy chuyến";
        TripResponseDto response = tripService.cancelTrip(tripId, currentUser.getId(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy cuốc xe thành công", response));
    }
}

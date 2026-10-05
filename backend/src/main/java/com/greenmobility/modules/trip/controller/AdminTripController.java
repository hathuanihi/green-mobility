package com.greenmobility.modules.trip.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.trip.dto.LiveTripAdminDto;
import com.greenmobility.modules.trip.dto.TodayTripStatsDto;
import com.greenmobility.modules.trip.dto.TripResponseDto;
import com.greenmobility.modules.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "1. Administration Operations", description = "Các API Giám sát và Điều phối Chuyến xe dành cho Quản trị viên")
@RestController
@RequestMapping("/admin/trips")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
public class AdminTripController {

    private final TripService tripService;

    public AdminTripController(TripService tripService) {
        this.tripService = tripService;
    }

    @Operation(summary = "Giám sát các cuốc xe trực tiếp (Live Tracking)", description = "Xem danh sách các cuốc xe đang hoạt động kèm tọa độ GPS thời gian thực và ETA từ Redis")
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<LiveTripAdminDto>>> getLiveTrips() {
        List<LiveTripAdminDto> liveTrips = tripService.getLiveTripsTracking();
        return ResponseEntity.ok(ApiResponse.ok(liveTrips));
    }

    @Operation(summary = "Thống kê hoạt động vận hành trong ngày", description = "Xem số cuốc xe active, thời gian đón trung bình, tổng km xe chạy và lượng CO2 giảm phát thải hôm nay")
    @GetMapping("/stats/today")
    public ResponseEntity<ApiResponse<TodayTripStatsDto>> getTodayStats() {
        TodayTripStatsDto stats = tripService.getTodayTripStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @Operation(summary = "Xem toàn bộ lịch sử chuyến xe", description = "Truy xuất danh sách tất cả các cuốc xe trên hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponseDto>>> getAllTrips() {
        List<TripResponseDto> allTrips = tripService.getAllTrips();
        return ResponseEntity.ok(ApiResponse.ok(allTrips));
    }
}


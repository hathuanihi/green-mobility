package com.greenmobility.modules.trip.controller;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.matching.repository.DriverGeoRedisRepository;
import com.greenmobility.modules.trip.dto.DriverLocationPingRequest;
import com.greenmobility.modules.trip.dto.DriverTripResponseDto;
import com.greenmobility.modules.trip.dto.TripCancelRequest;
import com.greenmobility.modules.trip.dto.TripResponseDto;
import com.greenmobility.modules.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "2. Driver & Vehicle Operations", description = "Các API điều phối cuốc xe và cập nhật vị trí thời gian thực dành cho Tài xế")
@RestController
@RequestMapping("/driver")
@PreAuthorize("hasAuthority('ROLE_DRIVER')")
public class DriverTripController {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverGeoRedisRepository driverGeoRepository;
    private final TripService tripService;

    public DriverTripController(DriverProfileRepository driverProfileRepository,
                                VehicleRepository vehicleRepository,
                                DriverGeoRedisRepository driverGeoRepository,
                                TripService tripService) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverGeoRepository = driverGeoRepository;
        this.tripService = tripService;
    }

    @Operation(summary = "Đồng bộ vị trí & dung lượng pin xe điện", description = "Tài xế định kỳ gửi tọa độ GPS và % pin xe điện lên Redis GEO khi đang trực tuyến")
    @PostMapping("/location/ping")
    public ResponseEntity<ApiResponse<Void>> pingLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody DriverLocationPingRequest request) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        if (!Boolean.TRUE.equals(profile.getIsActiveShift())) {
            throw new BadRequestException("Tài xế chưa bật ca làm việc! Vui lòng hoàn thành xác thực khuôn mặt trước khi phát tín hiệu GPS.");
        }

        Vehicle vehicle = vehicleRepository.findByDriverId(profile.getId())
                .orElseThrow(() -> new BadRequestException("Tài xế chưa liên kết phương tiện xe điện"));

        driverGeoRepository.updateLocation(
                profile.getId(),
                vehicle.getVehicleType(),
                request.getLat(),
                request.getLng(),
                request.getBatteryPercent() != null ? request.getBatteryPercent() : 100
        );

        return ResponseEntity.ok(ApiResponse.ok("Vị trí và dung lượng pin xe điện đã được đồng bộ", null));
    }

    @Operation(summary = "Tài xế chấp nhận cuốc xe", description = "Xác nhận nhận đơn trong vòng 15 giây đếm ngược")
    @PostMapping("/trips/{tripId}/accept")
    public ResponseEntity<ApiResponse<DriverTripResponseDto>> acceptTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        DriverTripResponseDto response = tripService.acceptTrip(profile.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok("Nhận cuốc xe thành công! Vui lòng di chuyển tới điểm đón khách", response));
    }

    @Operation(summary = "Tài xế từ chối cuốc xe", description = "Bỏ qua cuốc xe để hệ thống điều phối cho tài xế tiếp theo")
    @PostMapping("/trips/{tripId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @RequestBody(required = false) TripCancelRequest request) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        String reason = request != null ? request.getCancelReason() : "Tài xế bận";
        tripService.declineTrip(profile.getId(), tripId, reason);

        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối cuốc xe, hệ thống đang chuyển sang tài xế kế tiếp", null));
    }

    @Operation(summary = "Xem cuốc xe đang thực hiện của tài xế", description = "Lấy thông tin cuốc xe đang trong quá trình đón khách hoặc chở khách")
    @GetMapping("/trips/current")
    public ResponseEntity<ApiResponse<TripResponseDto>> getCurrentTrip(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        TripResponseDto currentTrip = tripService.getCurrentActiveTripForDriver(profile.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(currentTrip));
    }

    @Operation(summary = "Kiểm tra yêu cầu điều phối cuốc xe mới", description = "Tài xế định kỳ kiểm tra cuốc xe đang được điều phối cho mình trong 15s đếm ngược")
    @GetMapping("/trips/dispatch/pending")
    public ResponseEntity<ApiResponse<com.greenmobility.modules.trip.dto.DispatchNotificationDto>> getPendingDispatch(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        com.greenmobility.modules.trip.dto.DispatchNotificationDto pending = driverGeoRepository.getPendingDispatch(profile.getId());
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }
}

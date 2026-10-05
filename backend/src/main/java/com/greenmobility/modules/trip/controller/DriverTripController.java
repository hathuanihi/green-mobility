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
import com.greenmobility.modules.trip.dto.*;
import com.greenmobility.modules.trip.service.GpsTelemetryService;
import com.greenmobility.modules.trip.service.TripService;
import com.greenmobility.modules.trip.service.TripTrackingRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final GpsTelemetryService gpsTelemetryService;
    private final TripTrackingRedisService tripTrackingRedisService;

    public DriverTripController(DriverProfileRepository driverProfileRepository,
                                VehicleRepository vehicleRepository,
                                DriverGeoRedisRepository driverGeoRepository,
                                TripService tripService,
                                GpsTelemetryService gpsTelemetryService,
                                TripTrackingRedisService tripTrackingRedisService) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverGeoRepository = driverGeoRepository;
        this.tripService = tripService;
        this.gpsTelemetryService = gpsTelemetryService;
        this.tripTrackingRedisService = tripTrackingRedisService;
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
    public ResponseEntity<ApiResponse<DispatchNotificationDto>> getPendingDispatch(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        DispatchNotificationDto pending = driverGeoRepository.getPendingDispatch(profile.getId());
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    // ==========================================
    // SPRINT 3 PHASE 1: TRIP EXECUTION ENDPOINTS
    // ==========================================

    @Operation(summary = "Tài xế bắt đầu di chuyển đón khách", description = "Chuyển trạng thái sang DRIVER_ARRIVING, lấy lộ trình dẫn đường OSRM/Goong tới điểm đón")
    @PostMapping("/trips/{tripId}/start-arriving")
    public ResponseEntity<ApiResponse<DriverArrivingResponseDto>> startArriving(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        DriverArrivingResponseDto response = tripService.startArriving(profile.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Đang di chuyển đến đón khách. Vui lòng làm theo hướng dẫn điều hướng!",
                response
        ));
    }

    @Operation(summary = "Tài xế xác nhận đã đến điểm đón", description = "Xác nhận đã đến điểm đón khi cách <= 200m, chuyển trạng thái sang ARRIVED")
    @PostMapping("/trips/{tripId}/arrive")
    public ResponseEntity<ApiResponse<DriverArriveResponseDto>> arriveAtPickup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        DriverArriveResponseDto response = tripService.arriveAtPickup(profile.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Bạn đã đến điểm đón! Đang thông báo cho khách hàng ra xe.",
                response
        ));
    }

    @Operation(summary = "Tài xế bắt đầu chuyến đi", description = "Khách đã lên xe, chuyển trạng thái sang IN_TRIP và nhận lộ trình tới điểm trả")
    @PostMapping("/trips/{tripId}/start-trip")
    public ResponseEntity<ApiResponse<DriverStartTripResponseDto>> startTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        DriverStartTripResponseDto response = tripService.startTrip(profile.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Chuyến đi đã bắt đầu! Cùng di chuyển xanh bảo vệ môi trường!",
                response
        ));
    }

    @Operation(summary = "Tài xế hoàn thành chuyến đi", description = "Đã tới điểm trả, tính quãng đường thực tế qua MongoDB telemetry và hoàn thành cuốc xe")
    @PostMapping("/trips/{tripId}/complete")
    public ResponseEntity<ApiResponse<TripCompleteSummaryDto>> completeTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        TripCompleteSummaryDto response = tripService.completeTrip(profile.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Chuyến đi hoàn thành! Cảm ơn bạn đã đóng góp cho môi trường xanh!",
                response
        ));
    }

    @Operation(summary = "Tài xế hủy cuốc xe đang thực hiện", description = "Chỉ cho phép hủy khi đang DRIVER_ARRIVING hoặc ARRIVED (không hủy khi IN_TRIP)")
    @PostMapping("/trips/{tripId}/cancel")
    public ResponseEntity<ApiResponse<DriverCancelResponseDto>> cancelTrip(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @RequestBody(required = false) TripCancelRequest request) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        String reason = request != null ? request.getCancelReason() : "Tài xế hủy cuốc";
        DriverCancelResponseDto response = tripService.cancelByDriver(profile.getId(), tripId, reason);
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã hủy cuốc xe. Bạn đã được đưa trở lại trạng thái sẵn sàng nhận cuốc.",
                response
        ));
    }

    @Operation(summary = "Đồng bộ GPS batch (Offline Sync)", description = "Tài xế đồng bộ chuỗi điểm GPS đã tích lũy trên thiết bị khi mất kết nối mạng")
    @PostMapping("/trips/{tripId}/sync-gps-batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncGpsBatch(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID tripId,
            @Valid @RequestBody GpsBatchSyncRequest request) {

        DriverProfile profile = driverProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế"));

        TripResponseDto trip = tripService.getTripDetails(tripId);

        Instant minValidTime = trip.getMatchedAt() != null ? trip.getMatchedAt() : trip.getRequestedAt();
        Instant maxValidTime = java.time.Instant.now().plusSeconds(60);

        List<GpsTelemetryService.GpsPointData> validPointDataList = request.getPoints().stream()
                .filter(p -> p.getTimestamp() != null && !p.getTimestamp().isBefore(minValidTime) && !p.getTimestamp().isAfter(maxValidTime))
                .map(p -> new GpsTelemetryService.GpsPointData(
                        p.getLat(), p.getLng(), p.getSpeedKmh(), p.getBearing(),
                        null, p.getAccuracy(), p.getBatteryPercent(), p.getIsMockLocation(),
                        p.getTimestamp()
                )).toList();

        int savedCount = gpsTelemetryService.saveGpsPointsBatch(
                tripId.toString(),
                profile.getId().toString(),
                trip.getVehicleType().name(),
                trip.getStatus().name(),
                validPointDataList
        );

        // Update Redis with the latest point from the batch
        validPointDataList.stream()
                .max(java.util.Comparator.comparing(GpsTelemetryService.GpsPointData::timestamp))
                .ifPresent(latest -> {
                    tripTrackingRedisService.updateDriverLocation(
                            tripId,
                            profile.getId(),
                            latest.lat(),
                            latest.lng(),
                            latest.bearing(),
                            latest.speedKmh(),
                            latest.batteryPercent(),
                            null,
                            null
                    );
                });

        Map<String, Object> result = Map.of(
                "syncedPoints", savedCount,
                "skippedDuplicates", request.getPoints().size() - savedCount
        );

        return ResponseEntity.ok(ApiResponse.ok(
                String.format("Đã đồng bộ %d điểm GPS thành công", savedCount),
                result
        ));
    }
}

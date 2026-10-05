package com.greenmobility.modules.fraud.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.fraud.dto.FraudAlertDto;
import com.greenmobility.modules.fraud.dto.FraudStatsDto;
import com.greenmobility.modules.fraud.entity.FraudAlert;
import com.greenmobility.modules.fraud.repository.FraudAlertRepository;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "4. Admin Fraud & Safety Monitoring", description = "Các API Giám sát Gian lận GPS, Tốc độ dị thường và Rủi ro hành trình")
@RestController
@RequestMapping("/admin/fraud")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
public class AdminFraudController {

    private final FraudAlertRepository fraudAlertRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public AdminFraudController(
            FraudAlertRepository fraudAlertRepository,
            DriverProfileRepository driverProfileRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    private void seedInitialAlertsIfEmpty() {
        if (fraudAlertRepository.count() == 0) {
            // Check if we have drivers
            List<DriverProfile> drivers = driverProfileRepository.findAll();
            UUID sampleDriverId = drivers.isEmpty() ? null : drivers.get(0).getId();

            FraudAlert alert1 = new FraudAlert(
                    null,
                    sampleDriverId,
                    null,
                    "GPS_SPOOFING",
                    new BigDecimal("96.50"),
                    "{\"reason\":\"Phát hiện Mock Location Provider trên thiết bị tài xế. Tọa độ nhảy vọt 5.2km trong 3 giây.\",\"app\":\"FakeGPS Pro v4.1\",\"speed_recorded\":\"1240 km/h\"}"
            );

            FraudAlert alert2 = new FraudAlert(
                    null,
                    sampleDriverId,
                    null,
                    "HIGH_SPEED_ANOMALY",
                    new BigDecimal("82.00"),
                    "{\"reason\":\"Xe máy điện VinFast Feliz vượt quá tốc độ an toàn đô thị.\",\"speed_recorded\":\"118 km/h\",\"speed_limit\":\"50 km/h\",\"route\":\"Đại lộ Thăng Long\"}"
            );

            FraudAlert alert3 = new FraudAlert(
                    null,
                    sampleDriverId,
                    null,
                    "BATTERY_DRAIN_MISMATCH",
                    new BigDecimal("74.20"),
                    "{\"reason\":\"Lượng tiêu hao pin không tương thích với quãng đường di chuyển thực tế (Nghi vấn khống hành trình để nhận thưởng Carbon).\"," +
                            "\"expected_kwh\":\"1.8 kWh\",\"actual_kwh\":\"0.05 kWh\",\"distance\":\"12.4 km\"}"
            );
            alert3.setResolutionStatus("RESOLVED");
            alert3.setResolvedAt(Instant.now());

            fraudAlertRepository.saveAll(Arrays.asList(alert1, alert2, alert3));
        }
    }

    @Operation(summary = "Lấy danh sách cảnh báo gian lận GPS & an toàn")
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<FraudAlertDto>>> getAlerts(
            @RequestParam(required = false) String status) {
        seedInitialAlertsIfEmpty();

        List<FraudAlert> list;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            list = fraudAlertRepository.findByResolutionStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            list = fraudAlertRepository.findByOrderByCreatedAtDesc();
        }

        List<FraudAlertDto> dtos = list.stream().map(a -> {
            String driverName = "Tài xế Hệ Thống";
            String driverPhone = "-";
            String vehiclePlate = "-";

            if (a.getDriverId() != null) {
                Optional<DriverProfile> p = driverProfileRepository.findById(a.getDriverId());
                if (p.isPresent()) {
                    if (p.get().getUserId() != null) {
                        Optional<User> u = userRepository.findById(p.get().getUserId());
                        if (u.isPresent()) {
                            driverName = u.get().getFullName();
                            driverPhone = u.get().getPhoneNumber();
                        }
                    }
                    Optional<Vehicle> vehOpt = vehicleRepository.findByDriverId(p.get().getId());
                    if (vehOpt.isPresent()) {
                        vehiclePlate = vehOpt.get().getLicensePlate();
                    }
                }
            }

            return FraudAlertDto.fromEntity(a, driverName, driverPhone, vehiclePlate);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách cảnh báo thành công", dtos));
    }

    @Operation(summary = "Thống kê tổng quan rủi ro & gian lận")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FraudStatsDto>> getStats() {
        seedInitialAlertsIfEmpty();
        long total = fraudAlertRepository.count();
        long pending = fraudAlertRepository.countByResolutionStatus("PENDING");
        long resolved = fraudAlertRepository.countByResolutionStatus("RESOLVED");
        double highRiskRatio = total > 0 ? (double) pending / total * 100.0 : 0.0;

        return ResponseEntity.ok(ApiResponse.ok(new FraudStatsDto(total, pending, resolved, Math.round(highRiskRatio * 10.0) / 10.0)));
    }

    @Operation(summary = "Xử lý / Giải quyết cảnh báo gian lận")
    @PatchMapping("/alerts/{id}/resolve")
    public ResponseEntity<ApiResponse<FraudAlertDto>> resolveAlert(
            @PathVariable UUID id,
            @RequestParam String status) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo với ID: " + id));

        alert.setResolutionStatus(status.toUpperCase());
        alert.setResolvedAt(Instant.now());
        FraudAlert saved = fraudAlertRepository.save(alert);

        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái xử lý cảnh báo thành công",
                FraudAlertDto.fromEntity(saved, null, null, null)));
    }
}

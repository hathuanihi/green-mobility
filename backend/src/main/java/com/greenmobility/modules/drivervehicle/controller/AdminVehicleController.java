package com.greenmobility.modules.drivervehicle.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.modules.drivervehicle.dto.AdminVehicleDto;
import com.greenmobility.modules.drivervehicle.entity.DriverProfile;
import com.greenmobility.modules.drivervehicle.entity.Vehicle;
import com.greenmobility.modules.drivervehicle.entity.VehicleType;
import com.greenmobility.modules.drivervehicle.repository.DriverProfileRepository;
import com.greenmobility.modules.drivervehicle.repository.VehicleRepository;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "3. Admin Vehicle Management", description = "Các API Quản lý và Giám sát Đội xe điện VinFast dành cho Quản trị viên")
@RestController
@RequestMapping("/admin/vehicles")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
public class AdminVehicleController {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;

    public AdminVehicleController(
            VehicleRepository vehicleRepository,
            DriverProfileRepository driverProfileRepository,
            UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Lấy danh sách đội xe điện", description = "Xem danh sách xe điện VinFast kèm thông số pin kWh, cự ly và tài xế phụ trách")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminVehicleDto>>> getAllVehicles(
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) Boolean isVerified) {
        List<Vehicle> list = vehicleRepository.findAll();

        if (vehicleType != null) {
            list = list.stream().filter(v -> v.getVehicleType() == vehicleType).collect(Collectors.toList());
        }
        if (isVerified != null) {
            list = list.stream().filter(v -> Objects.equals(v.getIsVerified(), isVerified)).collect(Collectors.toList());
        }

        // Map driver names
        List<AdminVehicleDto> dtos = list.stream().map(v -> {
            String driverName = "Chưa gán";
            String driverPhone = "";
            if (v.getDriverId() != null) {
                Optional<DriverProfile> profile = driverProfileRepository.findById(v.getDriverId());
                if (profile.isPresent() && profile.get().getUserId() != null) {
                    Optional<User> u = userRepository.findById(profile.get().getUserId());
                    if (u.isPresent()) {
                        driverName = u.get().getFullName();
                        driverPhone = u.get().getPhoneNumber();
                    }
                }
            }
            return AdminVehicleDto.fromEntity(v, driverName, driverPhone);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đội xe điện thành công", dtos));
    }

    @Operation(summary = "Xem chi tiết xe điện theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminVehicleDto>> getVehicleById(@PathVariable UUID id) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phương tiện với ID: " + id));

        String driverName = "Chưa gán";
        String driverPhone = "";
        if (v.getDriverId() != null) {
            Optional<DriverProfile> profile = driverProfileRepository.findById(v.getDriverId());
            if (profile.isPresent() && profile.get().getUserId() != null) {
                Optional<User> u = userRepository.findById(profile.get().getUserId());
                if (u.isPresent()) {
                    driverName = u.get().getFullName();
                    driverPhone = u.get().getPhoneNumber();
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(AdminVehicleDto.fromEntity(v, driverName, driverPhone)));
    }

    @Operation(summary = "Xác minh / Duyệt phương tiện xe điện")
    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<AdminVehicleDto>> verifyVehicle(
            @PathVariable UUID id,
            @RequestParam boolean verified) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phương tiện với ID: " + id));
        v.setIsVerified(verified);
        Vehicle saved = vehicleRepository.save(v);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái xác minh xe thành công", AdminVehicleDto.fromEntity(saved, null, null)));
    }
}

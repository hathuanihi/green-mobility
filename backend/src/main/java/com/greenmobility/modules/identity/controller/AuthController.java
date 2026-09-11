package com.greenmobility.modules.identity.controller;

import com.greenmobility.common.response.ApiResponse;
import com.greenmobility.common.security.UserPrincipal;
import com.greenmobility.modules.identity.dto.AuthResponse;
import com.greenmobility.modules.identity.dto.LoginRequest;
import com.greenmobility.modules.identity.dto.RegisterRequest;
import com.greenmobility.modules.identity.dto.UserResponse;
import com.greenmobility.modules.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Identity & Authentication", description = "Các API đăng ký, đăng nhập và lấy thông tin tài khoản người dùng")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Đăng ký tài khoản mới", description = "Đăng ký người dùng với vai trò Khách hàng (ROLE_CUSTOMER) hoặc Tài xế (ROLE_DRIVER)")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký tài khoản thành công", response));
    }

    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập bằng số điện thoại và mật khẩu để lấy JWT Bearer Token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Xem hồ sơ người dùng hiện tại dựa trên Bearer Token")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập hoặc token không hợp lệ"));
        }
        UserResponse response = authService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

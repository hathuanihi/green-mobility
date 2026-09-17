package com.greenmobility.modules.identity.service;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.exception.ResourceNotFoundException;
import com.greenmobility.common.security.JwtTokenProvider;
import com.greenmobility.modules.drivervehicle.service.DriverPublicService;
import com.greenmobility.modules.identity.dto.AuthResponse;
import com.greenmobility.modules.identity.dto.LoginRequest;
import com.greenmobility.modules.identity.dto.RegisterRequest;
import com.greenmobility.modules.identity.dto.UserResponse;
import com.greenmobility.modules.identity.entity.Role;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final DriverPublicService driverPublicService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       DriverPublicService driverPublicService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.driverPublicService = driverPublicService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() != Role.ROLE_CUSTOMER && request.getRole() != Role.ROLE_DRIVER) {
            throw new BadRequestException("Vai trò đăng ký không hợp lệ. Chỉ chấp nhận ROLE_CUSTOMER hoặc ROLE_DRIVER");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Số điện thoại này đã được đăng ký trong hệ thống");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email này đã được sử dụng");
        }

        User user = new User(
                request.getPhoneNumber(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getRole()
        );

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getPhoneNumber(), savedUser.getRole().name());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getPhoneNumber(),
                savedUser.getFullName(),
                savedUser.getRole().name(),
                token,
                86400,
                null
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Số điện thoại hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Số điện thoại hoặc mật khẩu không chính xác");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getPhoneNumber(), user.getRole().name());

        String kycStatus = null;
        if (user.getRole() == Role.ROLE_DRIVER) {
            kycStatus = driverPublicService.getKycStatusByUserId(user.getId()).orElse(null);
        }

        return new AuthResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getRole().name(),
                token,
                86400,
                kycStatus
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return new UserResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}

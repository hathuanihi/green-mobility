package com.greenmobility.identity;

import com.greenmobility.common.exception.BadRequestException;
import com.greenmobility.common.security.JwtTokenProvider;
import com.greenmobility.modules.identity.dto.AuthResponse;
import com.greenmobility.modules.identity.dto.LoginRequest;
import com.greenmobility.modules.identity.dto.RegisterRequest;
import com.greenmobility.modules.identity.entity.Role;
import com.greenmobility.modules.identity.entity.User;
import com.greenmobility.modules.identity.repository.UserRepository;
import com.greenmobility.modules.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                86400
        );
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest(
                "0901234567",
                "Password123@",
                "Phạm Hà Anh Thư",
                "anhthu@greenmobility.vn",
                Role.ROLE_CUSTOMER
        );

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByEmail("anhthu@greenmobility.vn")).thenReturn(false);
        when(passwordEncoder.encode("Password123@")).thenReturn("hashedPassword");

        User savedUser = new User("0901234567", "anhthu@greenmobility.vn", "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_CUSTOMER);
        savedUser.setId(UUID.randomUUID());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("0901234567", response.getPhoneNumber());
        assertNotNull(response.getToken());
        assertEquals("ROLE_CUSTOMER", response.getRole());
    }

    @Test
    void testRegister_DuplicatePhoneNumber_ThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "0901234567", "Password123@", "Phạm Hà Anh Thư", null, Role.ROLE_CUSTOMER
        );

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("0901234567", "Password123@");

        User user = new User("0901234567", null, "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_DRIVER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123@", "hashedPassword")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("ROLE_DRIVER", response.getRole());
        assertNotNull(response.getToken());
    }

    @Test
    void testLogin_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("0901234567", "WrongPassword");

        User user = new User("0901234567", null, "hashedPassword", "Phạm Hà Anh Thư", Role.ROLE_CUSTOMER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}

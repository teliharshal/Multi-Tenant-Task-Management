package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.LoginRequest;
import com.example.taskmanagement.dto.response.AuthResponse;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.exception.UnauthorizedException;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder().id(1L).name("Tenant A").build();

        user = User.builder()
                .id(10L)
                .email("admin@test.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();

        request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());

        verify(userRepository).findByEmail("admin@test.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldThrowUnauthorized_whenUserNotFound() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail("admin@test.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_shouldThrowUnauthorized_whenPasswordIsInvalid() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail("admin@test.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService, never()).generateToken(any());
    }
}
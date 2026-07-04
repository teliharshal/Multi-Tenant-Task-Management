package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.CreateUserRequest;
import com.example.taskmanagement.dto.response.UserResponse;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.exception.BadRequestException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldCreateUser_whenValidRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        request.setRole(Role.USER);

        Tenant tenant = Tenant.builder()
                .id(1L)
                .name("Tenant A")
                .build();

        User savedUser = User.builder()
                .id(10L)
                .email("user@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .tenant(tenant)
                .build();

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentTenantId).thenReturn(1L);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserResponse response = userService.createUser(request);

            assertNotNull(response);
            assertEquals(10L, response.getId());
            assertEquals("user@test.com", response.getEmail());
            assertEquals(Role.USER, response.getRole());
            assertEquals(1L, response.getTenantId());

            verify(userRepository).findByEmail("user@test.com");
            verify(tenantRepository).findById(1L);
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void createUser_shouldThrowBadRequest_whenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        request.setRole(Role.USER);

        User existingUser = User.builder().id(99L).email("user@test.com").build();

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentTenantId).thenReturn(1L);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(existingUser));

            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> userService.createUser(request)
            );

            assertEquals("User with this email already exists", ex.getMessage());

            verify(userRepository).findByEmail("user@test.com");
            verify(tenantRepository, never()).findById(anyLong());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void createUser_shouldThrowResourceNotFound_whenTenantDoesNotExist() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        request.setRole(Role.USER);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentTenantId).thenReturn(1L);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
            when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.createUser(request)
            );

            assertEquals("Tenant not found", ex.getMessage());

            verify(userRepository).findByEmail("user@test.com");
            verify(tenantRepository).findById(1L);
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }
}
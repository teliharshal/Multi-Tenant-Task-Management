package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.TenantRegisterRequest;
import com.example.taskmanagement.dto.response.TenantRegisterResponse;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void registerTenant_shouldCreateTenantAndAdmin() {
        TenantRegisterRequest request = new TenantRegisterRequest();
        request.setTenantName("Tenant A");
        request.setAdminEmail("admin@test.com");
        request.setAdminPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        Tenant savedTenant = Tenant.builder()
                .id(1L)
                .name("Tenant A")
                .createdAt(LocalDateTime.now())
                .build();

        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        User savedAdmin = User.builder()
                .id(100L)
                .email("admin@test.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .tenant(savedTenant)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedAdmin);

        TenantRegisterResponse response = tenantService.registerTenant(request);

        assertNotNull(response);
        assertEquals(1L, response.getTenantId());
        assertEquals("Tenant A", response.getTenantName());
        assertEquals("admin@test.com", response.getAdminEmail());
        assertEquals("Tenant registered successfully", response.getMessage());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());

        Tenant tenantToSave = tenantCaptor.getValue();
        assertEquals("Tenant A", tenantToSave.getName());
        assertNotNull(tenantToSave.getCreatedAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User adminToSave = userCaptor.getValue();
        assertEquals("admin@test.com", adminToSave.getEmail());
        assertEquals("encodedPassword", adminToSave.getPassword());
        assertEquals(Role.ADMIN, adminToSave.getRole());
        assertEquals(savedTenant, adminToSave.getTenant());

        verify(passwordEncoder).encode("password123");
    }
}
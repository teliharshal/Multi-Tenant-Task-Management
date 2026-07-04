package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.TenantRegisterRequest;
import com.example.taskmanagement.dto.response.TenantRegisterResponse;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantRegisterResponse registerTenant(TenantRegisterRequest request) {
        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .createdAt(LocalDateTime.now())
                .build();

        tenant = tenantRepository.save(tenant);

        User admin = User.builder()
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();

        userRepository.save(admin);

        return TenantRegisterResponse.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .adminEmail(admin.getEmail())
                .message("Tenant registered successfully")
                .build();
    }
}
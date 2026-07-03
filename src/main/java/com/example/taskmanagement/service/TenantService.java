package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.TenantRegisterRequest;
import com.example.taskmanagement.dto.response.UserResponse;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.exception.BadRequestException;
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

    public UserResponse registerTenant(TenantRegisterRequest request) {
        if (tenantRepository.findByName(request.getTenantName()).isPresent()) {
            throw new BadRequestException("Tenant with this name already exists");
        }

        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new BadRequestException("Admin email already exists");
        }

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

        admin = userRepository.save(admin);

        return UserResponse.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .role(admin.getRole())
                .tenantId(tenant.getId())
                .build();
    }
}
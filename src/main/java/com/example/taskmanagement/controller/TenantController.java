package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.request.TenantRegisterRequest;
import com.example.taskmanagement.dto.response.UserResponse;
import com.example.taskmanagement.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerTenant(@Valid @RequestBody TenantRegisterRequest request) {
        UserResponse response = tenantService.registerTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
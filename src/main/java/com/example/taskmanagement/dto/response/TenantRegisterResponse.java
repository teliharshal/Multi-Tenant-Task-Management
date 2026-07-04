package com.example.taskmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegisterResponse {
    private Long tenantId;
    private String tenantName;
    private String adminEmail;
    private String message;
}
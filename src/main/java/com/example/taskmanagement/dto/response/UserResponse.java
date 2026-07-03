package com.example.taskmanagement.dto.response;

import com.example.taskmanagement.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private Role role;
    private Long tenantId;
}
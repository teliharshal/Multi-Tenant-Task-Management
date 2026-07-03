package com.example.taskmanagement.dto.response;

import com.example.taskmanagement.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assignedToUserId;
    private String assignedToUserEmail;
    private Long tenantId;
}
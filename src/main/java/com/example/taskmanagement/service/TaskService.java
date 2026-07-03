package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.request.AssignTaskRequest;
import com.example.taskmanagement.dto.request.CreateTaskRequest;
import com.example.taskmanagement.dto.request.UpdateTaskStatusRequest;
import com.example.taskmanagement.dto.response.TaskResponse;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.exception.AccessDeniedCustomException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TaskResponse createTask(CreateTaskRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING)
                .tenant(tenant)
                .build();

        task = taskRepository.save(task);
        return mapToTaskResponse(task);
    }

    public TaskResponse assignTask(Long taskId, AssignTaskRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for current tenant"));

        User user = userRepository.findByIdAndTenantId(request.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for current tenant"));

        task.setAssignedTo(user);
        task = taskRepository.save(task);

        return mapToTaskResponse(task);
    }

    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for current tenant"));

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUserId)) {
            throw new AccessDeniedCustomException("You can update only tasks assigned to you");
        }

        task.setStatus(request.getStatus());
        task = taskRepository.save(task);

        return mapToTaskResponse(task);
    }

    public List<TaskResponse> getTasks() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Role role = SecurityUtils.getCurrentUser().getRole();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        List<Task> tasks;

        if (role == Role.USER) {
            tasks = taskRepository.findByAssignedToIdAndTenantId(currentUserId, tenantId);
        } else {
            tasks = taskRepository.findAllByTenantId(tenantId);
        }

        return tasks.stream()
                .map(this::mapToTaskResponse)
                .toList();
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedToUserId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToUserEmail(task.getAssignedTo() != null ? task.getAssignedTo().getEmail() : null)
                .tenantId(task.getTenant().getId())
                .build();
    }
}
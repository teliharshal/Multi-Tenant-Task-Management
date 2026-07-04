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
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.exception.UnauthorizedException;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.SecurityUtils;
import com.example.taskmanagement.security.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskResponse createTask(CreateTaskRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING)
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public TaskResponse assignTask(Long taskId, AssignTaskRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for current tenant"));

        User user = userRepository.findByIdAndTenantId(request.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for current tenant"));

        if (user.getRole() != Role.USER) {
            throw new UnauthorizedException("Task can only be assigned to a USER");
        }

        task.setAssignedTo(user);
        Task updatedTask = taskRepository.save(task);

        return mapToResponse(updatedTask);
    }

    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for current tenant"));

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can update only tasks assigned to you");
        }

        task.setStatus(request.getStatus());
        Task updatedTask = taskRepository.save(task);

        return mapToResponse(updatedTask);
    }

    public List<TaskResponse> getTasks() {
        Long tenantId = TenantContext.getTenantId();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();

        List<Task> tasks;

        if (currentUserRole.equals("ADMIN") || currentUserRole.equals("MANAGER")) {
            tasks = taskRepository.findAllByTenantId(tenantId);
        } else {
            tasks = taskRepository.findAllByTenantIdAndAssignedToId(tenantId, currentUserId);
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedToUserId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToUserEmail(task.getAssignedTo() != null ? task.getAssignedTo().getEmail() : null)
                .tenantId(task.getTenant() != null ? task.getTenant().getId() : null)
                .build();
    }
}
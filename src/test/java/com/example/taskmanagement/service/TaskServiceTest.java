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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private Tenant tenant;
    private User admin;
    private User manager;
    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .id(1L)
                .name("Tenant A")
                .build();

        admin = User.builder()
                .id(10L)
                .email("admin@test.com")
                .password("encoded")
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();

        manager = User.builder()
                .id(11L)
                .email("manager@test.com")
                .password("encoded")
                .role(Role.MANAGER)
                .tenant(tenant)
                .build();

        user = User.builder()
                .id(12L)
                .email("user@test.com")
                .password("encoded")
                .role(Role.USER)
                .tenant(tenant)
                .build();

        task = Task.builder()
                .id(100L)
                .title("Prepare report")
                .description("Prepare monthly report")
                .status(TaskStatus.PENDING)
                .tenant(tenant)
                .assignedTo(null)
                .build();
    }

    @Test
    void createTask_shouldCreateTaskSuccessfully() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("New Task Description");

        Task savedTask = Task.builder()
                .id(1L)
                .title("New Task")
                .description("New Task Description")
                .status(TaskStatus.PENDING)
                .tenant(tenant)
                .build();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);

            when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

            TaskResponse response = taskService.createTask(request);

            assertNotNull(response);
            assertEquals("New Task", response.getTitle());
            assertEquals("New Task Description", response.getDescription());
            assertEquals(TaskStatus.PENDING, response.getStatus());
            assertEquals(1L, response.getTenantId());

            verify(taskRepository, times(1)).save(any(Task.class));
        }
    }

    @Test
    void assignTask_shouldAssignTaskSuccessfully() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setUserId(user.getId());

        task.setAssignedTo(null);

        Task updatedTask = Task.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .tenant(task.getTenant())
                .assignedTo(user)
                .build();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));
            when(userRepository.findByIdAndTenantId(user.getId(), 1L)).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

            TaskResponse response = taskService.assignTask(task.getId(), request);

            assertNotNull(response);
            assertEquals(user.getId(), response.getAssignedToUserId());
            assertEquals(user.getEmail(), response.getAssignedToUserEmail());

            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(userRepository).findByIdAndTenantId(user.getId(), 1L);
            verify(taskRepository).save(any(Task.class));
        }
    }

    @Test
    void assignTask_shouldThrowResourceNotFound_whenTaskNotFound() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setUserId(user.getId());

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);

            when(taskRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.assignTask(999L, request)
            );

            assertEquals("Task not found for current tenant", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(999L, 1L);
            verify(userRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
        }
    }

    @Test
    void assignTask_shouldThrowResourceNotFound_whenUserNotFound() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setUserId(999L);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));
            when(userRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.assignTask(task.getId(), request)
            );

            assertEquals("User not found for current tenant", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(userRepository).findByIdAndTenantId(999L, 1L);
        }
    }

    @Test
    void assignTask_shouldThrowUnauthorized_whenUserRoleIsNotUSER() {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setUserId(manager.getId());

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));
            when(userRepository.findByIdAndTenantId(manager.getId(), 1L)).thenReturn(Optional.of(manager));

            UnauthorizedException ex = assertThrows(
                    UnauthorizedException.class,
                    () -> taskService.assignTask(task.getId(), request)
            );

            assertEquals("Task can only be assigned to a USER", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(userRepository).findByIdAndTenantId(manager.getId(), 1L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Test
    void updateTaskStatus_shouldUpdateSuccessfully_whenTaskAssignedToCurrentUser() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.COMPLETED);

        task.setAssignedTo(user);

        Task updatedTask = Task.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(TaskStatus.COMPLETED)
                .tenant(task.getTenant())
                .assignedTo(user)
                .build();

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(user.getId());

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

            TaskResponse response = taskService.updateTaskStatus(task.getId(), request);

            assertNotNull(response);
            assertEquals(TaskStatus.COMPLETED, response.getStatus());
            assertEquals(user.getId(), response.getAssignedToUserId());

            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(taskRepository).save(any(Task.class));
        }
    }

    @Test
    void updateTaskStatus_shouldThrowUnauthorized_whenTaskAssignedToAnotherUser() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.COMPLETED);

        User anotherUser = User.builder()
                .id(99L)
                .email("another@test.com")
                .role(Role.USER)
                .tenant(tenant)
                .build();

        task.setAssignedTo(anotherUser);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(user.getId());

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));

            UnauthorizedException ex = assertThrows(
                    UnauthorizedException.class,
                    () -> taskService.updateTaskStatus(task.getId(), request)
            );

            assertEquals("You can update only tasks assigned to you", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Test
    void updateTaskStatus_shouldThrowUnauthorized_whenTaskNotAssigned() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.COMPLETED);

        task.setAssignedTo(null);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(user.getId());

            when(taskRepository.findByIdAndTenantId(task.getId(), 1L)).thenReturn(Optional.of(task));

            UnauthorizedException ex = assertThrows(
                    UnauthorizedException.class,
                    () -> taskService.updateTaskStatus(task.getId(), request)
            );

            assertEquals("You can update only tasks assigned to you", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(task.getId(), 1L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Test
    void updateTaskStatus_shouldThrowResourceNotFound_whenTaskNotFound() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.COMPLETED);

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(user.getId());

            when(taskRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.updateTaskStatus(999L, request)
            );

            assertEquals("Task not found for current tenant", ex.getMessage());
            verify(taskRepository).findByIdAndTenantId(999L, 1L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Test
    void getTasks_shouldReturnAllTenantTasks_forAdmin() {
        List<Task> taskList = List.of(
                Task.builder()
                        .id(1L)
                        .title("Task 1")
                        .description("Desc 1")
                        .status(TaskStatus.PENDING)
                        .tenant(tenant)
                        .assignedTo(user)
                        .build(),
                Task.builder()
                        .id(2L)
                        .title("Task 2")
                        .description("Desc 2")
                        .status(TaskStatus.IN_PROGRESS)
                        .tenant(tenant)
                        .assignedTo(null)
                        .build()
        );

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(admin.getId());
            securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("ADMIN");

            when(taskRepository.findAllByTenantId(1L)).thenReturn(taskList);

            List<TaskResponse> responses = taskService.getTasks();

            assertEquals(2, responses.size());
            assertEquals("Task 1", responses.get(0).getTitle());
            assertEquals("Task 2", responses.get(1).getTitle());

            verify(taskRepository).findAllByTenantId(1L);
            verify(taskRepository, never()).findAllByTenantIdAndAssignedToId(anyLong(), anyLong());
        }
    }

    @Test
    void getTasks_shouldReturnAllTenantTasks_forManager() {
        List<Task> taskList = List.of(
                Task.builder()
                        .id(1L)
                        .title("Manager Task")
                        .description("Desc")
                        .status(TaskStatus.PENDING)
                        .tenant(tenant)
                        .assignedTo(user)
                        .build()
        );

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(manager.getId());
            securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("MANAGER");

            when(taskRepository.findAllByTenantId(1L)).thenReturn(taskList);

            List<TaskResponse> responses = taskService.getTasks();

            assertEquals(1, responses.size());
            assertEquals("Manager Task", responses.get(0).getTitle());

            verify(taskRepository).findAllByTenantId(1L);
            verify(taskRepository, never()).findAllByTenantIdAndAssignedToId(anyLong(), anyLong());
        }
    }

    @Test
    void getTasks_shouldReturnOnlyAssignedTasks_forUser() {
        List<Task> taskList = List.of(
                Task.builder()
                        .id(1L)
                        .title("User Task")
                        .description("User Desc")
                        .status(TaskStatus.PENDING)
                        .tenant(tenant)
                        .assignedTo(user)
                        .build()
        );

        try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class);
             MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {

            tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(user.getId());
            securityUtilsMock.when(SecurityUtils::getCurrentUserRole).thenReturn("USER");

            when(taskRepository.findAllByTenantIdAndAssignedToId(1L, user.getId())).thenReturn(taskList);

            List<TaskResponse> responses = taskService.getTasks();

            assertEquals(1, responses.size());
            assertEquals("User Task", responses.get(0).getTitle());
            assertEquals(user.getId(), responses.get(0).getAssignedToUserId());

            verify(taskRepository).findAllByTenantIdAndAssignedToId(1L, user.getId());
            verify(taskRepository, never()).findAllByTenantId(anyLong());
        }
    }
}
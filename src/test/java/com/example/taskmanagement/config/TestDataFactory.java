package com.example.taskmanagement.config;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.enums.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.TenantRepository;
import com.example.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public Tenant createTenant(String name) {
        Tenant tenant = Tenant.builder()
                .name(name + "-" + UUID.randomUUID())   // unique tenant name
                .createdAt(LocalDateTime.now())
                .build();

        return tenantRepository.save(tenant);
    }

    /**
     * Use when you need a fixed email (for login tests)
     */
    public User createUser(String email, String password, Role role, Tenant tenant) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .tenant(tenant)
                .build();

        return userRepository.save(user);
    }

    /**
     * Use when you want a unique email to avoid duplicate-key failures in tests
     */
    public User createUniqueUser(String emailPrefix, String password, Role role, Tenant tenant) {
        String uniqueEmail = emailPrefix + "_" + UUID.randomUUID() + "@test.com";

        User user = User.builder()
                .email(uniqueEmail)
                .password(passwordEncoder.encode(password))
                .role(role)
                .tenant(tenant)
                .build();

        return userRepository.save(user);
    }

    public Task createTask(String title, Tenant tenant, User assignedTo) {
        Task task = Task.builder()
                .title(title)
                .description(title + " description")
                .status(TaskStatus.PENDING)
                .tenant(tenant)
                .assignedTo(assignedTo)
                .build();

        return taskRepository.save(task);
    }

    public Task createTask(String title, Tenant tenant) {
        Task task = Task.builder()
                .title(title)
                .description(title + " description")
                .status(TaskStatus.PENDING)
                .tenant(tenant)
                .build();

        return taskRepository.save(task);
    }
}
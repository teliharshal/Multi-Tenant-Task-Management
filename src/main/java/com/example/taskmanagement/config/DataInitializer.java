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
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Run only if no tenant exists
        if (tenantRepository.count() > 0) {
            return;
        }

        // 1) Create Tenant
        Tenant tenant = Tenant.builder()
                .name("Demo Tenant")
                .createdAt(LocalDateTime.now())
                .build();
        tenant = tenantRepository.save(tenant);

        // 2) Create Admin
        User admin = User.builder()
                .email("admin@demo.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();
        admin = userRepository.save(admin);

        // 3) Create Manager
        User manager = User.builder()
                .email("manager@demo.com")
                .password(passwordEncoder.encode("manager123"))
                .role(Role.MANAGER)
                .tenant(tenant)
                .build();
        manager = userRepository.save(manager);

        // 4) Create User
        User user = User.builder()
                .email("user@demo.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .tenant(tenant)
                .build();
        user = userRepository.save(user);

        // 5) Create Demo Task
        Task task = Task.builder()
                .title("Demo Task")
                .description("This is a seeded demo task")
                .status(TaskStatus.PENDING)
                .assignedTo(user)
                .tenant(tenant)
                .build();
        taskRepository.save(task);

        System.out.println("====================================================");
        System.out.println("Demo data initialized successfully");
        System.out.println("Tenant: Demo Tenant");
        System.out.println("Admin   -> admin@demo.com / admin123");
        System.out.println("Manager -> manager@demo.com / manager123");
        System.out.println("User    -> user@demo.com / user123");
        System.out.println("====================================================");
    }
}
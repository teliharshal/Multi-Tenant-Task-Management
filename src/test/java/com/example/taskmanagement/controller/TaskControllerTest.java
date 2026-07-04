package com.example.taskmanagement.controller;

import com.example.taskmanagement.config.TestDataFactory;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import com.example.taskmanagement.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory factory;

    @Autowired
    private JwtService jwtService;

    private Tenant tenantA;
    private Tenant tenantB;

    private User managerA;
    private User userA;
    private User userB;

    private String managerAToken;
    private String userAToken;
    private String userBToken;

    private Task taskA;

    @BeforeEach
    void setUp() {
        tenantA = factory.createTenant("Tenant A");
        tenantB = factory.createTenant("Tenant B");

        // use UNIQUE users so tests never clash
        managerA = factory.createUniqueUser("managerA", "password123", Role.MANAGER, tenantA);
        userA = factory.createUniqueUser("userA", "password123", Role.USER, tenantA);
        userB = factory.createUniqueUser("userB", "password123", Role.USER, tenantB);

        managerAToken = jwtService.generateToken(managerA);
        userAToken = jwtService.generateToken(userA);
        userBToken = jwtService.generateToken(userB);

        // IMPORTANT: create taskA here because later tests use taskA.getId()
        taskA = factory.createTask("Task A", tenantA);
    }

    @Test
    void createTask_shouldSucceed_whenCalledByManager() throws Exception {
        String body = """
                {
                  "title": "Prepare report",
                  "description": "Monthly report"
                }
                """;

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Prepare report"))
                .andExpect(jsonPath("$.description").value("Monthly report"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void assignTask_shouldSucceed_whenUserBelongsToSameTenant() throws Exception {
        String body = """
                {
                  "userId": %d
                }
                """.formatted(userA.getId());

        mockMvc.perform(put("/tasks/{id}/assign", taskA.getId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskA.getId()))
                .andExpect(jsonPath("$.assignedToUserId").value(userA.getId()))
                .andExpect(jsonPath("$.assignedToUserEmail").value(userA.getEmail()));
    }

    @Test
    void assignTask_shouldFail_whenUserBelongsToDifferentTenant() throws Exception {
        String body = """
                {
                  "userId": %d
                }
                """.formatted(userB.getId());

        mockMvc.perform(put("/tasks/{id}/assign", taskA.getId())
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_shouldSucceed_whenTaskAssignedToCurrentUser() throws Exception {
        // create a task already assigned to userA
        Task assignedTask = factory.createTask("Assigned Task", tenantA, userA);

        String body = """
                {
                  "status": "COMPLETED"
                }
                """;

        mockMvc.perform(put("/tasks/{id}/status", assignedTask.getId())
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedTask.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.assignedToUserId").value(userA.getId()));
    }

    @Test
    void getTasks_shouldReturnOnlyAssignedTasks_forUser() throws Exception {
        // task for current user in same tenant
        factory.createTask("Task For UserA", tenantA, userA);

        // task in same tenant but not assigned to userA
        factory.createTask("Another Task In TenantA", tenantA);

        // task in another tenant
        factory.createTask("Task For Other Tenant", tenantB, userB);

        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assignedToUserId").value(userA.getId()));
    }
}
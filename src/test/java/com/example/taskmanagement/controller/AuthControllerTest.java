package com.example.taskmanagement.controller;

import com.example.taskmanagement.config.TestDataFactory;
import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory factory;

    private String adminEmail;
    private final String adminPassword = "password123";

    @BeforeEach
    void setUp() {
        Tenant tenant = factory.createTenant("Tenant A");

        // generate unique email every test run
        adminEmail = "admin_" + UUID.randomUUID() + "@test.com";

        User admin = factory.createUser(adminEmail, adminPassword, Role.ADMIN, tenant);
    }

    @Test
    void login_shouldReturnJwt_whenCredentialsAreValid() throws Exception {
        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(adminEmail, adminPassword);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_shouldReturnUnauthorized_whenPasswordIsInvalid() throws Exception {
        String requestBody = """
                {
                  "email": "%s",
                  "password": "wrongpass"
                }
                """.formatted(adminEmail);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
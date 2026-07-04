package com.example.taskmanagement.controller;

import com.example.taskmanagement.config.TestDataFactory;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory factory;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        Tenant tenant = factory.createTenant("Tenant A");

        User admin = factory.createUniqueUser("admin", "password123", Role.ADMIN, tenant);
        User manager = factory.createUniqueUser("manager", "password123", Role.MANAGER, tenant);

        adminToken = jwtService.generateToken(admin);
        managerToken = jwtService.generateToken(manager);
    }

    @Test
    void createUser_shouldSucceed_whenCalledByAdmin() throws Exception {
        String body = """
                {
                  "email": "user1@test.com",
                  "password": "password123",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_shouldFail_whenCalledByManager() throws Exception {
        String body = """
                {
                  "email": "user2@test.com",
                  "password": "password123",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
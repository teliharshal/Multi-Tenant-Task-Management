package com.example.taskmanagement.security;

import com.example.taskmanagement.entity.Tenant;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                "myVerySecretKeyForJwtTesting12345678901234567890"
        );
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        jwtService.init();
    }

    @Test
    void generateToken_shouldContainUsernameUserIdTenantIdAndRole() {
        Tenant tenant = Tenant.builder().id(101L).name("Tenant A").build();

        User user = User.builder()
                .id(11L)
                .email("user@test.com")
                .role(Role.USER)
                .tenant(tenant)
                .build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("user@test.com", jwtService.extractUsername(token));
        assertEquals(11L, jwtService.extractUserId(token));
        assertEquals(101L, jwtService.extractTenantId(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenBelongsToUserAndNotExpired() {
        Tenant tenant = Tenant.builder().id(1L).build();
        User user = User.builder()
                .id(5L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();

        String token = jwtService.generateToken(user);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("admin@test.com");

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        Tenant tenant = Tenant.builder().id(1L).build();
        User user = User.builder()
                .id(5L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .tenant(tenant)
                .build();

        String token = jwtService.generateToken(user);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("other@test.com");

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }
}
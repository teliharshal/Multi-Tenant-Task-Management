package com.example.taskmanagement.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Task not found");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Task not found", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleBadRequest_shouldReturn400() {
        BadRequestException ex = new BadRequestException("Bad request");

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad request", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleUnauthorized_shouldReturn401() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized");

        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorized(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("Unauthorized", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleCustomAccessDenied_shouldReturn403() {
        AccessDeniedCustomException ex = new AccessDeniedCustomException("Forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleCustomAccessDenied(ex);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleSpringAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("No access");

        ResponseEntity<Map<String, Object>> response = handler.handleSpringAccessDenied(ex);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Access denied", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() throws Exception {
        DummyRequest dummyRequest = new DummyRequest();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(dummyRequest, "dummyRequest");

        bindingResult.addError(new FieldError("dummyRequest", "email", "Email is required"));
        bindingResult.addError(new FieldError("dummyRequest", "password", "Password is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation failed", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) response.getBody().get("fields");

        assertNotNull(fields);
        assertEquals("Email is required", fields.get("email"));
        assertEquals("Password is required", fields.get("password"));
    }

    @Test
    void handleConstraintViolation_shouldReturn400() {
        ConstraintViolationException ex =
                new ConstraintViolationException("Constraint violation", null);

        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Constraint violation", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleGeneric_shouldReturn500() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Something went wrong", response.getBody().get("error"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<?> response = handler.handleBadCredentials(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(401, body.get("status"));
        assertEquals("Invalid email or password", body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    static class DummyRequest {
        private String email;
        private String password;
    }
}
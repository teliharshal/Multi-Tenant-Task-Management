package com.example.taskmanagement.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @Test
    void tenantContext_shouldSetGetAndClearTenantId() {
        TenantContext.setTenantId(123L);
        assertEquals(123L, TenantContext.getTenantId());

        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }
}
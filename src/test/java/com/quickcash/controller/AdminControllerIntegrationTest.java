package com.quickcash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String ADMIN_API_KEY = "test-admin-api-key";

    @Test
    void list_agents_returns_200_with_api_key() throws Exception {
        mockMvc.perform(get("/api/v1/admin/agents?limit=10")
                        .header("X-Admin-API-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void list_agents_returns_401_or_403_without_api_key() throws Exception {
        mockMvc.perform(get("/api/v1/admin/agents"))
                .andExpect(status().is4xxClientError()); // 401 Unauthorized or 403 Forbidden
    }

    @Test
    void reports_summary_returns_200_with_api_key() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .header("X-Admin-API-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").exists())
                .andExpect(jsonPath("$.to").exists())
                .andExpect(jsonPath("$.cashRequestsCount").exists())
                .andExpect(jsonPath("$.depositsCount").exists());
    }

    @Test
    void reports_export_returns_200_with_api_key() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/export?limit=10")
                        .header("X-Admin-API-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

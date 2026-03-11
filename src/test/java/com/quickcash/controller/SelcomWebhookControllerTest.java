package com.quickcash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SelcomWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void callback_accepts_post_without_auth_and_returns_200() throws Exception {
        mockMvc.perform(post("/api/v1/selcom/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"00\",\"transaction_id\":\"txn-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("received"));
    }

    @Test
    void callback_accepts_with_signature_header() throws Exception {
        mockMvc.perform(post("/api/v1/selcom/callback")
                        .header("X-Selcom-Signature", "dummy-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}

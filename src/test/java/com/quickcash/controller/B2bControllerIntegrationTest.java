package com.quickcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.domain.User;
import com.quickcash.dto.B2bDisbursementItem;
import com.quickcash.dto.B2bDisbursementRequest;
import com.quickcash.repository.UserRepository;
import com.quickcash.selcom.SelcomApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class B2bControllerIntegrationTest {

    private static final String B2B_API_KEY = "test-b2b-api-key";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;

    @MockBean
    SelcomApiClient selcomApiClient;

    UUID businessUserId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .uid("b2b-" + UUID.randomUUID())
                .email("b2b@example.com")
                .displayName("Business")
                .build();
        user = userRepository.save(user);
        businessUserId = user.getId();
    }

    @Test
    void createDisbursement_returns_200_with_api_key() throws Exception {
        B2bDisbursementItem item = new B2bDisbursementItem();
        item.setRecipientPhone("255712345678");
        item.setRecipientName("Worker");
        item.setAmount(new BigDecimal("30000"));
        B2bDisbursementRequest req = new B2bDisbursementRequest();
        req.setBusinessUserId(businessUserId);
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/b2b/disbursements")
                        .header("X-B2B-API-Key", B2B_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void createDisbursement_returns_4xx_without_api_key() throws Exception {
        B2bDisbursementItem item = new B2bDisbursementItem();
        item.setRecipientPhone("255700000000");
        item.setAmount(new BigDecimal("10000"));
        B2bDisbursementRequest req = new B2bDisbursementRequest();
        req.setBusinessUserId(businessUserId);
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/b2b/disbursements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }
}

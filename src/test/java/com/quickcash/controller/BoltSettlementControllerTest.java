package com.quickcash.controller;

import com.quickcash.auth.JwtService;
import com.quickcash.domain.Settlement;
import com.quickcash.domain.User;
import com.quickcash.repository.SettlementRepository;
import com.quickcash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BoltSettlementControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SettlementRepository settlementRepository;

    User user;
    String token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .uid("uid-" + UUID.randomUUID())
                .email("test@example.com")
                .displayName("Test")
                .build();
        user = userRepository.save(user);
        token = jwtService.createToken(user.getId(), user.getUid());
    }

    @Test
    void listPending_returns_empty_when_no_bolt_settlements() throws Exception {
        mockMvc.perform(get("/api/v1/bolt/settlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void exportCsv_returns_csv_with_header() throws Exception {
        mockMvc.perform(get("/api/v1/bolt/settlements/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("bolt-settlements.csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("settlement_id,request_id,bolt_job_id,bolt_payout_amount,created_at")));
    }
}

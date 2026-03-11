package com.quickcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.auth.JwtService;
import com.quickcash.domain.User;
import com.quickcash.dto.DepositRequestCreate;
import com.quickcash.repository.DepositRequestRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DepositControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    DepositRequestRepository depositRequestRepository;

    @MockBean
    SelcomApiClient selcomApiClient;

    User user;
    String token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .uid("test-uid-" + UUID.randomUUID())
                .email("deposit@example.com")
                .displayName("Deposit User")
                .build();
        user = userRepository.save(user);
        token = jwtService.createToken(user.getId(), user.getUid());
        when(selcomApiClient.verifyAccount(anyString(), anyString(), anyString(), any()))
                .thenReturn(SelcomApiClient.SelcomVerifyResult.builder().verified(true).requestId("stub").build());
    }

    @Test
    void create_deposit_returns_200_and_list_includes_it() throws Exception {
        DepositRequestCreate req = new DepositRequestCreate();
        req.setDestinationBankCode("01");
        req.setDestinationAccountNumber("1234567890");
        req.setDestinationAccountName("Test");
        req.setCashAmount(new BigDecimal("50000"));

        mockMvc.perform(post("/api/v1/deposits/request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEARCHING_AGENT"))
                .andExpect(jsonPath("$.destinationBankCode").value("01"))
                .andExpect(jsonPath("$.cashAmount").value(50000));

        mockMvc.perform(get("/api/v1/deposits?limit=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void list_deposits_requires_auth() throws Exception {
        mockMvc.perform(get("/api/v1/deposits"))
                .andExpect(status().is4xxClientError()); // 401 or 403 when not authenticated
    }
}

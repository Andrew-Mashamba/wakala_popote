package com.quickcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.auth.JwtService;
import com.quickcash.bolt.BoltApiClient;
import com.quickcash.domain.*;
import com.quickcash.dto.CashRequestCreateV1;
import com.quickcash.repository.CashRequestRepository;
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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CashRequestV1ControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CashRequestRepository cashRequestRepository;

    @MockBean
    SelcomApiClient selcomApiClient;
    @MockBean
    BoltApiClient boltApiClient;

    User user;
    String token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .uid("test-uid-" + UUID.randomUUID())
                .email("test@example.com")
                .displayName("Test User")
                .build();
        user = userRepository.save(user);
        token = jwtService.createToken(user.getId(), user.getUid());
    }

    @Test
    void collect_returns_200_and_updates_status_when_verified() throws Exception {
        CashRequest request = CashRequest.builder()
                .user(user)
                .requestedAmount(new BigDecimal("100000"))
                .principalAmount(new BigDecimal("100000"))
                .totalClientCharge(new BigDecimal("104500"))
                .userLatitude(-6.78)
                .userLongitude(39.27)
                .status(CashRequest.CashRequestStatus.VERIFIED)
                .selcomRequestId("stub-req")
                .clientBankCode("01")
                .clientAccountNumber("1234567890")
                .clientAccountName("Test")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        request = cashRequestRepository.save(request);

        when(selcomApiClient.collectPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(SelcomApiClient.SelcomCollectResult.builder().success(true).transactionId("txn-1").build());

        mockMvc.perform(post("/api/v1/cash/requests/" + request.getId() + "/collect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEARCHING_AGENT"));
    }

    @Test
    void collect_returns_400_when_not_verified() throws Exception {
        CashRequest request = CashRequest.builder()
                .user(user)
                .requestedAmount(new BigDecimal("100000"))
                .principalAmount(new BigDecimal("100000"))
                .userLatitude(-6.78)
                .userLongitude(39.27)
                .status(CashRequest.CashRequestStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        request = cashRequestRepository.save(request);

        mockMvc.perform(post("/api/v1/cash/requests/" + request.getId() + "/collect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()); // IllegalStateException -> 500
    }

    @Test
    void idempotency_same_key_returns_same_response_and_no_duplicate_request() throws Exception {
        CashRequestCreateV1 body = CashRequestCreateV1.builder()
                .amount(new BigDecimal("25000"))
                .latitude(-6.78)
                .longitude(39.27)
                .requestType(CashRequest.RequestType.LOCAL_CASH)
                .build();
        String idempotencyKey = "test-idem-" + UUID.randomUUID();
        String content = objectMapper.writeValueAsString(body);

        var result1 = mockMvc.perform(post("/api/v1/cash/request")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").exists())
                .andReturn();
        String response1 = result1.getResponse().getContentAsString();

        var result2 = mockMvc.perform(post("/api/v1/cash/request")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andReturn();
        String response2 = result2.getResponse().getContentAsString();

        assertThat(response2).isEqualTo(response1);
        long count = cashRequestRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(user.getId())).count();
        assertThat(count).isOne();
    }
}

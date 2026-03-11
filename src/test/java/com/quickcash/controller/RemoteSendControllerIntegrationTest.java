package com.quickcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.auth.JwtService;
import com.quickcash.domain.User;
import com.quickcash.dto.SendCashRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RemoteSendControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;

    @MockBean
    SelcomApiClient selcomApiClient;

    User user;
    String token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .uid("test-uid-" + UUID.randomUUID())
                .email("send@example.com")
                .displayName("Sender")
                .build();
        user = userRepository.save(user);
        token = jwtService.createToken(user.getId(), user.getUid());
    }

    @Test
    void send_returns_200_and_remote_send_status() throws Exception {
        SendCashRequest req = new SendCashRequest();
        req.setRecipientPhone("255712345678");
        req.setRecipientName("Recipient");
        req.setAmount(new BigDecimal("50000"));
        req.setDeliveryLatitude(-6.78);
        req.setDeliveryLongitude(39.27);

        mockMvc.perform(post("/api/v1/cash/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestType").value("REMOTE_SEND"))
                .andExpect(jsonPath("$.status").exists());
    }
}

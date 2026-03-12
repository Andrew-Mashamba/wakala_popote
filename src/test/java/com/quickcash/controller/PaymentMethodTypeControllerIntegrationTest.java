package com.quickcash.controller;

import com.quickcash.auth.JwtService;
import com.quickcash.domain.User;
import com.quickcash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentMethodTypeControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;

    User user;
    String token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .uid("test-uid-" + UUID.randomUUID())
                .email("types@example.com")
                .displayName("Types User")
                .build();
        user = userRepository.save(user);
        token = jwtService.createToken(user.getId(), user.getUid());
    }

    @Test
    void getTypes_returns_200_with_categories() throws Exception {
        mockMvc.perform(get("/api/v1/payment-method-types")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[0].id").value("MOBILE_WALLET"))
                .andExpect(jsonPath("$.categories[0].label").value("Mobile Wallet"))
                .andExpect(jsonPath("$.categories[0].subTypes").isArray())
                .andExpect(jsonPath("$.categories[0].subTypes[0].id").value("M_PESA"))
                .andExpect(jsonPath("$.categories[0].subTypes[0].label").value("M-Pesa"))
                .andExpect(jsonPath("$.categories[1].id").value("BANK_ACCOUNT"))
                .andExpect(jsonPath("$.categories[2].id").value("CARD"));
    }

    @Test
    void getTypes_without_auth_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/payment-method-types")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

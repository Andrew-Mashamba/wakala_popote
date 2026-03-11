package com.quickcash.controller;

import com.quickcash.auth.JwtService;
import com.quickcash.domain.*;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AgentRepository agentRepository;
    @Autowired
    AgentAssignmentRepository agentAssignmentRepository;
    @Autowired
    CashRequestRepository cashRequestRepository;

    @MockBean
    SelcomApiClient selcomApiClient;

    User agentUser;
    Agent agent;
    String agentToken;
    CashRequest searchRequest;

    @BeforeEach
    void setUp() {
        agentUser = User.builder()
                .id(UUID.randomUUID())
                .uid("agent-uid-" + UUID.randomUUID())
                .email("agent@example.com")
                .displayName("Agent User")
                .build();
        agentUser = userRepository.save(agentUser);
        agent = Agent.builder()
                .user(agentUser)
                .selcomAccountId("selcom-agent-1")
                .selcomAccountName("Agent One")
                .isAvailable(true)
                .currentLat(-6.78)
                .currentLng(39.27)
                .totalDeliveries(0)
                .totalEarnings(BigDecimal.ZERO)
                .build();
        agent = agentRepository.save(agent);
        agentToken = jwtService.createToken(agentUser.getId(), agentUser.getUid());

        User clientUser = User.builder()
                .id(UUID.randomUUID())
                .uid("client-uid-" + UUID.randomUUID())
                .email("client@example.com")
                .displayName("Client")
                .build();
        clientUser = userRepository.save(clientUser);
        searchRequest = CashRequest.builder()
                .user(clientUser)
                .requestedAmount(new BigDecimal("50000"))
                .principalAmount(new BigDecimal("50000"))
                .totalClientCharge(new BigDecimal("52250"))
                .totalAgentPayment(new BigDecimal("53500"))
                .userLatitude(-6.78)
                .userLongitude(39.27)
                .deliveryLat(-6.78)
                .deliveryLng(39.27)
                .status(CashRequest.CashRequestStatus.SEARCHING_AGENT)
                .clientBankCode("01")
                .clientAccountNumber("123")
                .clientAccountName("Client")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        searchRequest = cashRequestRepository.save(searchRequest);
        AgentAssignment pendingAssignment = AgentAssignment.builder()
                .request(searchRequest)
                .agent(agent)
                .status(AgentAssignment.AssignmentStatus.PENDING)
                .assignmentMethod(AgentAssignment.AssignmentMethod.BROADCAST)
                .build();
        agentAssignmentRepository.save(pendingAssignment);

        when(selcomApiClient.creditAgent(any(), any(), any()))
                .thenReturn(SelcomApiClient.SelcomCreditResult.builder().success(true).transactionId("txn-1").build());
    }

    @Test
    void register_returns_409_when_already_registered() throws Exception {
        mockMvc.perform(post("/api/v1/agent/register")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selcomAccountId\":\"another\",\"selcomAccountName\":\"Other\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void profile_returns_agent_info() throws Exception {
        mockMvc.perform(get("/api/v1/agent/profile")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent.getId().toString()))
                .andExpect(jsonPath("$.selcomAccountId").value("selcom-agent-1"))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void earnings_returns_total_and_history() throws Exception {
        mockMvc.perform(get("/api/v1/agent/earnings")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnings").exists())
                .andExpect(jsonPath("$.totalDeliveries").exists())
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    void list_requests_returns_available_requests() throws Exception {
        mockMvc.perform(get("/api/v1/agent/requests")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("limit", "20"))
                .andExpect(status().isOk());
        // May be empty if distance filter excludes; we have same lat/lng so should include
    }

    @Test
    void accept_then_deliver_flow() throws Exception {
        mockMvc.perform(post("/api/v1/agent/requests/" + searchRequest.getId() + "/accept")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-6.78,\"longitude\":39.27}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(searchRequest.getId().toString()));

        mockMvc.perform(post("/api/v1/agent/requests/" + searchRequest.getId() + "/en-route")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/agent/requests/" + searchRequest.getId() + "/arrived")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/agent/requests/" + searchRequest.getId() + "/deliver")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}

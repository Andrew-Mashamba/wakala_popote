package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    AgentRepository agentRepository;
    @Mock
    AgentAssignmentRepository agentAssignmentRepository;
    @Mock
    UserService userService;
    @Mock
    CashRequestRepository cashRequestRepository;
    @Mock
    SettlementService settlementService;
    @Mock
    SelcomApiClient selcomApiClient;
    @Mock
    FcmNotificationService fcmNotificationService;

    @InjectMocks
    AgentService agentService;

    @Test
    void register_creates_agent() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDisplayName("Agent One");
        when(userService.getById(userId.toString())).thenReturn(user);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(i -> {
            Agent a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            return a;
        });

        Agent agent = agentService.register(userId, "selcom-123", "Agent One");

        assertThat(agent.getSelcomAccountId()).isEqualTo("selcom-123");
        assertThat(agent.getUser()).isSameAs(user);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void register_throws_when_already_agent() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userService.getById(userId.toString())).thenReturn(user);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(Agent.builder().build()));

        assertThatThrownBy(() -> agentService.register(userId, "selcom-123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void accept_upgrades_pending_assignment_and_cancels_others() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Agent agent = Agent.builder().id(UUID.randomUUID()).user(user).selcomAccountId("s1").build();
        CashRequest request = new CashRequest();
        request.setId(requestId);
        request.setStatus(CashRequest.CashRequestStatus.SEARCHING_AGENT);
        AgentAssignment pendingAssignment = AgentAssignment.builder()
                .id(UUID.randomUUID())
                .request(request)
                .agent(agent)
                .status(AgentAssignment.AssignmentStatus.PENDING)
                .build();

        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(cashRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(agentAssignmentRepository.findByRequestAndAgentAndStatus(request, agent, AgentAssignment.AssignmentStatus.PENDING))
                .thenReturn(Optional.of(pendingAssignment));
        when(agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.ACCEPTED)).thenReturn(Optional.empty());
        when(agentAssignmentRepository.findByRequestAndStatusIn(request, List.of(AgentAssignment.AssignmentStatus.PENDING)))
                .thenReturn(List.of(pendingAssignment));
        when(agentAssignmentRepository.save(any(AgentAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> i.getArgument(0));

        AgentAssignment assignment = agentService.accept(userId, requestId, -6.78, 39.27);

        assertThat(assignment.getStatus()).isEqualTo(AgentAssignment.AssignmentStatus.ACCEPTED);
        assertThat(assignment.getAgent()).isSameAs(agent);
        assertThat(assignment.getRequest()).isSameAs(request);
        assertThat(request.getStatus()).isEqualTo(CashRequest.CashRequestStatus.AGENT_ASSIGNED);
        verify(agentAssignmentRepository, atLeast(1)).save(any(AgentAssignment.class));
        verify(cashRequestRepository).save(request);
    }

    @Test
    void deliver_creates_settlement_and_credits_agent() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Agent agent = Agent.builder().id(UUID.randomUUID()).user(user).selcomAccountId("agent-selcom").build();
        CashRequest request = new CashRequest();
        request.setId(requestId);
        request.setTotalClientCharge(new BigDecimal("104500"));
        request.setTotalAgentPayment(new BigDecimal("103500"));
        request.setClientBankCode("01");
        request.setClientAccountNumber("123");
        request.setClientAccountName("Client");
        AgentAssignment assignment = AgentAssignment.builder()
                .id(assignmentId)
                .request(request)
                .agent(agent)
                .status(AgentAssignment.AssignmentStatus.ARRIVED)
                .build();
        Settlement settlement = Settlement.builder().id(UUID.randomUUID()).build();

        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(cashRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.ARRIVED))
                .thenReturn(Optional.of(assignment));
        when(settlementService.createOnDelivery(any(), any(), any(), any(), any(), any(), any())).thenReturn(settlement);
        when(selcomApiClient.creditAgent(any(), any(), any()))
                .thenReturn(SelcomApiClient.SelcomCreditResult.builder().success(true).transactionId("txn-1").build());
        when(agentAssignmentRepository.save(any(AgentAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(agentRepository.save(any(Agent.class))).thenAnswer(i -> i.getArgument(0));

        agentService.deliver(userId, requestId);

        verify(settlementService).createOnDelivery(eq(request), eq(assignment), eq("01"), eq("123"), eq(new BigDecimal("104500")), eq("agent-selcom"), eq(new BigDecimal("103500")));
        verify(selcomApiClient).creditAgent(eq("agent-selcom"), eq(new BigDecimal("103500")), startsWith("QC-"));
        verify(settlementService).markAgentCredited(eq(settlement.getId()), eq("txn-1"));
        assertThat(request.getStatus()).isEqualTo(CashRequest.CashRequestStatus.SETTLED);
    }

    @Test
    void listEarningsHistory_returns_delivered_assignments_only() {
        UUID userId = UUID.randomUUID();
        Agent agent = Agent.builder().id(UUID.randomUUID()).build();
        CashRequest req = new CashRequest();
        req.setId(UUID.randomUUID());
        AgentAssignment delivered = AgentAssignment.builder()
                .id(UUID.randomUUID())
                .request(req)
                .agent(agent)
                .status(AgentAssignment.AssignmentStatus.DELIVERED)
                .settlementAmount(new BigDecimal("51000"))
                .deliveryConfirmedAt(Instant.now())
                .build();
        AgentAssignment pending = AgentAssignment.builder()
                .id(UUID.randomUUID())
                .request(req)
                .agent(agent)
                .status(AgentAssignment.AssignmentStatus.ACCEPTED)
                .build();

        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(agentAssignmentRepository.findByAgentOrderByAssignedAtDesc(eq(agent), any()))
                .thenReturn(List.of(pending, delivered));

        var history = agentService.listEarningsHistory(userId, 10);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getRequestId()).isEqualTo(req.getId());
        assertThat(history.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("51000"));
    }
}

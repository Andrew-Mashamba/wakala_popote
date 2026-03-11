package com.quickcash.service;

import com.quickcash.domain.AdminFlag;
import com.quickcash.domain.Agent;
import com.quickcash.domain.User;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.AdminFlagRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.repository.DepositRequestRepository;
import com.quickcash.repository.SelcomAccountApplicationRepository;
import com.quickcash.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    CashRequestRepository cashRequestRepository;
    @Mock
    DepositRequestRepository depositRequestRepository;
    @Mock
    SettlementRepository settlementRepository;
    @Mock
    AgentRepository agentRepository;
    @Mock
    SelcomAccountApplicationRepository applicationRepository;
    @Mock
    AdminFlagRepository adminFlagRepository;
    @Mock
    KycApplicationService kycApplicationService;
    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    AdminService adminService;

    @Test
    void verifyAgent_sets_tier_to_verified() {
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder()
                .id(agentId)
                .user(new User())
                .selcomAccountId("s1")
                .agentTier(Agent.AgentTier.NEW)
                .build();
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(i -> i.getArgument(0));

        Agent result = adminService.verifyAgent(agentId);

        assertThat(result.getAgentTier()).isEqualTo(Agent.AgentTier.VERIFIED);
        verify(agentRepository).save(agent);
    }

    @Test
    void suspendAgent_sets_suspended_and_unavailable() {
        UUID agentId = UUID.randomUUID();
        Agent agent = Agent.builder()
                .id(agentId)
                .user(new User())
                .selcomAccountId("s1")
                .selcomAccountStatus(Agent.SelcomAccountStatus.ACTIVE)
                .isAvailable(true)
                .build();
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(i -> i.getArgument(0));

        Agent result = adminService.suspendAgent(agentId);

        assertThat(result.getSelcomAccountStatus()).isEqualTo(Agent.SelcomAccountStatus.SUSPENDED);
        assertThat(result.getIsAvailable()).isFalse();
        verify(agentRepository).save(agent);
    }

    @Test
    void getAgent_throws_when_not_found() {
        UUID agentId = UUID.randomUUID();
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getAgent(agentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listComplianceFlags_returns_unresolved_only() {
        AdminFlag flag = AdminFlag.builder()
                .id(UUID.randomUUID())
                .flagType(AdminFlag.FlagType.COMPLIANCE)
                .entityType("AGENT")
                .entityId(UUID.randomUUID().toString())
                .reason("PEP check")
                .resolved(false)
                .build();
        when(adminFlagRepository.findByFlagTypeAndResolvedFalseOrderByCreatedAtDesc(AdminFlag.FlagType.COMPLIANCE))
                .thenReturn(List.of(flag));

        List<AdminFlag> result = adminService.listComplianceFlags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlagType()).isEqualTo(AdminFlag.FlagType.COMPLIANCE);
        assertThat(result.get(0).getResolved()).isFalse();
    }

    @Test
    void clearComplianceFlag_sets_resolved() {
        UUID flagId = UUID.randomUUID();
        AdminFlag flag = AdminFlag.builder()
                .id(flagId)
                .flagType(AdminFlag.FlagType.COMPLIANCE)
                .resolved(false)
                .build();
        when(adminFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(adminFlagRepository.save(any(AdminFlag.class))).thenAnswer(i -> i.getArgument(0));

        adminService.clearComplianceFlag(flagId);

        verify(adminFlagRepository).save(flag);
        assertThat(flag.getResolved()).isTrue();
    }
}

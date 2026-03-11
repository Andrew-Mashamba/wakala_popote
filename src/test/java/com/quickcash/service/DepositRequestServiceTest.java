package com.quickcash.service;

import com.quickcash.domain.Agent;
import com.quickcash.domain.DepositRequest;
import com.quickcash.domain.User;
import com.quickcash.dto.DepositRequestCreate;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.DepositRequestRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositRequestServiceTest {

    @Mock
    DepositRequestRepository depositRequestRepository;
    @Mock
    UserService userService;
    @Mock
    FeeCalculationService feeCalculationService;
    @Mock
    SelcomApiClient selcomApiClient;
    @Mock
    AgentRepository agentRepository;
    @Mock
    FcmNotificationService fcmNotificationService;

    @InjectMocks
    DepositRequestService depositRequestService;

    @Test
    void create_verifies_account_and_saves_with_searching_agent_when_verified() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFcmToken(null);
        when(userService.getById(userId.toString())).thenReturn(user);

        DepositRequestCreate req = new DepositRequestCreate();
        req.setDestinationBankCode("01");
        req.setDestinationAccountNumber("1234567890");
        req.setDestinationAccountName("Test");
        req.setCashAmount(new BigDecimal("100000"));

        var breakdown = FeeCalculationService.DepositFeeBreakdown.builder()
                .cashAmount(new BigDecimal("100000"))
                .serviceFee(new BigDecimal("500"))
                .agentCommission(new BigDecimal("350"))
                .platformMargin(new BigDecimal("150"))
                .netDepositAmount(new BigDecimal("99500"))
                .build();
        when(feeCalculationService.calculateDeposit(req.getCashAmount())).thenReturn(breakdown);
        when(selcomApiClient.verifyAccount(eq("01"), eq("1234567890"), any(), any()))
                .thenReturn(SelcomApiClient.SelcomVerifyResult.builder().verified(true).requestId("req-1").build());
        when(depositRequestRepository.save(any(DepositRequest.class))).thenAnswer(i -> {
            DepositRequest d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(Instant.now());
            return d;
        });

        DepositRequest result = depositRequestService.create(userId, req);

        assertThat(result.getStatus()).isEqualTo(DepositRequest.DepositStatus.SEARCHING_AGENT);
        assertThat(result.getDestinationBankCode()).isEqualTo("01");
        assertThat(result.getNetDepositAmount()).isEqualByComparingTo("99500");
        verify(depositRequestRepository, atLeastOnce()).save(any(DepositRequest.class));
    }

    @Test
    void cancel_sets_cancelled_and_saves() {
        UUID depositId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        DepositRequest d = DepositRequest.builder()
                .id(depositId)
                .clientUser(user)
                .status(DepositRequest.DepositStatus.SEARCHING_AGENT)
                .build();
        when(depositRequestRepository.findById(depositId)).thenReturn(Optional.of(d));
        when(depositRequestRepository.save(any(DepositRequest.class))).thenAnswer(i -> i.getArgument(0));

        DepositRequest result = depositRequestService.cancel(depositId, userId, "Changed mind");

        assertThat(result.getStatus()).isEqualTo(DepositRequest.DepositStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Changed mind");
        verify(depositRequestRepository).save(d);
    }

    @Test
    void getByIdAndClient_throws_when_wrong_client() {
        UUID depositId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        DepositRequest d = DepositRequest.builder()
                .id(depositId)
                .clientUser(otherUser)
                .build();
        when(depositRequestRepository.findById(depositId)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> depositRequestService.getByIdAndClient(depositId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agentAccept_assigns_agent_and_updates_status() {
        UUID depositId = UUID.randomUUID();
        UUID agentUserId = UUID.randomUUID();
        User clientUser = new User();
        clientUser.setId(UUID.randomUUID());
        clientUser.setFcmToken(null);
        Agent agent = Agent.builder()
                .id(UUID.randomUUID())
                .selcomAccountId("selcom-1")
                .user(new User())
                .build();
        DepositRequest d = DepositRequest.builder()
                .id(depositId)
                .clientUser(clientUser)
                .status(DepositRequest.DepositStatus.SEARCHING_AGENT)
                .build();
        when(depositRequestRepository.findById(depositId)).thenReturn(Optional.of(d));
        when(agentRepository.findByUserId(agentUserId)).thenReturn(Optional.of(agent));
        when(depositRequestRepository.save(any(DepositRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(userService.getById(clientUser.getId().toString())).thenReturn(clientUser);

        DepositRequest result = depositRequestService.agentAccept(depositId, agentUserId);

        assertThat(result.getStatus()).isEqualTo(DepositRequest.DepositStatus.AGENT_ASSIGNED);
        assertThat(result.getAssignedAgent()).isSameAs(agent);
        verify(depositRequestRepository).save(d);
    }
}

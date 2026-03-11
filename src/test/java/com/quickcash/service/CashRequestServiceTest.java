package com.quickcash.service;

import com.quickcash.bolt.BoltApiClient;
import com.quickcash.domain.*;
import com.quickcash.dto.CashRequestCreateV1;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CashRequestServiceTest {

    @Mock
    CashRequestRepository cashRequestRepository;
    @Mock
    UserService userService;
    @Mock
    FeeCalculationService feeCalculationService;
    @Mock
    PaymentMethodService paymentMethodService;
    @Mock
    SelcomApiClient selcomApiClient;
    @Mock
    AgentRepository agentRepository;
    @Mock
    AgentAssignmentRepository agentAssignmentRepository;
    @Mock
    FcmNotificationService fcmNotificationService;
    @Mock
    BoltApiClient boltApiClient;
    @Mock
    com.quickcash.sms.SmsOtpService smsOtpService;
    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    CashRequestService cashRequestService;

    @Test
    void createRequestV1_without_payment_method_saves_pending_verification() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        CashRequestCreateV1 dto = new CashRequestCreateV1();
        dto.setAmount(new BigDecimal("100000"));
        dto.setLatitude(-6.78);
        dto.setLongitude(39.27);
        dto.setRequestType(CashRequest.RequestType.LOCAL_CASH);

        FeeCalculationService.FeeBreakdown fees = FeeCalculationService.FeeBreakdown.builder()
                .principalAmount(new BigDecimal("100000"))
                .serviceFee(new BigDecimal("3000"))
                .transportFee(new BigDecimal("1500"))
                .agentFee(new BigDecimal("2000"))
                .totalClientCharge(new BigDecimal("104500"))
                .totalAgentPayment(new BigDecimal("103500"))
                .build();

        when(userService.getById(userId.toString())).thenReturn(user);
        when(feeCalculationService.calculate(dto.getAmount())).thenReturn(fees);
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> {
            CashRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            r.setUpdatedAt(Instant.now());
            return r;
        });

        CashRequest result = cashRequestService.createRequestV1(userId, dto);

        assertThat(result.getStatus()).isEqualTo(CashRequest.CashRequestStatus.PENDING_VERIFICATION);
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(result.getTotalClientCharge()).isEqualByComparingTo(new BigDecimal("104500"));
        verify(cashRequestRepository, atLeast(1)).save(any(CashRequest.class));
    }

    @Test
    void collectPayment_throws_when_not_verified() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        CashRequest request = new CashRequest();
        request.setId(requestId);
        request.setUser(user);
        request.setStatus(CashRequest.CashRequestStatus.PENDING_VERIFICATION);

        when(cashRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> cashRequestService.collectPayment(requestId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VERIFIED");
    }

    @Test
    void collectPayment_moves_to_searching_agent_and_notifies_when_selcom_success() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUid("uid-" + userId);
        CashRequest request = new CashRequest();
        request.setId(requestId);
        request.setUser(user);
        request.setStatus(CashRequest.CashRequestStatus.VERIFIED);
        request.setRequestedAmount(new BigDecimal("100000"));
        request.setPrincipalAmount(new BigDecimal("100000"));
        request.setSelcomRequestId("selcom-req-1");
        request.setClientBankCode("01");
        request.setClientAccountNumber("1234567890");
        request.setClientAccountName("Test");
        request.setTotalClientCharge(new BigDecimal("104500"));

        when(cashRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(selcomApiClient.collectPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(SelcomApiClient.SelcomCollectResult.builder().success(true).transactionId("txn-1").build());
        when(agentRepository.findByIsAvailableTrue()).thenReturn(List.of()); // no agents -> no PENDING assignments created
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> i.getArgument(0));

        CashRequest result = cashRequestService.collectPayment(requestId, userId);

        assertThat(result.getStatus()).isEqualTo(CashRequest.CashRequestStatus.SEARCHING_AGENT);
        verify(selcomApiClient).collectPayment(eq("selcom-req-1"), eq("01"), eq("1234567890"), eq("Test"), eq(new BigDecimal("104500")), startsWith("QC-"));
        verify(cashRequestRepository).save(request);
    }
}

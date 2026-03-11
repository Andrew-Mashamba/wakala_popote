package com.quickcash.service;

import com.quickcash.domain.CashRequest;
import com.quickcash.domain.User;
import com.quickcash.dto.SendCashRequest;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import com.quickcash.bolt.BoltApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteSendServiceTest {

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
    void createSendRequest_sets_recipient_and_otp() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userService.getById(userId.toString())).thenReturn(user);

        SendCashRequest req = new SendCashRequest();
        req.setRecipientPhone("255712345678");
        req.setRecipientName("Recipient");
        req.setAmount(new BigDecimal("50000"));
        req.setDeliveryLatitude(-6.78);
        req.setDeliveryLongitude(39.27);

        var fees = com.quickcash.service.FeeCalculationService.FeeBreakdown.builder()
                .principalAmount(new BigDecimal("50000"))
                .serviceFee(new BigDecimal("1500"))
                .transportFee(new BigDecimal("1500"))
                .agentFee(new BigDecimal("2000"))
                .totalClientCharge(new BigDecimal("53000"))
                .totalAgentPayment(new BigDecimal("53500"))
                .build();
        when(feeCalculationService.calculate(req.getAmount())).thenReturn(fees);
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> {
            CashRequest c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CashRequest result = cashRequestService.createSendRequest(userId, req);

        assertThat(result.getRequestType()).isEqualTo(CashRequest.RequestType.REMOTE_SEND);
        assertThat(result.getRecipientPhone()).isEqualTo("255712345678");
        assertThat(result.getRecipientOtp()).isNotNull();
        assertThat(result.getRecipientOtp()).hasSize(6);
        assertThat(result.getRecipientOtpVerified()).isFalse();
        verify(cashRequestRepository, atLeastOnce()).save(any(CashRequest.class));
    }

    @Test
    void verifyRecipientOtp_returns_true_when_valid() {
        UUID requestId = UUID.randomUUID();
        CashRequest request = CashRequest.builder()
                .id(requestId)
                .requestType(CashRequest.RequestType.REMOTE_SEND)
                .recipientOtp("123456")
                .recipientOtpExpiresAt(Instant.now().plusSeconds(300))
                .recipientOtpVerified(false)
                .build();
        when(cashRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(cashRequestRepository.save(any(CashRequest.class))).thenAnswer(i -> i.getArgument(0));

        boolean valid = cashRequestService.verifyRecipientOtp(requestId, "123456");

        assertThat(valid).isTrue();
        assertThat(request.getRecipientOtpVerified()).isTrue();
        verify(cashRequestRepository).save(request);
    }
}

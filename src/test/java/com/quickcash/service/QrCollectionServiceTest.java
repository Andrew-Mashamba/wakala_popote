package com.quickcash.service;

import com.quickcash.domain.Agent;
import com.quickcash.domain.QrCollectionRequest;
import com.quickcash.domain.User;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.repository.QrCollectionRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrCollectionServiceTest {

    @Mock
    QrCollectionRequestRepository qrRepository;
    @Mock
    CashRequestRepository cashRequestRepository;
    @Mock
    AgentAssignmentRepository agentAssignmentRepository;
    @Mock
    AgentRepository agentRepository;
    @Mock
    UserService userService;
    @Mock
    FeeCalculationService feeCalculationService;
    @Mock
    PaymentMethodService paymentMethodService;
    @Mock
    SelcomApiClient selcomApiClient;
    @Mock
    SettlementService settlementService;

    @InjectMocks
    QrCollectionService qrCollectionService;

    @Test
    void generate_creates_qr_with_token_and_expiry() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setLatitude(-6.78);
        user.setLongitude(39.27);
        when(userService.getById(userId.toString())).thenReturn(user);

        var fees = FeeCalculationService.FeeBreakdown.builder()
                .principalAmount(new BigDecimal("100000"))
                .serviceFee(new BigDecimal("3000"))
                .transportFee(BigDecimal.ZERO)
                .agentFee(new BigDecimal("2000"))
                .totalClientCharge(new BigDecimal("103000"))
                .totalAgentPayment(new BigDecimal("105000"))
                .build();
        when(feeCalculationService.calculate(any())).thenReturn(fees);
        when(cashRequestRepository.save(any())).thenAnswer(i -> {
            var r = i.getArgument(0, com.quickcash.domain.CashRequest.class);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(qrRepository.save(any(QrCollectionRequest.class))).thenAnswer(i -> {
            QrCollectionRequest q = i.getArgument(0);
            q.setId(UUID.randomUUID());
            q.setCreatedAt(Instant.now());
            return q;
        });

        QrCollectionRequest result = qrCollectionService.generate(userId, new BigDecimal("100000"), null, false);

        assertThat(result.getQrToken()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getStatus()).isEqualTo(QrCollectionRequest.QrStatus.PENDING);
        verify(qrRepository).save(any(QrCollectionRequest.class));
    }

    @Test
    void scanByToken_throws_when_token_not_found() {
        when(qrRepository.findByQrToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCollectionService.scanByToken("unknown", UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.quickcash.service;

import com.quickcash.domain.CashRequest;
import com.quickcash.domain.DepositRequest;
import com.quickcash.domain.Settlement;
import com.quickcash.domain.User;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.repository.DepositRequestRepository;
import com.quickcash.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    CashRequestRepository cashRequestRepository;
    @Mock
    DepositRequestRepository depositRequestRepository;
    @Mock
    SettlementRepository settlementRepository;

    @InjectMocks
    ReportService reportService;

    @Test
    void getSummary_returns_counts_and_amounts_for_date_range() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        User u = new User();
        u.setId(UUID.randomUUID());
        CashRequest r = CashRequest.builder()
                .id(UUID.randomUUID())
                .user(u)
                .requestedAmount(new BigDecimal("100000"))
                .totalClientCharge(new BigDecimal("104500"))
                .status(CashRequest.CashRequestStatus.DELIVERED)
                .createdAt(Instant.now().minusSeconds(1800))
                .build();
        DepositRequest d = DepositRequest.builder()
                .id(UUID.randomUUID())
                .clientUser(u)
                .destinationBankCode("01")
                .destinationAccountNumber("123")
                .cashAmount(new BigDecimal("50000"))
                .status(DepositRequest.DepositStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(1800))
                .build();
        when(cashRequestRepository.findAll()).thenReturn(List.of(r));
        when(depositRequestRepository.findAll()).thenReturn(List.of(d));
        when(settlementRepository.findAll()).thenReturn(List.of());

        var summary = reportService.getSummary(from, to);

        assertThat(summary).containsEntry("cashRequestsCount", 1);
        assertThat(summary).containsEntry("deliveredCount", 1L);
        assertThat(summary).containsEntry("depositsCount", 1);
        assertThat(summary).containsEntry("completedDepositsCount", 1L);
        assertThat(summary.get("cashRequestsTotalAmount")).isEqualTo(new BigDecimal("104500"));
        assertThat(summary.get("depositsTotalAmount")).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    void exportRequests_returns_list_of_maps() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        User u = new User();
        u.setId(UUID.randomUUID());
        Instant midRange = Instant.now().minusSeconds(1800);
        CashRequest r = CashRequest.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .user(u)
                .requestedAmount(new BigDecimal("50000"))
                .status(CashRequest.CashRequestStatus.PENDING_VERIFICATION)
                .createdAt(midRange)
                .build();
        when(cashRequestRepository.findAll()).thenReturn(List.of(r));

        List<java.util.Map<String, String>> rows = reportService.exportRequests(from, to, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("id", "11111111-1111-1111-1111-111111111111");
        assertThat(rows.get(0)).containsEntry("status", "PENDING_VERIFICATION");
        assertThat(rows.get(0)).containsEntry("amount", "50000");
    }
}

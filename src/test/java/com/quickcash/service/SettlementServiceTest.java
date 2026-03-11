package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    SettlementRepository settlementRepository;

    @InjectMocks
    SettlementService settlementService;

    @Test
    void createOnDelivery_saves_settlement_with_correct_fields() {
        CashRequest request = new CashRequest();
        request.setId(UUID.randomUUID());
        AgentAssignment assignment = new AgentAssignment();
        assignment.setId(UUID.randomUUID());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArgument(0));

        Settlement s = settlementService.createOnDelivery(
                request, assignment,
                "01", "1234567890", new BigDecimal("104500"),
                "agent-selcom-1", new BigDecimal("103500"));

        assertThat(s.getRequest()).isSameAs(request);
        assertThat(s.getAssignment()).isSameAs(assignment);
        assertThat(s.getClientBankCode()).isEqualTo("01");
        assertThat(s.getClientAccountNumber()).isEqualTo("1234567890");
        assertThat(s.getClientDebitAmount()).isEqualByComparingTo(new BigDecimal("104500"));
        assertThat(s.getClientDebitStatus()).isEqualTo(Settlement.ClientDebitStatus.SETTLED);
        assertThat(s.getAgentSelcomAccountId()).isEqualTo("agent-selcom-1");
        assertThat(s.getAgentCreditAmount()).isEqualByComparingTo(new BigDecimal("103500"));
        assertThat(s.getAgentCreditStatus()).isEqualTo(Settlement.AgentCreditStatus.PENDING);
        verify(settlementRepository).save(s);
    }

    @Test
    void markAgentCredited_updates_settlement_when_found() {
        UUID id = UUID.randomUUID();
        Settlement settlement = Settlement.builder().id(id).agentCreditStatus(Settlement.AgentCreditStatus.PENDING).build();
        when(settlementRepository.findById(id)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArgument(0));

        settlementService.markAgentCredited(id, "txn-123");

        assertThat(settlement.getAgentCreditStatus()).isEqualTo(Settlement.AgentCreditStatus.CREDITED);
        assertThat(settlement.getAgentCreditReference()).isEqualTo("txn-123");
        assertThat(settlement.getAgentCreditedAt()).isNotNull();
        verify(settlementRepository).save(settlement);
    }

    @Test
    void markAgentCreditFailed_updates_settlement_when_found() {
        UUID id = UUID.randomUUID();
        Settlement settlement = Settlement.builder().id(id).build();
        when(settlementRepository.findById(id)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArgument(0));

        settlementService.markAgentCreditFailed(id, "Insufficient funds");

        assertThat(settlement.getAgentCreditStatus()).isEqualTo(Settlement.AgentCreditStatus.FAILED);
        assertThat(settlement.getAgentCreditError()).isEqualTo("Insufficient funds");
        verify(settlementRepository).save(settlement);
    }

    @Test
    void listBoltSettlementsPending_returns_only_bolt_pending() {
        CashRequest req = new CashRequest();
        req.setId(UUID.randomUUID());
        Settlement s = Settlement.builder()
                .id(UUID.randomUUID())
                .request(req)
                .settlementType(Settlement.SettlementType.BOLT)
                .boltJobId("bolt-1")
                .boltPayoutAmount(new BigDecimal("5000"))
                .boltSettlementStatus(Settlement.BoltSettlementStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        when(settlementRepository.findBySettlementTypeAndBoltSettlementStatusOrderByCreatedAtAsc(
                eq(Settlement.SettlementType.BOLT), eq(Settlement.BoltSettlementStatus.PENDING), any()))
                .thenReturn(List.of(s));

        var list = settlementService.listBoltSettlementsPending(10);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getBoltJobId()).isEqualTo("bolt-1");
        assertThat(list.get(0).getBoltPayoutAmount()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void exportBoltSettlementsCsv_includes_header_and_rows() {
        CashRequest req = new CashRequest();
        req.setId(UUID.randomUUID());
        Settlement s = Settlement.builder()
                .id(UUID.randomUUID())
                .request(req)
                .boltJobId("job-1")
                .boltPayoutAmount(new BigDecimal("10000"))
                .createdAt(Instant.EPOCH)
                .build();
        when(settlementRepository.findBySettlementTypeAndBoltSettlementStatusOrderByCreatedAtAsc(
                eq(Settlement.SettlementType.BOLT), eq(Settlement.BoltSettlementStatus.PENDING), any()))
                .thenReturn(List.of(s));

        String csv = settlementService.exportBoltSettlementsCsv(10);

        assertThat(csv).startsWith("settlement_id,request_id,bolt_job_id,bolt_payout_amount,created_at");
        assertThat(csv).contains("job-1");
        assertThat(csv).contains("10000");
    }
}

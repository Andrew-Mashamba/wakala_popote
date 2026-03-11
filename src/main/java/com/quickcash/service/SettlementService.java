package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.repository.BoltSettlementRepository;
import com.quickcash.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final AgentFloatService agentFloatService;
    private final BoltSettlementRepository boltSettlementRepository;

    @Transactional
    public Settlement createOnDelivery(CashRequest request, AgentAssignment assignment,
                                       String clientBankCode, String clientAccountNumber, BigDecimal clientDebitAmount,
                                       String agentSelcomAccountId, BigDecimal agentCreditAmount) {
        var s = Settlement.builder()
                .request(request)
                .assignment(assignment)
                .clientBankCode(clientBankCode)
                .clientAccountNumber(clientAccountNumber)
                .clientDebitAmount(clientDebitAmount)
                .clientDebitStatus(Settlement.ClientDebitStatus.SETTLED)
                .settlementType(Settlement.SettlementType.REALTIME)
                .agentSelcomAccountId(agentSelcomAccountId)
                .agentCreditAmount(agentCreditAmount)
                .agentCreditStatus(Settlement.AgentCreditStatus.PENDING)
                .build();
        s = settlementRepository.save(s);
        log.info("Settlement created: id={}, requestId={}, assignmentId={}, agentCreditAmount={}",
                s.getId(), request.getId(), assignment.getId(), agentCreditAmount);
        return s;
    }

    @Transactional
    public Settlement createOnBoltDelivery(CashRequest request, String boltJobId, BigDecimal boltPayoutAmount) {
        var s = Settlement.builder()
                .request(request)
                .assignment(null)
                .clientBankCode(request.getClientBankCode())
                .clientAccountNumber(request.getClientAccountNumber())
                .clientDebitAmount(request.getTotalClientCharge())
                .clientDebitStatus(Settlement.ClientDebitStatus.SETTLED)
                .settlementType(Settlement.SettlementType.BOLT)
                .boltJobId(boltJobId)
                .boltPayoutAmount(boltPayoutAmount)
                .boltSettlementStatus(Settlement.BoltSettlementStatus.PENDING)
                .build();
        s = settlementRepository.save(s);
        BoltSettlement bs = BoltSettlement.builder()
                .request(request)
                .boltJobId(boltJobId)
                .referenceId("qc_req_" + request.getId())
                .payoutAmount(boltPayoutAmount)
                .build();
        boltSettlementRepository.save(bs);
        log.info("Bolt settlement created: id={}, requestId={}, boltJobId={}, boltPayoutAmount={}",
                s.getId(), request.getId(), boltJobId, boltPayoutAmount);
        return s;
    }

    @Transactional
    public void markAgentCredited(UUID settlementId, String reference) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.setAgentCreditStatus(Settlement.AgentCreditStatus.CREDITED);
            s.setAgentCreditReference(reference);
            s.setAgentCreditedAt(Instant.now());
            settlementRepository.save(s);
            if (s.getAssignment() != null && s.getAssignment().getAgent() != null && s.getAgentCreditAmount() != null) {
                agentFloatService.recordSettlementCredit(
                        s.getAssignment().getAgent().getId(),
                        s.getAgentCreditAmount(),
                        s.getRequest().getId(),
                        s.getId(),
                        reference);
            }
            log.info("Settlement agent credited: id={}, reference={}", settlementId, reference);
        });
    }

    @Transactional
    public void markAgentCreditFailed(UUID settlementId, String error) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.setAgentCreditStatus(Settlement.AgentCreditStatus.FAILED);
            s.setAgentCreditError(error);
            settlementRepository.save(s);
            log.warn("Settlement agent credit failed: id={}, error={}", settlementId, error);
        });
    }

    public Optional<Settlement> findByRequestId(UUID requestId) {
        return settlementRepository.findByRequestId(requestId);
    }

    /** List Bolt settlements pending payout (Phase 3: settlement to Bolt). */
    public List<Settlement> listBoltSettlementsPending(int limit) {
        return settlementRepository.findBySettlementTypeAndBoltSettlementStatusOrderByCreatedAtAsc(
                Settlement.SettlementType.BOLT, Settlement.BoltSettlementStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /** Export Bolt PENDING settlements as CSV for reconciliation with Bolt (Phase 3). */
    public String exportBoltSettlementsCsv(int limit) {
        List<Settlement> list = listBoltSettlementsPending(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("settlement_id,request_id,bolt_job_id,bolt_payout_amount,created_at\n");
        DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;
        for (Settlement s : list) {
            sb.append(s.getId()).append(",")
                    .append(s.getRequest().getId()).append(",")
                    .append(s.getBoltJobId() != null ? s.getBoltJobId() : "").append(",")
                    .append(s.getBoltPayoutAmount() != null ? s.getBoltPayoutAmount().toPlainString() : "").append(",")
                    .append(s.getCreatedAt() != null ? iso.format(s.getCreatedAt()) : "")
                    .append("\n");
        }
        log.info("Exported {} Bolt settlements to CSV", list.size());
        return sb.toString();
    }
}

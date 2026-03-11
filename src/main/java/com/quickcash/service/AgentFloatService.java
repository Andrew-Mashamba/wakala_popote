package com.quickcash.service;

import com.quickcash.domain.Agent;
import com.quickcash.domain.AgentFloatTransaction;
import com.quickcash.domain.CashRequest;
import com.quickcash.domain.Settlement;
import com.quickcash.repository.AgentFloatTransactionRepository;
import com.quickcash.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Records agent float transactions (PROJECT.md §6.4). Call when crediting agent after delivery or cash in/out.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentFloatService {

    private final AgentRepository agentRepository;
    private final AgentFloatTransactionRepository floatTransactionRepository;

    /**
     * Record a settlement credit to agent (after delivery). Updates agent.availableCash and appends a float transaction.
     */
    @Transactional
    public AgentFloatTransaction recordSettlementCredit(UUID agentId, BigDecimal creditAmount,
                                                        UUID requestId, UUID settlementId, String reference) {
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) {
            log.warn("Agent float: agent not found for id={}", agentId);
            return null;
        }
        BigDecimal current = agent.getAvailableCash() != null ? agent.getAvailableCash() : BigDecimal.ZERO;
        BigDecimal balanceAfter = current.add(creditAmount);
        agent.setAvailableCash(balanceAfter);
        agentRepository.save(agent);

        var tx = AgentFloatTransaction.builder()
                .agent(agent)
                .transactionType(AgentFloatTransaction.TransactionType.SETTLEMENT_CREDIT)
                .amount(creditAmount)
                .balanceAfter(balanceAfter)
                .reference(reference)
                .notes("Settlement credit for delivery")
                .build();
        if (requestId != null) {
            CashRequest r = new CashRequest();
            r.setId(requestId);
            tx.setRequest(r);
        }
        if (settlementId != null) {
            Settlement s = new Settlement();
            s.setId(settlementId);
            tx.setSettlement(s);
        }
        tx = floatTransactionRepository.save(tx);
        log.info("Agent float: SETTLEMENT_CREDIT agentId={}, amount={}, balanceAfter={}", agentId, creditAmount, balanceAfter);
        return tx;
    }
}

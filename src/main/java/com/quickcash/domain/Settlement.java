package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlements_status", columnList = "client_debit_status, bolt_settlement_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AgentAssignment assignment;

    @Column(name = "client_bank_code")
    private String clientBankCode;

    @Column(name = "client_account_number")
    private String clientAccountNumber;

    @Column(name = "client_debit_amount", precision = 15, scale = 2)
    private BigDecimal clientDebitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_debit_status")
    @Builder.Default
    private ClientDebitStatus clientDebitStatus = ClientDebitStatus.PENDING;

    @Column(name = "client_debit_reference")
    private String clientDebitReference;

    @Column(name = "client_debit_error", columnDefinition = "TEXT")
    private String clientDebitError;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type")
    private SettlementType settlementType;

    @Column(name = "agent_selcom_account_id")
    private String agentSelcomAccountId;

    @Column(name = "agent_credit_amount", precision = 15, scale = 2)
    private BigDecimal agentCreditAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_credit_status")
    @Builder.Default
    private AgentCreditStatus agentCreditStatus = AgentCreditStatus.PENDING;

    @Column(name = "agent_credit_reference")
    private String agentCreditReference;

    @Column(name = "agent_credit_error", columnDefinition = "TEXT")
    private String agentCreditError;

    @Column(name = "platform_fee", precision = 15, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "selcom_fee", precision = 15, scale = 2)
    private BigDecimal selcomFee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "agent_credited_at")
    private Instant agentCreditedAt;

    @Column(name = "client_debited_at")
    private Instant clientDebitedAt;

    @Column(name = "bolt_job_id")
    private String boltJobId;

    @Column(name = "bolt_payout_amount", precision = 15, scale = 2)
    private BigDecimal boltPayoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "bolt_settlement_status")
    private BoltSettlementStatus boltSettlementStatus;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum ClientDebitStatus { PENDING, INITIATED, PENDING_BOT, SETTLED, FAILED }
    public enum SettlementType { REALTIME, BOT_T1, BOLT }
    public enum AgentCreditStatus { PENDING, CREDITED, FAILED }
    public enum BoltSettlementStatus { PENDING, PAID, FAILED }
}

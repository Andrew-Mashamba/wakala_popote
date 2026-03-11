package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cash deposit request: client deposits cash via agent, credited to client bank (PROJECT.md §6.4).
 */
@Entity
@Table(name = "deposit_requests", indexes = {
        @Index(name = "idx_deposits_status", columnList = "status"),
        @Index(name = "idx_deposits_client", columnList = "client_user_id"),
        @Index(name = "idx_deposits_agent", columnList = "assigned_agent_id"),
        @Index(name = "idx_deposits_location", columnList = "collection_lat, collection_lng"),
        @Index(name = "idx_deposits_bank", columnList = "destination_bank_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;

    @Column(name = "destination_bank_code", nullable = false, length = 10)
    private String destinationBankCode;

    @Column(name = "destination_account_number", nullable = false, length = 20)
    private String destinationAccountNumber;

    @Column(name = "destination_account_name", length = 100)
    private String destinationAccountName;

    @Column(name = "collection_lat")
    private Double collectionLat;

    @Column(name = "collection_lng")
    private Double collectionLng;

    @Column(name = "collection_address", columnDefinition = "TEXT")
    private String collectionAddress;

    @Column(name = "cash_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashAmount;

    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "agent_commission", nullable = false, precision = 15, scale = 2)
    private BigDecimal agentCommission;

    @Column(name = "platform_margin", nullable = false, precision = 15, scale = 2)
    private BigDecimal platformMargin;

    @Column(name = "net_deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netDepositAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DepositStatus status = DepositStatus.PENDING_VERIFICATION;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "selcom_debit_reference", length = 50)
    private String selcomDebitReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "selcom_debit_status")
    private SelcomDebitStatus selcomDebitStatus;

    @Column(name = "tips_credit_reference", length = 50)
    private String tipsCreditReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "tips_credit_status")
    private TipsCreditStatus tipsCreditStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "agent_assigned_at")
    private Instant agentAssignedAt;

    @Column(name = "agent_en_route_at")
    private Instant agentEnRouteAt;

    @Column(name = "cash_collected_at")
    private Instant cashCollectedAt;

    @Column(name = "credit_completed_at")
    private Instant creditCompletedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum DepositStatus {
        PENDING_VERIFICATION, VERIFIED, SEARCHING_AGENT, AGENT_ASSIGNED, AGENT_EN_ROUTE,
        CASH_COLLECTED, PROCESSING_CREDIT, CREDITED, COMPLETED, FAILED, CANCELLED
    }

    public enum SelcomDebitStatus { PENDING, SUCCESS, FAILED }
    public enum TipsCreditStatus { PENDING, SUCCESS, FAILED }
}

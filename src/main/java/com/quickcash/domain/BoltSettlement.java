package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Optional Bolt payout reconciliation table (BACKEND_IMPLEMENTATION_PLAN §4.1). One row per Bolt delivery for reporting/payout.
 */
@Entity
@Table(name = "bolt_settlements", indexes = {
        @Index(name = "idx_bolt_settlement_job", columnList = "bolt_job_id"),
        @Index(name = "idx_bolt_settlement_request", columnList = "request_id"),
        @Index(name = "idx_bolt_settlement_status", columnList = "payout_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoltSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @Column(name = "bolt_job_id", nullable = false, length = 64)
    private String boltJobId;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "payout_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal payoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum PayoutStatus { PENDING, PAID, FAILED }
}

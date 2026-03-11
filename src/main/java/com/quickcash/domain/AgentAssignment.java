package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method")
    @Builder.Default
    private AssignmentMethod assignmentMethod = AssignmentMethod.BROADCAST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "agent_lat_at_accept")
    private Double agentLatAtAccept;

    @Column(name = "agent_lng_at_accept")
    private Double agentLngAtAccept;

    @Column(name = "delivery_confirmed_at")
    private Instant deliveryConfirmedAt;

    @Column(name = "recipient_otp_entered", length = 6)
    private String recipientOtpEntered;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status")
    private SettlementStatus settlementStatus;

    @Column(name = "settlement_reference")
    private String settlementReference;

    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "settled_at")
    private Instant settledAt;

    @PrePersist
    void prePersist() {
        if (assignedAt == null) assignedAt = Instant.now();
    }

    public enum AssignmentMethod { AUTO, MANUAL, BROADCAST }
    public enum AssignmentStatus { PENDING, ACCEPTED, REJECTED, EN_ROUTE, ARRIVED, DELIVERED, CANCELLED }
    public enum SettlementStatus { PENDING, CREDITED, FAILED }
}

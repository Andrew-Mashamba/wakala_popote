package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** B2B bulk disbursement batch (PROJECT.md §8.3). */
@Entity
@Table(name = "b2b_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class B2bBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", length = 100)
    private String businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BatchStatus status = BatchStatus.CREATED;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum BatchStatus { CREATED, PROCESSING, COMPLETED, PARTIAL_FAILED }
}

package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "b2b_batch_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class B2bBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private B2bBatch batch;

    @Column(name = "recipient_phone", length = 15)
    private String recipientPhone;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_request_id")
    private CashRequest cashRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ItemStatus status = ItemStatus.PENDING;

    public enum ItemStatus { PENDING, CREATED, FAILED }
}

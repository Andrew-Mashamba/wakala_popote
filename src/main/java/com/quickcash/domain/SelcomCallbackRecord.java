package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency for Selcom webhook callbacks: process each order_id/transaction once.
 */
@Entity
@Table(name = "selcom_callback_records", indexes = {
        @Index(name = "idx_selcom_callback_order", columnList = "order_id", unique = true),
        @Index(name = "idx_selcom_callback_transid", columnList = "transid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelcomCallbackRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "transid", length = 64)
    private String transid;

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (processedAt == null) processedAt = Instant.now();
    }
}

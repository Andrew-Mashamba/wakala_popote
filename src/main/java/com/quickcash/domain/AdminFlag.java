package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Compliance or fraud flag for admin review. Cleared or blocked via admin APIs. */
@Entity
@Table(name = "admin_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false)
    private FlagType flagType;

    @Column(name = "entity_type", length = 50)
    private String entityType; // AGENT, USER, APPLICATION

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "resolved")
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "blocked")
    @Builder.Default
    private Boolean blocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum FlagType { COMPLIANCE, FRAUD }
}

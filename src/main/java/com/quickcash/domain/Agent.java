package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents", indexes = {
        @Index(name = "idx_agents_available", columnList = "is_available, current_lat, current_lng"),
        @Index(name = "idx_agents_selcom", columnList = "selcom_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "selcom_account_id", nullable = false, length = 20)
    private String selcomAccountId;

    @Column(name = "selcom_account_name", length = 100)
    private String selcomAccountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "selcom_account_status")
    @Builder.Default
    private SelcomAccountStatus selcomAccountStatus = SelcomAccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_tier")
    @Builder.Default
    private AgentTier agentTier = AgentTier.NEW;

    @Column(name = "total_deliveries")
    @Builder.Default
    private Integer totalDeliveries = 0;

    @Column(name = "total_earnings", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("5.00");

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "available_cash", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal availableCash = BigDecimal.ZERO;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "last_location_update")
    private Instant lastLocationUpdate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum SelcomAccountStatus { ACTIVE, SUSPENDED, CLOSED }
    public enum AgentTier { NEW, VERIFIED, SUPER }
}

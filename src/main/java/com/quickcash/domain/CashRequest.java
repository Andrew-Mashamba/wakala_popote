package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_requests", indexes = {
        @Index(name = "idx_requests_status", columnList = "status"),
        @Index(name = "idx_requests_location", columnList = "delivery_lat, delivery_lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    @Builder.Default
    private RequestType requestType = RequestType.LOCAL_CASH;

    @Column(name = "client_payment_method_id")
    private UUID clientPaymentMethodId;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "principal_amount", precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "service_fee", precision = 19, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "transport_fee", precision = 19, scale = 2)
    private BigDecimal transportFee;

    @Column(name = "agent_fee", precision = 19, scale = 2)
    private BigDecimal agentFee;

    @Column(name = "total_client_charge", precision = 19, scale = 2)
    private BigDecimal totalClientCharge;

    @Column(name = "total_agent_payment", precision = 19, scale = 2)
    private BigDecimal totalAgentPayment;

    @Column(name = "user_latitude", nullable = false)
    private Double userLatitude;

    @Column(name = "user_longitude", nullable = false)
    private Double userLongitude;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    private String clientName;
    private String clientImageUrl;

    @Column(name = "recipient_phone", length = 15)
    private String recipientPhone;
    @Column(name = "recipient_name", length = 100)
    private String recipientName;
    @Column(name = "recipient_location_lat")
    private Double recipientLocationLat;
    @Column(name = "recipient_location_lng")
    private Double recipientLocationLng;
    @Column(name = "recipient_location_address", columnDefinition = "TEXT")
    private String recipientLocationAddress;
    @Column(name = "recipient_otp", length = 6)
    private String recipientOtp;
    @Column(name = "recipient_otp_expires_at")
    private Instant recipientOtpExpiresAt;
    @Column(name = "recipient_otp_verified")
    private Boolean recipientOtpVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CashRequestStatus status = CashRequestStatus.PENDING_VERIFICATION;

    @Column(name = "agent_user_id")
    private UUID agentUserId;

    @Column(name = "bolt_job_id")
    private String boltJobId;

    @Column(name = "selcom_request_id")
    private String selcomRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "selcom_verification_status")
    private SelcomVerificationStatus selcomVerificationStatus;

    @Column(name = "client_bank_code", length = 10)
    private String clientBankCode;

    @Column(name = "client_account_number", length = 20)
    private String clientAccountNumber;

    @Column(name = "client_account_name", length = 100)
    private String clientAccountName;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "agent_assigned_at")
    private Instant agentAssignedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    public enum RequestType {
        LOCAL_CASH,
        BOLT_DELIVERY,
        REMOTE_SEND
    }

    public enum SelcomVerificationStatus {
        PENDING,
        VERIFIED,
        FAILED
    }

    public enum CashRequestStatus {
        PENDING_VERIFICATION,
        VERIFIED,
        SEARCHING_AGENT,
        AGENT_ASSIGNED,
        AGENT_EN_ROUTE,
        DELIVERED,
        SETTLED,
        FAILED,
        CANCELLED,
        EXPIRED,
        // Legacy aliases for backward compat
        PENDING,
        ACCEPTED,
        ARRIVED,
        COMPLETED
    }
}

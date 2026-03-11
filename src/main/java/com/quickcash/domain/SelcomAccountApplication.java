package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Selcom account application (agent onboarding eKYC). PROJECT.md §6.4.
 */
@Entity
@Table(name = "selcom_account_applications", indexes = {
        @Index(name = "idx_applications_status", columnList = "status"),
        @Index(name = "idx_applications_nida", columnList = "nida_number"),
        @Index(name = "idx_applications_phone", columnList = "phone_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelcomAccountApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 50)
    @Builder.Default
    private String nationality = "Tanzanian";

    @Column(name = "nida_number", nullable = false, unique = true, length = 20)
    private String nidaNumber;

    @Column(name = "nida_verified")
    @Builder.Default
    private Boolean nidaVerified = false;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "phone_otp", length = 6)
    private String phoneOtp;

    @Column(name = "phone_otp_expires_at")
    private Instant phoneOtpExpiresAt;

    @Column(length = 100)
    private String email;

    @Column(name = "id_front_image_url", length = 500)
    private String idFrontImageUrl;

    @Column(name = "id_back_image_url", length = 500)
    private String idBackImageUrl;

    @Column(name = "selfie_image_url", length = 500)
    private String selfieImageUrl;

    @Column(name = "face_match_passed")
    @Builder.Default
    private Boolean faceMatchPassed = false;

    @Column(name = "liveness_check_passed")
    @Builder.Default
    private Boolean livenessCheckPassed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "pep_check_status")
    @Builder.Default
    private ComplianceStatus pepCheckStatus = ComplianceStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanctions_check_status")
    @Builder.Default
    private ComplianceStatus sanctionsCheckStatus = ComplianceStatus.PENDING;

    @Column(name = "application_attempts")
    @Builder.Default
    private Integer applicationAttempts = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "selcom_account_id", length = 50)
    private String selcomAccountId;

    @Column(name = "selcom_account_created_at")
    private Instant selcomAccountCreatedAt;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "application")
    private AgentOnboardingProgress onboardingProgress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum Gender { MALE, FEMALE }
    public enum ComplianceStatus { PENDING, CLEAR, FLAGGED }
    public enum ApplicationStatus {
        DRAFT, SUBMITTED, NIDA_VERIFICATION, FACE_VERIFICATION, COMPLIANCE_CHECK,
        MANUAL_REVIEW, APPROVED, ACCOUNT_CREATED, REJECTED, BLOCKED
    }
}

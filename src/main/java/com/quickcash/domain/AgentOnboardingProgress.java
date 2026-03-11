package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_onboarding_progress", indexes = {
        @Index(name = "idx_onboarding_application", columnList = "application_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentOnboardingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private SelcomAccountApplication application;

    @Column(name = "welcome_video_watched")
    @Builder.Default
    private Boolean welcomeVideoWatched = false;

    @Column(name = "cash_handling_module_completed")
    @Builder.Default
    private Boolean cashHandlingModuleCompleted = false;

    @Column(name = "safety_module_completed")
    @Builder.Default
    private Boolean safetyModuleCompleted = false;

    @Column(name = "app_tutorial_completed")
    @Builder.Default
    private Boolean appTutorialCompleted = false;

    @Column(name = "quiz_score")
    private Integer quizScore;

    @Column(name = "quiz_passed")
    @Builder.Default
    private Boolean quizPassed = false;

    @Column(name = "float_deposited")
    @Builder.Default
    private Boolean floatDeposited = false;

    @Column(name = "float_deposit_amount", precision = 15, scale = 2)
    private BigDecimal floatDepositAmount;

    @Column(name = "profile_completed")
    @Builder.Default
    private Boolean profileCompleted = false;

    @Column(name = "terms_accepted")
    @Builder.Default
    private Boolean termsAccepted = false;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status")
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.NOT_STARTED;

    @Column(name = "ready_for_activation")
    @Builder.Default
    private Boolean readyForActivation = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public enum OnboardingStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }
}

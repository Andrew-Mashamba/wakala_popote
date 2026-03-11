package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OnboardingProgressResponse {

    private UUID applicationId;
    private String onboardingStatus;
    private Boolean welcomeVideoWatched;
    private Boolean cashHandlingModuleCompleted;
    private Boolean safetyModuleCompleted;
    private Boolean appTutorialCompleted;
    private Boolean quizPassed;
    private Boolean floatDeposited;
    private Boolean profileCompleted;
    private Boolean termsAccepted;
    private Boolean readyForActivation;
}

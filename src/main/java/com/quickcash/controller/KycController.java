package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.AgentOnboardingProgress;
import com.quickcash.domain.SelcomAccountApplication;
import com.quickcash.dto.KycApplicationRequest;
import com.quickcash.dto.KycApplicationResponse;
import com.quickcash.dto.OnboardingProgressResponse;
import com.quickcash.service.KycApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycApplicationService kycApplicationService;

    @PostMapping("/apply")
    public ResponseEntity<KycApplicationResponse> apply(@CurrentUser UUID userId, @RequestBody @Valid KycApplicationRequest request) {
        SelcomAccountApplication app = kycApplicationService.apply(userId, request);
        return ResponseEntity.ok(toResponse(app));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<KycApplicationResponse>> listApplications(@CurrentUser UUID userId) {
        var list = kycApplicationService.listByUser(userId).stream()
                .map(KycController::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<KycApplicationResponse> getApplication(@CurrentUser UUID userId, @PathVariable UUID id) {
        SelcomAccountApplication app = kycApplicationService.getByIdAndUser(id, userId);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/submit")
    public ResponseEntity<KycApplicationResponse> submit(@CurrentUser UUID userId, @PathVariable UUID id) {
        SelcomAccountApplication app = kycApplicationService.submit(id, userId);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/nida")
    public ResponseEntity<KycApplicationResponse> submitNida(@CurrentUser UUID userId, @PathVariable UUID id) {
        SelcomAccountApplication app = kycApplicationService.submitNida(id, userId);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/id-doc")
    public ResponseEntity<KycApplicationResponse> uploadIdDoc(@CurrentUser UUID userId, @PathVariable UUID id,
                                                            @RequestBody Map<String, String> body) {
        String idFrontUrl = body.get("idFrontImageUrl");
        String idBackUrl = body.get("idBackImageUrl");
        SelcomAccountApplication app = kycApplicationService.uploadIdDoc(id, userId, idFrontUrl, idBackUrl);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/selfie")
    public ResponseEntity<KycApplicationResponse> uploadSelfie(@CurrentUser UUID userId, @PathVariable UUID id,
                                                              @RequestBody Map<String, String> body) {
        String selfieUrl = body.get("selfieImageUrl");
        SelcomAccountApplication app = kycApplicationService.uploadSelfie(id, userId, selfieUrl);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/face-verification")
    public ResponseEntity<KycApplicationResponse> completeFaceVerification(@CurrentUser UUID userId, @PathVariable UUID id,
                                                                         @RequestBody Map<String, Boolean> body) {
        boolean faceMatchPassed = Boolean.TRUE.equals(body.get("faceMatchPassed"));
        boolean livenessPassed = Boolean.TRUE.equals(body.get("livenessCheckPassed"));
        SelcomAccountApplication app = kycApplicationService.completeFaceVerification(id, userId, faceMatchPassed, livenessPassed);
        return ResponseEntity.ok(toResponse(app));
    }

    @PostMapping("/applications/{id}/phone-otp/request")
    public ResponseEntity<Void> requestPhoneOtp(@CurrentUser UUID userId, @PathVariable UUID id) {
        kycApplicationService.requestPhoneOtp(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applications/{id}/phone-otp/verify")
    public ResponseEntity<KycApplicationResponse> verifyPhoneOtp(@CurrentUser UUID userId, @PathVariable UUID id,
                                                               @RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        SelcomAccountApplication app = kycApplicationService.verifyPhoneOtp(id, userId, otp);
        return ResponseEntity.ok(toResponse(app));
    }

    @GetMapping("/applications/{id}/onboarding")
    public ResponseEntity<OnboardingProgressResponse> getOnboardingProgress(@CurrentUser UUID userId, @PathVariable UUID id) {
        AgentOnboardingProgress progress = kycApplicationService.getOnboardingProgress(id, userId);
        return ResponseEntity.ok(toOnboardingResponse(progress));
    }

    @PutMapping("/applications/{id}/onboarding")
    public ResponseEntity<OnboardingProgressResponse> updateOnboardingProgress(@CurrentUser UUID userId, @PathVariable UUID id,
                                                                               @RequestBody Map<String, Object> body) {
        Boolean welcomeVideoWatched = getBoolean(body, "welcomeVideoWatched");
        Boolean cashHandlingModuleCompleted = getBoolean(body, "cashHandlingModuleCompleted");
        Boolean safetyModuleCompleted = getBoolean(body, "safetyModuleCompleted");
        Boolean appTutorialCompleted = getBoolean(body, "appTutorialCompleted");
        Integer quizScore = body.get("quizScore") != null ? ((Number) body.get("quizScore")).intValue() : null;
        Boolean quizPassed = getBoolean(body, "quizPassed");
        Boolean floatDeposited = getBoolean(body, "floatDeposited");
        BigDecimal floatDepositAmount = body.get("floatDepositAmount") != null ? new BigDecimal(body.get("floatDepositAmount").toString()) : null;
        Boolean profileCompleted = getBoolean(body, "profileCompleted");
        Boolean termsAccepted = getBoolean(body, "termsAccepted");
        AgentOnboardingProgress progress = kycApplicationService.updateOnboardingProgress(id, userId,
                welcomeVideoWatched, cashHandlingModuleCompleted, safetyModuleCompleted, appTutorialCompleted,
                quizScore, quizPassed, floatDeposited, floatDepositAmount, profileCompleted, termsAccepted);
        return ResponseEntity.ok(toOnboardingResponse(progress));
    }

    private static Boolean getBoolean(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    private static KycApplicationResponse toResponse(SelcomAccountApplication app) {
        return KycApplicationResponse.builder()
                .id(app.getId())
                .status(app.getStatus().name())
                .fullName(app.getFullName())
                .dateOfBirth(app.getDateOfBirth())
                .nidaNumber(app.getNidaNumber())
                .phoneNumber(app.getPhoneNumber())
                .phoneVerified(Boolean.TRUE.equals(app.getPhoneVerified()))
                .nidaVerified(Boolean.TRUE.equals(app.getNidaVerified()))
                .faceMatchPassed(Boolean.TRUE.equals(app.getFaceMatchPassed()))
                .livenessCheckPassed(Boolean.TRUE.equals(app.getLivenessCheckPassed()))
                .createdAt(app.getCreatedAt())
                .submittedAt(app.getSubmittedAt())
                .rejectionReason(app.getRejectionReason())
                .selcomAccountId(app.getSelcomAccountId())
                .build();
    }

    private static OnboardingProgressResponse toOnboardingResponse(AgentOnboardingProgress p) {
        return OnboardingProgressResponse.builder()
                .applicationId(p.getApplication().getId())
                .onboardingStatus(p.getOnboardingStatus().name())
                .welcomeVideoWatched(Boolean.TRUE.equals(p.getWelcomeVideoWatched()))
                .cashHandlingModuleCompleted(Boolean.TRUE.equals(p.getCashHandlingModuleCompleted()))
                .safetyModuleCompleted(Boolean.TRUE.equals(p.getSafetyModuleCompleted()))
                .appTutorialCompleted(Boolean.TRUE.equals(p.getAppTutorialCompleted()))
                .quizPassed(Boolean.TRUE.equals(p.getQuizPassed()))
                .floatDeposited(Boolean.TRUE.equals(p.getFloatDeposited()))
                .profileCompleted(Boolean.TRUE.equals(p.getProfileCompleted()))
                .termsAccepted(Boolean.TRUE.equals(p.getTermsAccepted()))
                .readyForActivation(Boolean.TRUE.equals(p.getReadyForActivation()))
                .build();
    }
}

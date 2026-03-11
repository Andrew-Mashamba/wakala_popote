package com.quickcash.service;

import com.quickcash.domain.AgentOnboardingProgress;
import com.quickcash.domain.SelcomAccountApplication;
import com.quickcash.dto.KycApplicationRequest;
import com.quickcash.dto.KycApplicationResponse;
import com.quickcash.dto.OnboardingProgressResponse;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.AgentOnboardingProgressRepository;
import com.quickcash.repository.SelcomAccountApplicationRepository;
import com.quickcash.selcom.SelcomApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * KYC / Selcom account application and onboarding. Logs to kyc.log via logger name.
 */
@Service
@RequiredArgsConstructor
public class KycApplicationService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.kyc");

    private final SelcomAccountApplicationRepository applicationRepository;
    private final AgentOnboardingProgressRepository onboardingRepository;
    private final UserService userService;
    private final SelcomApiClient selcomApiClient;

    @Transactional
    public SelcomAccountApplication apply(UUID userId, KycApplicationRequest req) {
        var user = userService.getById(userId.toString());
        if (applicationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .anyMatch(a -> a.getStatus() == SelcomAccountApplication.ApplicationStatus.DRAFT
                        || a.getStatus() == SelcomAccountApplication.ApplicationStatus.SUBMITTED
                        || a.getStatus().name().startsWith("NIDA") || a.getStatus().name().startsWith("FACE")
                        || a.getStatus() == SelcomAccountApplication.ApplicationStatus.COMPLIANCE_CHECK
                        || a.getStatus() == SelcomAccountApplication.ApplicationStatus.MANUAL_REVIEW
                        || a.getStatus() == SelcomAccountApplication.ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("User already has an active application");
        }
        SelcomAccountApplication.Gender g = null;
        if (req.getGender() != null) {
            try {
                g = SelcomAccountApplication.Gender.valueOf(req.getGender().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        SelcomAccountApplication app = SelcomAccountApplication.builder()
                .user(user)
                .fullName(req.getFullName())
                .dateOfBirth(req.getDateOfBirth())
                .gender(g)
                .nationality(req.getNationality() != null ? req.getNationality() : "Tanzanian")
                .nidaNumber(req.getNidaNumber())
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .status(SelcomAccountApplication.ApplicationStatus.DRAFT)
                .applicationAttempts(1)
                .build();
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        app = applicationRepository.save(app);
        AgentOnboardingProgress progress = AgentOnboardingProgress.builder()
                .application(app)
                .onboardingStatus(AgentOnboardingProgress.OnboardingStatus.NOT_STARTED)
                .build();
        progress.setCreatedAt(Instant.now());
        onboardingRepository.save(progress);
        log.info("KYC application created: id={}, userId={}, nida={}", app.getId(), userId, maskNida(app.getNidaNumber()));
        return app;
    }

    public SelcomAccountApplication getByIdAndUser(UUID applicationId, UUID userId) {
        SelcomAccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("KycApplication", applicationId));
        if (app.getUser() == null || !app.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("KycApplication", applicationId);
        }
        return app;
    }

    public List<SelcomAccountApplication> listByUser(UUID userId) {
        var user = userService.getById(userId.toString());
        return applicationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public SelcomAccountApplication submit(UUID applicationId, UUID userId) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        if (app.getStatus() != SelcomAccountApplication.ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application already submitted or in progress");
        }
        app.setStatus(SelcomAccountApplication.ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(Instant.now());
        app = applicationRepository.save(app);
        log.info("KYC application submitted: id={}, userId={}", applicationId, userId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication submitNida(UUID applicationId, UUID userId) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        if (app.getStatus() != SelcomAccountApplication.ApplicationStatus.SUBMITTED) {
            return app;
        }
        app.setStatus(SelcomAccountApplication.ApplicationStatus.NIDA_VERIFICATION);
        app = applicationRepository.save(app);
        // Stub: mark NIDA verified for dev
        app.setNidaVerified(true);
        app.setStatus(SelcomAccountApplication.ApplicationStatus.FACE_VERIFICATION);
        app = applicationRepository.save(app);
        log.info("KYC NIDA verification (stub) completed: id={}", applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication uploadIdDoc(UUID applicationId, UUID userId, String idFrontUrl, String idBackUrl) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        app.setIdFrontImageUrl(idFrontUrl);
        app.setIdBackImageUrl(idBackUrl);
        app = applicationRepository.save(app);
        log.info("KYC ID doc uploaded: id={}", applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication uploadSelfie(UUID applicationId, UUID userId, String selfieUrl) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        app.setSelfieImageUrl(selfieUrl);
        app = applicationRepository.save(app);
        log.info("KYC selfie uploaded: id={}", applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication completeFaceVerification(UUID applicationId, UUID userId, boolean faceMatchPassed, boolean livenessPassed) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        app.setFaceMatchPassed(faceMatchPassed);
        app.setLivenessCheckPassed(livenessPassed);
        if (faceMatchPassed && livenessPassed) {
            app.setStatus(SelcomAccountApplication.ApplicationStatus.COMPLIANCE_CHECK);
            app.setPepCheckStatus(SelcomAccountApplication.ComplianceStatus.CLEAR);
            app.setSanctionsCheckStatus(SelcomAccountApplication.ComplianceStatus.CLEAR);
            app.setStatus(SelcomAccountApplication.ApplicationStatus.APPROVED);
        }
        app = applicationRepository.save(app);
        log.info("KYC face verification completed: id={}, faceMatch={}, liveness={}", applicationId, faceMatchPassed, livenessPassed);
        return app;
    }

    @Transactional
    public SelcomAccountApplication requestPhoneOtp(UUID applicationId, UUID userId) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        app.setPhoneOtp(otp);
        app.setPhoneOtpExpiresAt(Instant.now().plusSeconds(300));
        app = applicationRepository.save(app);
        log.info("KYC phone OTP requested: id={} (stub; OTP not sent)", applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication verifyPhoneOtp(UUID applicationId, UUID userId, String otp) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        if (app.getPhoneOtp() == null || app.getPhoneOtpExpiresAt() == null || Instant.now().isAfter(app.getPhoneOtpExpiresAt())) {
            throw new IllegalStateException("OTP expired or not requested");
        }
        if (!app.getPhoneOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        app.setPhoneVerified(true);
        app.setPhoneOtp(null);
        app.setPhoneOtpExpiresAt(null);
        app = applicationRepository.save(app);
        log.info("KYC phone OTP verified: id={}", applicationId);
        return app;
    }

    public AgentOnboardingProgress getOnboardingProgress(UUID applicationId, UUID userId) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        return onboardingRepository.findByApplication(app)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingProgress", applicationId));
    }

    @Transactional
    public AgentOnboardingProgress updateOnboardingProgress(UUID applicationId, UUID userId,
                                                            Boolean welcomeVideoWatched, Boolean cashHandlingCompleted,
                                                            Boolean safetyCompleted, Boolean appTutorialCompleted,
                                                            Integer quizScore, Boolean quizPassed,
                                                            Boolean floatDeposited, BigDecimal floatAmount,
                                                            Boolean profileCompleted, Boolean termsAccepted) {
        SelcomAccountApplication app = getByIdAndUser(applicationId, userId);
        AgentOnboardingProgress progress = onboardingRepository.findByApplication(app)
                .orElseGet(() -> {
                    AgentOnboardingProgress p = AgentOnboardingProgress.builder()
                            .application(app)
                            .onboardingStatus(AgentOnboardingProgress.OnboardingStatus.IN_PROGRESS)
                            .build();
                    p.setCreatedAt(Instant.now());
                    return onboardingRepository.save(p);
                });
        if (welcomeVideoWatched != null) progress.setWelcomeVideoWatched(welcomeVideoWatched);
        if (cashHandlingCompleted != null) progress.setCashHandlingModuleCompleted(cashHandlingCompleted);
        if (safetyCompleted != null) progress.setSafetyModuleCompleted(safetyCompleted);
        if (appTutorialCompleted != null) progress.setAppTutorialCompleted(appTutorialCompleted);
        if (quizScore != null) progress.setQuizScore(quizScore);
        if (quizPassed != null) progress.setQuizPassed(quizPassed);
        if (floatDeposited != null) progress.setFloatDeposited(floatDeposited);
        if (floatAmount != null) progress.setFloatDepositAmount(floatAmount);
        if (profileCompleted != null) progress.setProfileCompleted(profileCompleted);
        if (termsAccepted != null) {
            progress.setTermsAccepted(termsAccepted);
            if (termsAccepted) progress.setTermsAcceptedAt(Instant.now());
        }
        progress.setOnboardingStatus(AgentOnboardingProgress.OnboardingStatus.IN_PROGRESS);
        boolean ready = Boolean.TRUE.equals(progress.getWelcomeVideoWatched())
                && Boolean.TRUE.equals(progress.getCashHandlingModuleCompleted())
                && Boolean.TRUE.equals(progress.getSafetyModuleCompleted())
                && Boolean.TRUE.equals(progress.getAppTutorialCompleted())
                && Boolean.TRUE.equals(progress.getQuizPassed())
                && Boolean.TRUE.equals(progress.getFloatDeposited())
                && Boolean.TRUE.equals(progress.getProfileCompleted())
                && Boolean.TRUE.equals(progress.getTermsAccepted());
        progress.setReadyForActivation(ready);
        if (ready) {
            progress.setOnboardingStatus(AgentOnboardingProgress.OnboardingStatus.COMPLETED);
            progress.setCompletedAt(Instant.now());
        }
        progress = onboardingRepository.save(progress);
        log.info("KYC onboarding progress updated: applicationId={}, readyForActivation={}", applicationId, ready);
        return progress;
    }

    /** Called by admin when approving: create Selcom account. */
    @Transactional
    public SelcomAccountApplication createSelcomAccount(UUID applicationId) {
        SelcomAccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("KycApplication", applicationId));
        if (app.getStatus() != SelcomAccountApplication.ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application must be APPROVED to create account");
        }
        var result = selcomApiClient.createAgentAccount(app.getFullName(), app.getPhoneNumber(), app.getNidaNumber());
        if (result.isSuccess()) {
            app.setSelcomAccountId(result.getAccountId());
            app.setStatus(SelcomAccountApplication.ApplicationStatus.ACCOUNT_CREATED);
            app.setSelcomAccountCreatedAt(Instant.now());
            app = applicationRepository.save(app);
            log.info("Selcom account created for application: id={}, accountId={}", applicationId, result.getAccountId());
        } else {
            log.error("Selcom createAccount failed for application: id={}, error={}", applicationId, result.getError());
        }
        return app;
    }

    private static String maskNida(String nida) {
        if (nida == null || nida.length() < 4) return "****";
        return "****" + nida.substring(nida.length() - 4);
    }
}

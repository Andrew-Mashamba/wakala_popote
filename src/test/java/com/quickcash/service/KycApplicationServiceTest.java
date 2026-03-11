package com.quickcash.service;

import com.quickcash.domain.AgentOnboardingProgress;
import com.quickcash.domain.SelcomAccountApplication;
import com.quickcash.domain.User;
import com.quickcash.dto.KycApplicationRequest;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.AgentOnboardingProgressRepository;
import com.quickcash.repository.SelcomAccountApplicationRepository;
import com.quickcash.selcom.SelcomApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycApplicationServiceTest {

    @Mock
    SelcomAccountApplicationRepository applicationRepository;
    @Mock
    AgentOnboardingProgressRepository onboardingRepository;
    @Mock
    UserService userService;
    @Mock
    SelcomApiClient selcomApiClient;

    @InjectMocks
    KycApplicationService kycApplicationService;

    @Test
    void apply_creates_application_and_onboarding_progress() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userService.getById(userId.toString())).thenReturn(user);
        when(applicationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(applicationRepository.save(any(SelcomAccountApplication.class))).thenAnswer(i -> {
            SelcomAccountApplication a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            return a;
        });
        when(onboardingRepository.save(any(AgentOnboardingProgress.class))).thenAnswer(i -> {
            AgentOnboardingProgress p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            return p;
        });

        KycApplicationRequest req = new KycApplicationRequest();
        req.setFullName("John Doe");
        req.setDateOfBirth(LocalDate.of(1990, 1, 15));
        req.setNidaNumber("19900115-12345-67890");
        req.setPhoneNumber("255712345678");
        req.setNationality("Tanzanian");

        SelcomAccountApplication result = kycApplicationService.apply(userId, req);

        assertThat(result.getStatus()).isEqualTo(SelcomAccountApplication.ApplicationStatus.DRAFT);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getNidaNumber()).isEqualTo("19900115-12345-67890");
        verify(applicationRepository).save(any(SelcomAccountApplication.class));
        verify(onboardingRepository).save(any(AgentOnboardingProgress.class));
    }

    @Test
    void submit_updates_status_to_submitted() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        SelcomAccountApplication app = SelcomAccountApplication.builder()
                .id(appId)
                .user(user)
                .status(SelcomAccountApplication.ApplicationStatus.DRAFT)
                .build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(SelcomAccountApplication.class))).thenAnswer(i -> i.getArgument(0));

        SelcomAccountApplication result = kycApplicationService.submit(appId, userId);

        assertThat(result.getStatus()).isEqualTo(SelcomAccountApplication.ApplicationStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
        verify(applicationRepository).save(app);
    }

    @Test
    void getByIdAndUser_throws_when_different_user() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        SelcomAccountApplication app = SelcomAccountApplication.builder()
                .id(appId)
                .user(otherUser)
                .build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> kycApplicationService.getByIdAndUser(appId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

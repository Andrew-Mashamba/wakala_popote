package com.quickcash.repository;

import com.quickcash.domain.AgentOnboardingProgress;
import com.quickcash.domain.SelcomAccountApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentOnboardingProgressRepository extends JpaRepository<AgentOnboardingProgress, UUID> {

    Optional<AgentOnboardingProgress> findByApplication(SelcomAccountApplication application);

    Optional<AgentOnboardingProgress> findByApplicationId(UUID applicationId);
}

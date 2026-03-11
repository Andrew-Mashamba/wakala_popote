package com.quickcash.repository;

import com.quickcash.domain.Agent;
import com.quickcash.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByUserId(UUID userId);

    Optional<Agent> findByUser(User user);

    List<Agent> findByIsAvailableTrue();
}

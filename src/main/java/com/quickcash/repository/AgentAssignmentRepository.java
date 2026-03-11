package com.quickcash.repository;

import com.quickcash.domain.Agent;
import com.quickcash.domain.AgentAssignment;
import com.quickcash.domain.CashRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentAssignmentRepository extends JpaRepository<AgentAssignment, UUID> {

    List<AgentAssignment> findByRequestOrderByAssignedAtDesc(CashRequest request);

    Optional<AgentAssignment> findByRequestAndStatus(CashRequest request, AgentAssignment.AssignmentStatus status);

    Optional<AgentAssignment> findByRequestAndAgentAndStatus(CashRequest request, Agent agent, AgentAssignment.AssignmentStatus status);

    List<AgentAssignment> findByRequestAndStatusIn(CashRequest request, List<AgentAssignment.AssignmentStatus> statuses);

    List<AgentAssignment> findByAgentOrderByAssignedAtDesc(Agent agent, org.springframework.data.domain.Pageable pageable);
}

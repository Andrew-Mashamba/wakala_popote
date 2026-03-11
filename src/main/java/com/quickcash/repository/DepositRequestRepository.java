package com.quickcash.repository;

import com.quickcash.domain.Agent;
import com.quickcash.domain.DepositRequest;
import com.quickcash.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequest, UUID> {

    List<DepositRequest> findByClientUserOrderByCreatedAtDesc(User clientUser, Pageable pageable);

    List<DepositRequest> findByAssignedAgentOrderByCreatedAtDesc(Agent agent, Pageable pageable);

    List<DepositRequest> findByStatusOrderByCreatedAtDesc(DepositRequest.DepositStatus status, Pageable pageable);
}

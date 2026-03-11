package com.quickcash.repository;

import com.quickcash.domain.CashRequest;
import com.quickcash.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CashRequestRepository extends JpaRepository<CashRequest, UUID> {

    List<CashRequest> findByUserOrderByCreatedAtDesc(User user, org.springframework.data.domain.Pageable pageable);

    List<CashRequest> findByStatusOrderByCreatedAtDesc(CashRequest.CashRequestStatus status, org.springframework.data.domain.Pageable pageable);

    java.util.Optional<CashRequest> findByBoltJobId(String boltJobId);
}

package com.quickcash.repository;

import com.quickcash.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Optional<Settlement> findByRequestId(UUID requestId);

    Optional<Settlement> findByBoltJobId(String boltJobId);

    List<Settlement> findBySettlementTypeAndBoltSettlementStatusOrderByCreatedAtAsc(
            Settlement.SettlementType type, Settlement.BoltSettlementStatus boltStatus,
            org.springframework.data.domain.Pageable pageable);

    List<Settlement> findByRequestIdOrderByCreatedAtDesc(UUID requestId);
}

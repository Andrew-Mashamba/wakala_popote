package com.quickcash.repository;

import com.quickcash.domain.BoltSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoltSettlementRepository extends JpaRepository<BoltSettlement, UUID> {

    Optional<BoltSettlement> findByBoltJobId(String boltJobId);

    Optional<BoltSettlement> findByRequest_Id(UUID requestId);
}

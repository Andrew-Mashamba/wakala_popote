package com.quickcash.repository;

import com.quickcash.domain.SelcomCallbackRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SelcomCallbackRecordRepository extends JpaRepository<SelcomCallbackRecord, UUID> {

    Optional<SelcomCallbackRecord> findByOrderId(String orderId);
}

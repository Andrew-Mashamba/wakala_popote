package com.quickcash.repository;

import com.quickcash.domain.B2bBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface B2bBatchRepository extends JpaRepository<B2bBatch, UUID> {
}

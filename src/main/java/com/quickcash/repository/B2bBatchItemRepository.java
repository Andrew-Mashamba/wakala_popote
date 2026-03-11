package com.quickcash.repository;

import com.quickcash.domain.B2bBatch;
import com.quickcash.domain.B2bBatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface B2bBatchItemRepository extends JpaRepository<B2bBatchItem, UUID> {

    List<B2bBatchItem> findByBatchOrderById(B2bBatch batch);
}

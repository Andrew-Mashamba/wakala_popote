package com.quickcash.repository;

import com.quickcash.domain.QrCollectionRequest;
import com.quickcash.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QrCollectionRequestRepository extends JpaRepository<QrCollectionRequest, UUID> {

    Optional<QrCollectionRequest> findByQrToken(String qrToken);

    List<QrCollectionRequest> findByUserOrderByCreatedAtDesc(User user);
}

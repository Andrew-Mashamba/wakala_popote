package com.quickcash.repository;

import com.quickcash.domain.SelcomAccountApplication;
import com.quickcash.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SelcomAccountApplicationRepository extends JpaRepository<SelcomAccountApplication, UUID> {

    List<SelcomAccountApplication> findByUserOrderByCreatedAtDesc(User user);

    Optional<SelcomAccountApplication> findByNidaNumber(String nidaNumber);

    List<SelcomAccountApplication> findByStatusOrderByCreatedAtDesc(SelcomAccountApplication.ApplicationStatus status, org.springframework.data.domain.Pageable pageable);
}

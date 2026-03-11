package com.quickcash.repository;

import com.quickcash.domain.AdminFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminFlagRepository extends JpaRepository<AdminFlag, UUID> {

    List<AdminFlag> findByFlagTypeAndResolvedFalseOrderByCreatedAtDesc(AdminFlag.FlagType type);

    List<AdminFlag> findByFlagTypeAndBlockedFalseOrderByCreatedAtDesc(AdminFlag.FlagType type);
}

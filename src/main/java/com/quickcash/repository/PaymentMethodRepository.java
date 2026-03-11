package com.quickcash.repository;

import com.quickcash.domain.PaymentMethod;
import com.quickcash.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByUserOrderByIsDefaultDescCreatedAtAsc(User user);

    List<PaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtAsc(UUID userId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);
}

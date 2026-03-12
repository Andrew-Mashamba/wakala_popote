package com.quickcash.repository;

import com.quickcash.domain.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodTypeRepository extends JpaRepository<PaymentMethodType, String> {

    List<PaymentMethodType> findAllByOrderByDisplayOrderAscIdAsc();
}

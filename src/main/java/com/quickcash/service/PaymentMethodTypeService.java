package com.quickcash.service;

import com.quickcash.domain.PaymentMethodSubType;
import com.quickcash.domain.PaymentMethodType;
import com.quickcash.dto.PaymentMethodSubTypeResponse;
import com.quickcash.dto.PaymentMethodTypeResponse;
import com.quickcash.dto.PaymentMethodTypesResponse;
import com.quickcash.repository.PaymentMethodTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns payment method categories and sub-types from database.
 * Data is seeded from db/seed/payment_method_types.json - no hardcoding.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodTypeService {

    private final PaymentMethodTypeRepository paymentMethodTypeRepository;

    @Transactional(readOnly = true)
    public PaymentMethodTypesResponse getTypes() {
        List<PaymentMethodType> types = paymentMethodTypeRepository.findAllByOrderByDisplayOrderAscIdAsc();
        List<PaymentMethodTypeResponse> categories = types.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PaymentMethodTypesResponse.builder()
                .categories(categories)
                .build();
    }

    private PaymentMethodTypeResponse toResponse(PaymentMethodType type) {
        List<PaymentMethodSubType> subs = type.getSubTypes();
        List<PaymentMethodSubTypeResponse> subTypes = (subs != null ? subs : Collections.<PaymentMethodSubType>emptyList())
                .stream()
                .map(this::toSubTypeResponse)
                .collect(Collectors.toList());
        return PaymentMethodTypeResponse.builder()
                .id(type.getId())
                .label(type.getLabel())
                .subTypes(subTypes)
                .build();
    }

    private PaymentMethodSubTypeResponse toSubTypeResponse(PaymentMethodSubType sub) {
        return PaymentMethodSubTypeResponse.builder()
                .id(sub.getId())
                .label(sub.getLabel())
                .build();
    }
}

package com.quickcash.service;

import com.quickcash.domain.PaymentMethod;
import com.quickcash.dto.PaymentMethodRequest;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserService userService;

    @Transactional
    public PaymentMethod create(UUID userId, PaymentMethodRequest request) {
        var user = userService.getById(userId.toString());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultForUser(user);
        }
        var pm = PaymentMethod.builder()
                .user(user)
                .methodType(request.getMethodType())
                .mobileProvider(request.getMobileProvider())
                .mobileNumber(request.getMobileNumber())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .cardToken(request.getCardToken())
                .cardLastFour(request.getCardLastFour())
                .cardBrand(request.getCardBrand())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();
        return paymentMethodRepository.save(pm);
    }

    public List<PaymentMethod> listByUser(UUID userId) {
        return paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId);
    }

    public PaymentMethod getByIdAndUser(UUID id, UUID userId) {
        return paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", id));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var pm = getByIdAndUser(id, userId);
        paymentMethodRepository.delete(pm);
    }

    @Transactional
    public PaymentMethod setDefault(UUID id, UUID userId) {
        var pm = getByIdAndUser(id, userId);
        clearDefaultForUser(pm.getUser());
        pm.setIsDefault(true);
        return paymentMethodRepository.save(pm);
    }

    private void clearDefaultForUser(com.quickcash.domain.User user) {
        var list = paymentMethodRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user);
        for (var p : list) {
            if (Boolean.TRUE.equals(p.getIsDefault())) {
                p.setIsDefault(false);
                paymentMethodRepository.save(p);
            }
        }
    }
}

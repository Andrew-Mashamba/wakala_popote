package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.PaymentMethod;
import com.quickcash.dto.PaymentMethodRequest;
import com.quickcash.dto.PaymentMethodResponse;
import com.quickcash.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> list(@CurrentUser UUID userId) {
        var list = paymentMethodService.listByUser(userId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@CurrentUser UUID userId,
                                                        @RequestBody @Valid PaymentMethodRequest request) {
        var created = paymentMethodService.create(userId, request);
        return ResponseEntity.ok(toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        paymentMethodService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<PaymentMethodResponse> setDefault(@CurrentUser UUID userId, @PathVariable UUID id) {
        var updated = paymentMethodService.setDefault(id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    private PaymentMethodResponse toResponse(PaymentMethod pm) {
        return PaymentMethodResponse.builder()
                .id(pm.getId())
                .methodType(pm.getMethodType())
                .mobileProvider(pm.getMobileProvider())
                .mobileNumber(pm.getMobileNumber())
                .bankCode(pm.getBankCode())
                .accountNumber(pm.getAccountNumber())
                .accountName(pm.getAccountName())
                .cardLastFour(pm.getCardLastFour())
                .cardBrand(pm.getCardBrand())
                .isDefault(pm.getIsDefault())
                .isVerified(pm.getIsVerified())
                .build();
    }
}

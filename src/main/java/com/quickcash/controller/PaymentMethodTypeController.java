package com.quickcash.controller;

import com.quickcash.dto.PaymentMethodTypesResponse;
import com.quickcash.service.PaymentMethodTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-method-types")
@RequiredArgsConstructor
public class PaymentMethodTypeController {

    private final PaymentMethodTypeService paymentMethodTypeService;

    @GetMapping
    public ResponseEntity<PaymentMethodTypesResponse> getTypes() {
        return ResponseEntity.ok(paymentMethodTypeService.getTypes());
    }
}

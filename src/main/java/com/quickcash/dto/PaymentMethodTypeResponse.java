package com.quickcash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodTypeResponse {
    private String id;
    private String label;
    private List<PaymentMethodSubTypeResponse> subTypes;
}

package com.quickcash.dto;

import com.quickcash.domain.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PaymentMethodResponse {
    private UUID id;
    private PaymentMethod.MethodType methodType;
    private String mobileProvider;
    private String mobileNumber;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String cardLastFour;
    private String cardBrand;
    private Boolean isDefault;
    private Boolean isVerified;
}

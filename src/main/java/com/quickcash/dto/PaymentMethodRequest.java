package com.quickcash.dto;

import com.quickcash.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentMethodRequest {
    @NotNull
    private PaymentMethod.MethodType methodType;
    private String mobileProvider;
    private String mobileNumber;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String cardToken;
    private String cardLastFour;
    private String cardBrand;
    private Boolean isDefault;
}

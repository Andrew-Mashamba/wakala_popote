package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class B2bDisbursementItem {

    @NotBlank
    private String recipientPhone;

    private String recipientName;

    @NotNull
    @DecimalMin("1000")
    private BigDecimal amount;

    private String reference;

    private Double deliveryLatitude;

    private Double deliveryLongitude;
}

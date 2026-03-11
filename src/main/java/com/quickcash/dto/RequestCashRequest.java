package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestCashRequest {

    @NotNull
    private String userId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal requestedAmount;

    @NotNull
    private Double userLatitude;

    @NotNull
    private Double userLongitude;

    private String name;
    private String image;  // URL or base64
}

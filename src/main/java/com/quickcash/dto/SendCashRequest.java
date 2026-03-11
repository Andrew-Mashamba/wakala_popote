package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** Request body for POST /api/v1/cash/send (remote send to recipient with OTP). */
@Data
public class SendCashRequest {

    @NotBlank
    private String recipientPhone;

    private String recipientName;

    @NotNull
    @DecimalMin("1000")
    private BigDecimal amount;

    @NotNull
    private Double deliveryLatitude;

    @NotNull
    private Double deliveryLongitude;

    private String deliveryAddress;

    private UUID paymentMethodId;

    /** If true and payment method is bank, verify then collect and move to SEARCHING_AGENT. */
    private Boolean collectNow;
}

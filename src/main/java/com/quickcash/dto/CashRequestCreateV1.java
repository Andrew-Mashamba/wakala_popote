package com.quickcash.dto;

import com.quickcash.domain.CashRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CashRequestCreateV1 {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String deliveryAddress;

    private UUID paymentMethodId;

    /** If true and payment method is bank, verify then collect and move to SEARCHING_AGENT. */
    private Boolean collectNow;

    @NotNull
    @lombok.Builder.Default
    private CashRequest.RequestType requestType = CashRequest.RequestType.LOCAL_CASH;
}

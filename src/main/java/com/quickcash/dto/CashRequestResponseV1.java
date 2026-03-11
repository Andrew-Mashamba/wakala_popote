package com.quickcash.dto;

import com.quickcash.domain.CashRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CashRequestResponseV1 {
    private UUID id;
    private CashRequest.RequestType requestType;
    private String status;
    private BigDecimal principalAmount;
    private BigDecimal serviceFee;
    private BigDecimal transportFee;
    private BigDecimal totalClientCharge;
    private Double latitude;
    private Double longitude;
    private String deliveryAddress;
    private Instant createdAt;
    private Instant completedAt;
}

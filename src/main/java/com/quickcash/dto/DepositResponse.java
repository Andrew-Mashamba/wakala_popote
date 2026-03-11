package com.quickcash.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DepositResponse {

    private UUID id;
    private String status;
    private String destinationBankCode;
    private String destinationAccountNumber;
    private String destinationAccountName;
    private BigDecimal cashAmount;
    private BigDecimal serviceFee;
    private BigDecimal netDepositAmount;
    private String collectionAddress;
    private Double collectionLat;
    private Double collectionLng;
    private UUID assignedAgentId;
    private Instant createdAt;
    private Instant agentAssignedAt;
    private Instant cashCollectedAt;
    private Instant completedAt;
    private String failureReason;
    private String cancellationReason;
}

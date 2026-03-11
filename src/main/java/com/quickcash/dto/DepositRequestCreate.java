package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequestCreate {

    @NotBlank
    private String destinationBankCode;

    @NotBlank
    private String destinationAccountNumber;

    private String destinationAccountName;

    @NotNull
    @DecimalMin("1000")
    private BigDecimal cashAmount;

    private Double collectionLat;
    private Double collectionLng;
    private String collectionAddress;
}

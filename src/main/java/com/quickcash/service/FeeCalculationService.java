package com.quickcash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    @Value("${app.fees.service-fee-percent:3.0}")
    private double serviceFeePercent;

    @Value("${app.fees.transport-fee-fixed-tzs:1500}")
    private double transportFeeFixedTzs;

    @Value("${app.fees.agent-fee-base-tzs:2000}")
    private double agentFeeBaseTzs;

    @Value("${app.fees.deposit-service-fee-percent:0.5}")
    private double depositServiceFeePercent;

    @Value("${app.fees.deposit-agent-share-percent:70.0}")
    private double depositAgentSharePercent;

    public FeeBreakdown calculate(BigDecimal principalAmount) {
        BigDecimal principal = principalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = principal.multiply(BigDecimal.valueOf(serviceFeePercent / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal transportFee = BigDecimal.valueOf(transportFeeFixedTzs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal agentFee = BigDecimal.valueOf(agentFeeBaseTzs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalClientCharge = principal.add(serviceFee).add(transportFee);
        BigDecimal totalAgentPayment = principal.add(agentFee).add(transportFee);
        return FeeBreakdown.builder()
                .principalAmount(principal)
                .serviceFee(serviceFee)
                .transportFee(transportFee)
                .agentFee(agentFee)
                .totalClientCharge(totalClientCharge)
                .totalAgentPayment(totalAgentPayment)
                .build();
    }

    /** Deposit: service fee (e.g. 0.5%), split into agent commission + platform margin. */
    public DepositFeeBreakdown calculateDeposit(BigDecimal cashAmount) {
        BigDecimal cash = cashAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = cash.multiply(BigDecimal.valueOf(depositServiceFeePercent / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal agentCommission = serviceFee.multiply(BigDecimal.valueOf(depositAgentSharePercent / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformMargin = serviceFee.subtract(agentCommission).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netDeposit = cash.subtract(serviceFee).setScale(2, RoundingMode.HALF_UP);
        return DepositFeeBreakdown.builder()
                .cashAmount(cash)
                .serviceFee(serviceFee)
                .agentCommission(agentCommission)
                .platformMargin(platformMargin)
                .netDepositAmount(netDeposit)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class DepositFeeBreakdown {
        private BigDecimal cashAmount;
        private BigDecimal serviceFee;
        private BigDecimal agentCommission;
        private BigDecimal platformMargin;
        private BigDecimal netDepositAmount;
    }

    @lombok.Data
    @lombok.Builder
    public static class FeeBreakdown {
        private BigDecimal principalAmount;
        private BigDecimal serviceFee;
        private BigDecimal transportFee;
        private BigDecimal agentFee;
        private BigDecimal totalClientCharge;
        private BigDecimal totalAgentPayment;
    }
}

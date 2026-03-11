package com.quickcash.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FeeCalculationServiceTest {

    @Autowired
    FeeCalculationService feeCalculationService;

    @Test
    void calculate_returns_correct_breakdown() {
        BigDecimal principal = new BigDecimal("100000.00");
        FeeCalculationService.FeeBreakdown breakdown = feeCalculationService.calculate(principal);

        assertThat(breakdown.getPrincipalAmount()).isEqualByComparingTo(principal);
        assertThat(breakdown.getServiceFee()).isEqualByComparingTo(new BigDecimal("3000.00")); // 3%
        assertThat(breakdown.getTransportFee()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(breakdown.getAgentFee()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(breakdown.getTotalClientCharge()).isEqualByComparingTo(new BigDecimal("104500.00"));
        assertThat(breakdown.getTotalAgentPayment()).isEqualByComparingTo(new BigDecimal("103500.00"));
    }

    @Test
    void calculate_rounds_to_two_decimals() {
        FeeCalculationService.FeeBreakdown breakdown = feeCalculationService.calculate(new BigDecimal("100.33"));
        assertThat(breakdown.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("100.33"));
        assertThat(breakdown.getServiceFee().scale()).isEqualTo(2);
    }
}

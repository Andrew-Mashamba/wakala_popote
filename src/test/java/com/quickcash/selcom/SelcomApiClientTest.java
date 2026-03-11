package com.quickcash.selcom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When app.selcom.api-key is empty, client stubs success (for dev/test).
 */
@SpringBootTest
@ActiveProfiles("test")
class SelcomApiClientTest {

    @Autowired
    SelcomApiClient selcomApiClient;

    @Test
    void isConfigured_false_when_api_key_empty() {
        assertThat(selcomApiClient.isConfigured()).isFalse();
    }

    @Test
    void verifyAccount_returns_stub_success_when_not_configured() {
        SelcomApiClient.SelcomVerifyResult result = selcomApiClient.verifyAccount(
                "01", "1234567890", "Test User", new BigDecimal("50000"));
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getRequestId()).startsWith("stub-");
    }

    @Test
    void collectPayment_returns_stub_success_when_not_configured() {
        SelcomApiClient.SelcomCollectResult result = selcomApiClient.collectPayment(
                "stub-1", "01", "1234567890", "Test", new BigDecimal("52000"), "QC-ref");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("stub-txn-");
    }

    @Test
    void creditAgent_returns_stub_success_when_not_configured() {
        SelcomApiClient.SelcomCreditResult result = selcomApiClient.creditAgent(
                "agent-123", new BigDecimal("51000"), "QC-req-1");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("stub-credit-");
    }
}

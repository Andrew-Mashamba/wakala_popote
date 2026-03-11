package com.quickcash.selcom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Selcom API client (PROJECT.md §4.3). Auth: API Key + Secret with HMAC SHA256.
 * When api-key is not set, returns stub success for dev/test.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SelcomApiClient {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.selcom.api-url:https://api.selcommobile.com}")
    private String apiUrl;

    @Value("${app.selcom.api-key:}")
    private String apiKey;

    @Value("${app.selcom.vendor-secret:}")
    private String vendorSecret;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && vendorSecret != null && !vendorSecret.isBlank();
    }

    /**
     * Verify bank account (TIPS lookup). Returns true if account exists and can be debited.
     */
    public SelcomVerifyResult verifyAccount(String bankCode, String accountNumber, String accountName, java.math.BigDecimal amount) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing verifyAccount success for bankCode={}, accountNumber={}", bankCode, mask(accountNumber));
            return SelcomVerifyResult.builder().verified(true).requestId("stub-" + System.currentTimeMillis()).build();
        }
        try {
            String path = "/v1/utility/check/account";
            var body = objectMapper.createObjectNode()
                    .put("bank_code", bankCode)
                    .put("account_number", accountNumber)
                    .put("account_name", accountName)
                    .put("amount", amount.doubleValue());
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String requestId = response != null && response.has("request_id") ? response.path("request_id").asText() : null;
            log.info("Selcom verifyAccount: bankCode={}, result={}, requestId={}", bankCode, ok, requestId);
            return SelcomVerifyResult.builder().verified(ok).requestId(requestId).rawResponse(response).build();
        } catch (Exception e) {
            log.warn("Selcom verifyAccount failed: bankCode={}, error={}", bankCode, e.getMessage());
            return SelcomVerifyResult.builder().verified(false).error(e.getMessage()).build();
        }
    }

    /**
     * Initiate collection (debit) from client bank account.
     */
    public SelcomCollectResult collectPayment(String requestId, String bankCode, String accountNumber,
                                               String accountName, java.math.BigDecimal amount, String reference) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing collectPayment success for reference={}", reference);
            return SelcomCollectResult.builder().success(true).transactionId("stub-txn-" + System.currentTimeMillis()).build();
        }
        try {
            String path = "/v1/payments/c2b";
            var body = objectMapper.createObjectNode()
                    .put("request_id", requestId)
                    .put("bank_code", bankCode)
                    .put("account_number", accountNumber)
                    .put("account_name", accountName)
                    .put("amount", amount.doubleValue())
                    .put("reference", reference);
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String txnId = response != null && response.has("transaction_id") ? response.path("transaction_id").asText() : null;
            log.info("Selcom collectPayment: reference={}, result={}, transactionId={}", reference, ok, txnId);
            return SelcomCollectResult.builder().success(ok).transactionId(txnId).rawResponse(response).build();
        } catch (Exception e) {
            log.warn("Selcom collectPayment failed: reference={}, error={}", reference, e.getMessage());
            return SelcomCollectResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    /**
     * Debit agent's Selcom account (e.g. deposit flow: agent collected cash, we debit their float).
     */
    public SelcomDebitResult debitAgent(String agentSelcomAccountId, java.math.BigDecimal amount, String reference) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing debitAgent success for reference={}", reference);
            return SelcomDebitResult.builder().success(true).transactionId("stub-debit-" + System.currentTimeMillis()).build();
        }
        try {
            String path = "/v1/collections/agent";
            var body = objectMapper.createObjectNode()
                    .put("account_id", agentSelcomAccountId)
                    .put("amount", amount.doubleValue())
                    .put("reference", reference);
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String txnId = response != null && response.has("transaction_id") ? response.path("transaction_id").asText() : null;
            log.info("Selcom debitAgent: accountId={}, amount={}, result={}, transactionId={}", agentSelcomAccountId, amount, ok, txnId);
            return SelcomDebitResult.builder().success(ok).transactionId(txnId).rawResponse(response).build();
        } catch (Exception e) {
            log.warn("Selcom debitAgent failed: reference={}, error={}", reference, e.getMessage());
            return SelcomDebitResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    /**
     * Credit to client bank account (TIPS/TISS) after deposit collection.
     */
    public SelcomCreditResult creditToBank(String bankCode, String accountNumber, String accountName,
                                           java.math.BigDecimal amount, String reference) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing creditToBank success for reference={}", reference);
            return SelcomCreditResult.builder().success(true).transactionId("stub-tips-" + System.currentTimeMillis()).build();
        }
        try {
            String path = "/v1/disbursements/bank";
            var body = objectMapper.createObjectNode()
                    .put("bank_code", bankCode)
                    .put("account_number", accountNumber)
                    .put("account_name", accountName)
                    .put("amount", amount.doubleValue())
                    .put("reference", reference);
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String txnId = response != null && response.has("transaction_id") ? response.path("transaction_id").asText() : null;
            log.info("Selcom creditToBank: bankCode={}, result={}, transactionId={}", bankCode, ok, txnId);
            return SelcomCreditResult.builder().success(ok).transactionId(txnId).rawResponse(response).build();
        } catch (Exception e) {
            log.warn("Selcom creditToBank failed: reference={}, error={}", reference, e.getMessage());
            return SelcomCreditResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    /**
     * Create Selcom agent account (after KYC approval). Stub when not configured.
     */
    public SelcomCreateAccountResult createAgentAccount(String fullName, String phoneNumber, String nidaNumber) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing createAgentAccount for nida={}", mask(nidaNumber));
            return SelcomCreateAccountResult.builder()
                    .success(true)
                    .accountId("stub-account-" + System.currentTimeMillis())
                    .accountNumber("STUB" + (int)(Math.random() * 100000)).build();
        }
        try {
            String path = "/v1/accounts/agent";
            var body = objectMapper.createObjectNode()
                    .put("full_name", fullName)
                    .put("phone_number", phoneNumber)
                    .put("nida_number", nidaNumber);
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String accountId = response != null && response.has("account_id") ? response.path("account_id").asText() : null;
            String accountNumber = response != null && response.has("account_number") ? response.path("account_number").asText() : null;
            log.info("Selcom createAgentAccount: result={}, accountId={}", ok, accountId);
            return SelcomCreateAccountResult.builder().success(ok).accountId(accountId).accountNumber(accountNumber).build();
        } catch (Exception e) {
            log.warn("Selcom createAgentAccount failed: error={}", e.getMessage());
            return SelcomCreateAccountResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    /**
     * Credit agent's Selcom account (internal book transfer after delivery).
     */
    public SelcomCreditResult creditAgent(String agentSelcomAccountId, java.math.BigDecimal amount, String reference) {
        if (!isConfigured()) {
            log.debug("Selcom not configured; stubbing creditAgent success for reference={}", reference);
            return SelcomCreditResult.builder().success(true).transactionId("stub-credit-" + System.currentTimeMillis()).build();
        }
        try {
            String path = "/v1/disbursements/agent";
            var body = objectMapper.createObjectNode()
                    .put("account_id", agentSelcomAccountId)
                    .put("amount", amount.doubleValue())
                    .put("reference", reference);
            var response = post(path, body);
            boolean ok = response != null && response.has("result") && "00".equals(response.path("result").asText(""));
            String txnId = response != null && response.has("transaction_id") ? response.path("transaction_id").asText() : null;
            log.info("Selcom creditAgent: accountId={}, amount={}, result={}, transactionId={}", agentSelcomAccountId, amount, ok, txnId);
            return SelcomCreditResult.builder().success(ok).transactionId(txnId).rawResponse(response).build();
        } catch (Exception e) {
            log.warn("Selcom creditAgent failed: reference={}, error={}", reference, e.getMessage());
            return SelcomCreditResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    private JsonNode post(String path, com.fasterxml.jackson.databind.JsonNode body) throws Exception {
        String url = apiUrl + path;
        String bodyStr = objectMapper.writeValueAsString(body);
        String signature = sign(bodyStr);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SELCOM " + apiKey + ":" + signature);
        var entity = new HttpEntity<>(bodyStr, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (resp.getStatusCode().isError()) {
            throw new RuntimeException("Selcom API error: " + resp.getStatusCode());
        }
        if (resp.getBody() == null) return null;
        return objectMapper.readTree(resp.getBody());
    }

    private String sign(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(vendorSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmac);
    }

    /**
     * Verify webhook callback signature. Digest header should be base64(HMAC-SHA256(vendor-secret, rawBody)).
     * When vendor-secret is not configured, returns true (skip verification for dev).
     */
    public boolean verifyWebhookSignature(String rawBody, String digestHeader) {
        if (vendorSecret == null || vendorSecret.isBlank()) {
            return true;
        }
        if (digestHeader == null || digestHeader.isBlank()) {
            return false;
        }
        try {
            String expected = sign(rawBody);
            return java.security.MessageDigest.isEqual(
                    Base64.getDecoder().decode(digestHeader.trim()),
                    Base64.getDecoder().decode(expected));
        } catch (Exception e) {
            log.warn("Selcom webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private static String mask(String s) {
        if (s == null || s.length() < 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }

    @lombok.Data
    @lombok.Builder
    public static class SelcomVerifyResult {
        private boolean verified;
        private String requestId;
        private String error;
        private JsonNode rawResponse;
    }

    @lombok.Data
    @lombok.Builder
    public static class SelcomCollectResult {
        private boolean success;
        private String transactionId;
        private String error;
        private JsonNode rawResponse;
    }

    @lombok.Data
    @lombok.Builder
    public static class SelcomDebitResult {
        private boolean success;
        private String transactionId;
        private String error;
        private JsonNode rawResponse;
    }

    @lombok.Data
    @lombok.Builder
    public static class SelcomCreateAccountResult {
        private boolean success;
        private String accountId;
        private String accountNumber;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class SelcomCreditResult {
        private boolean success;
        private String transactionId;
        private String error;
        private JsonNode rawResponse;
    }
}

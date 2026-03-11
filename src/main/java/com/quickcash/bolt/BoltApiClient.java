package com.quickcash.bolt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bolt Partner API client (PROJECT_BOLT.md §7.1).
 * Create job, get job status, cancel job. When api-key is empty, stubs success for dev/test.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BoltApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.bolt.api-url:https://api.bolt.com}")
    private String apiUrl;

    @Value("${app.bolt.api-key:}")
    private String apiKey;

    @Value("${app.bolt.default-pickup-lat:-6.792354}")
    private double defaultPickupLat;
    @Value("${app.bolt.default-pickup-lng:39.208328}")
    private double defaultPickupLng;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Create Bolt job for a cash request (uses default pickup = float point; delivery = client).
     */
    public BoltCreateJobResult createJobForRequest(com.quickcash.domain.CashRequest request) {
        double deliveryLat = request.getDeliveryLat() != null ? request.getDeliveryLat() : request.getUserLatitude();
        double deliveryLng = request.getDeliveryLng() != null ? request.getDeliveryLng() : request.getUserLongitude();
        return createJob(
                request.getId(),
                defaultPickupLat, defaultPickupLng, "Quick Cash Float Point",
                deliveryLat, deliveryLng, request.getDeliveryAddress(),
                request.getClientName(), null,
                request.getRequestedAmount() != null ? request.getRequestedAmount() : java.math.BigDecimal.ZERO,
                request.getServiceFee() != null ? request.getServiceFee() : java.math.BigDecimal.ZERO,
                request.getTransportFee() != null ? request.getTransportFee() : java.math.BigDecimal.ZERO);
    }

    /**
     * Create Quick Cash delivery job. reference_id = cash_request.id for idempotency.
     */
    public BoltCreateJobResult createJob(UUID requestId, double pickupLat, double pickupLng, String pickupAddress,
                                         double deliveryLat, double deliveryLng, String deliveryAddress,
                                         String contactName, String contactPhone,
                                         BigDecimal cashAmountTzs, BigDecimal serviceFeeTzs, BigDecimal deliveryFeeTzs) {
        if (!isConfigured()) {
            String stubJobId = "bolt-stub-" + requestId.toString().replace("-", "").substring(0, 16);
            log.debug("Bolt not configured; stubbing createJob success for requestId={}, jobId={}", requestId, stubJobId);
            return BoltCreateJobResult.builder()
                    .success(true)
                    .jobId(stubJobId)
                    .status("PENDING")
                    .trackingUrl(null)
                    .build();
        }
        try {
            String path = "/v1/partners/quickcash/jobs";
            ObjectNode body = objectMapper.createObjectNode()
                    .put("reference_id", "qc_req_" + requestId)
                    .put("cash_amount_tzs", cashAmountTzs.doubleValue());
            body.set("pickup", objectMapper.createObjectNode()
                    .put("latitude", pickupLat)
                    .put("longitude", pickupLng)
                    .put("address", pickupAddress != null ? pickupAddress : "Quick Cash Float Point")
                    .put("instructions", "Collect TZS " + cashAmountTzs.toPlainString() + " for job qc_req_" + requestId));
            body.set("delivery", objectMapper.createObjectNode()
                    .put("latitude", deliveryLat)
                    .put("longitude", deliveryLng)
                    .put("address", deliveryAddress != null ? deliveryAddress : "Client location")
                    .put("contact_name", contactName != null ? contactName : "Client")
                    .put("contact_phone", contactPhone != null ? contactPhone : ""));
            body.set("metadata", objectMapper.createObjectNode()
                    .put("client_request_id", requestId.toString())
                    .put("service_fee_tzs", serviceFeeTzs != null ? serviceFeeTzs.doubleValue() : 0)
                    .put("delivery_fee_tzs", deliveryFeeTzs != null ? deliveryFeeTzs.doubleValue() : 0));
            JsonNode resp = post(path, body);
            String jobId = resp != null && resp.has("job_id") ? resp.path("job_id").asText() : null;
            String status = resp != null && resp.has("status") ? resp.path("status").asText() : "PENDING";
            String trackingUrl = resp != null && resp.has("tracking_url") ? resp.path("tracking_url").asText(null) : null;
            log.info("Bolt createJob: requestId={}, jobId={}, status={}", requestId, jobId, status);
            return BoltCreateJobResult.builder().success(true).jobId(jobId).status(status).trackingUrl(trackingUrl).build();
        } catch (Exception e) {
            log.warn("Bolt createJob failed: requestId={}, error={}", requestId, e.getMessage());
            return BoltCreateJobResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    /**
     * Get job status and optional tracking URL.
     */
    public BoltJobResult getJob(String jobId) {
        if (!isConfigured()) {
            return BoltJobResult.builder().found(true).status("PENDING").trackingUrl(null).build();
        }
        try {
            String path = "/v1/partners/quickcash/jobs/" + jobId;
            JsonNode resp = get(path);
            if (resp == null) return BoltJobResult.builder().found(false).build();
            String status = resp.has("status") ? resp.path("status").asText() : null;
            String trackingUrl = resp.has("tracking_url") ? resp.path("tracking_url").asText(null) : null;
            return BoltJobResult.builder().found(true).status(status).trackingUrl(trackingUrl).build();
        } catch (Exception e) {
            log.warn("Bolt getJob failed: jobId={}, error={}", jobId, e.getMessage());
            return BoltJobResult.builder().found(false).error(e.getMessage()).build();
        }
    }

    /**
     * Cancel job (e.g. client cancelled request).
     */
    public BoltCancelResult cancelJob(String jobId) {
        if (!isConfigured()) {
            log.debug("Bolt not configured; stubbing cancelJob success for jobId={}", jobId);
            return BoltCancelResult.builder().success(true).build();
        }
        try {
            String path = "/v1/partners/quickcash/jobs/" + jobId + "/cancel";
            post(path, objectMapper.createObjectNode());
            return BoltCancelResult.builder().success(true).build();
        } catch (Exception e) {
            log.warn("Bolt cancelJob failed: jobId={}, error={}", jobId, e.getMessage());
            return BoltCancelResult.builder().success(false).error(e.getMessage()).build();
        }
    }

    private JsonNode post(String path, JsonNode body) throws Exception {
        String url = apiUrl + path;
        String bodyStr = objectMapper.writeValueAsString(body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(bodyStr, headers), String.class);
        if (resp.getStatusCode().isError()) throw new RuntimeException("Bolt API error: " + resp.getStatusCode());
        return resp.getBody() != null ? objectMapper.readTree(resp.getBody()) : null;
    }

    private JsonNode get(String path) throws Exception {
        String url = apiUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (resp.getStatusCode().isError()) throw new RuntimeException("Bolt API error: " + resp.getStatusCode());
        return resp.getBody() != null ? objectMapper.readTree(resp.getBody()) : null;
    }

    @lombok.Data
    @lombok.Builder
    public static class BoltCreateJobResult {
        private boolean success;
        private String jobId;
        private String status;
        private String trackingUrl;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class BoltJobResult {
        private boolean found;
        private String status;
        private String trackingUrl;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class BoltCancelResult {
        private boolean success;
        private String error;
    }
}

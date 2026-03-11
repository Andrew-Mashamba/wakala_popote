package com.quickcash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.domain.SelcomCallbackRecord;
import com.quickcash.repository.SelcomCallbackRecordRepository;
import com.quickcash.selcom.SelcomApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Selcom webhook: signature verification and idempotency. Process each order_id once.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SelcomWebhookService {

    private final SelcomApiClient selcomApiClient;
    private final SelcomCallbackRecordRepository callbackRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verify signature (Digest header = HMAC-SHA256(vendor-secret, rawBody)). When vendor-secret not set, allows through.
     */
    public boolean verifySignature(String rawBody, String digestHeader) {
        return selcomApiClient.verifyWebhookSignature(rawBody, digestHeader);
    }

    /**
     * If order_id already processed, returns true (idempotent: treat as success). Otherwise saves record and returns false (caller should process).
     */
    @Transactional
    public boolean alreadyProcessed(String orderId) {
        return callbackRecordRepository.findByOrderId(orderId).isPresent();
    }

    @Transactional
    public void markProcessed(String orderId, String transid, String result) {
        if (callbackRecordRepository.findByOrderId(orderId).isPresent()) {
            return;
        }
        SelcomCallbackRecord record = SelcomCallbackRecord.builder()
                .orderId(orderId)
                .transid(transid)
                .result(result)
                .build();
        callbackRecordRepository.save(record);
        log.info("Selcom callback recorded: orderId={}, transid={}, result={}", orderId, transid, result);
    }

    @SuppressWarnings("unchecked")
    public String getOrderIdFromPayload(String rawBody) {
        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            Object o = payload.get("order_id");
            if (o != null) return o.toString();
            o = payload.get("orderId");
            if (o != null) return o.toString();
            o = payload.get("reference");
            if (o != null) return o.toString();
        } catch (Exception e) {
            log.debug("Could not parse order_id from callback body: {}", e.getMessage());
        }
        return null;
    }
}

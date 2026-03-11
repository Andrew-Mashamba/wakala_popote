package com.quickcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.service.SelcomWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Selcom payment/callback webhook. Verifies Digest (HMAC-SHA256) and processes each order_id once (idempotency).
 */
@RestController
@RequestMapping("/api/v1/selcom")
@RequiredArgsConstructor
@Slf4j
public class SelcomWebhookController {

    private final SelcomWebhookService selcomWebhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestBody String rawBody,
                                                         @RequestHeader(value = "Digest", required = false) String digest,
                                                         @RequestHeader(value = "X-Selcom-Signature", required = false) String legacySignature) {
        String signature = digest != null ? digest : legacySignature;
        if (!selcomWebhookService.verifySignature(rawBody, signature)) {
            log.warn("Selcom callback signature verification failed");
            return ResponseEntity.status(401).body(Map.of("status", "invalid_signature"));
        }
        String orderId = selcomWebhookService.getOrderIdFromPayload(rawBody);
        if (orderId == null) orderId = "unknown-" + System.currentTimeMillis();
        if (selcomWebhookService.alreadyProcessed(orderId)) {
            log.debug("Selcom callback idempotent: orderId={} already processed", orderId);
            return ResponseEntity.ok(Map.of("status", "received"));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            String transid = payload.get("transid") != null ? payload.get("transid").toString() : null;
            String result = payload.get("result") != null ? payload.get("result").toString() : null;
            selcomWebhookService.markProcessed(orderId, transid, result);
        } catch (Exception e) {
            log.debug("Parse callback body: {}", e.getMessage());
            selcomWebhookService.markProcessed(orderId, null, null);
        }
        log.info("Selcom callback received: orderId={}", orderId);
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}

package com.quickcash.controller;

import com.quickcash.service.BoltWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Bolt partner webhooks (PROJECT_BOLT.md §7.1).
 * Receives job.accepted, job.completed, job.cancelled; updates cash_request and creates settlement on completed.
 */
@RestController
@RequestMapping("/api/v1/bolt")
@RequiredArgsConstructor
@Slf4j
public class BoltWebhookController {

    private final BoltWebhookService boltWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody Map<String, Object> payload,
                                                       @RequestHeader(value = "X-Bolt-Signature", required = false) String signature) {
        log.info("Bolt webhook received: signaturePresent={}, keys={}", signature != null, payload.keySet());
        // TODO: verify HMAC with app.bolt.webhook-secret when set
        boltWebhookService.processWebhook(payload);
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}

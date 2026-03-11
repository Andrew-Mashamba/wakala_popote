package com.quickcash.controller;

import com.quickcash.domain.CashRequest;
import com.quickcash.dto.RequestCashRequest;
import com.quickcash.service.CashRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cash request endpoints. Path matches existing Flutter app (visa_agent).
 * See PROJECT.md for full /api/v1/cash/... spec.
 */
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashRequestService cashRequestService;

    /**
     * Request cash delivery. Creates request and notifies agents (or triggers Bolt job when integrated).
     */
    @PostMapping("/requestCash")
    public ResponseEntity<Map<String, Object>> requestCash(@RequestBody @Valid RequestCashRequest request) {
        CashRequest created = cashRequestService.createRequest(request);
        return ResponseEntity.ok(Map.of(
                "id", created.getId().toString(),
                "status", created.getStatus().name(),
                "requestedAmount", created.getRequestedAmount()
        ));
    }
}

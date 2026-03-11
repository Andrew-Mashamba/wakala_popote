package com.quickcash.controller;

import com.quickcash.domain.Settlement;
import com.quickcash.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bolt settlement list and export (Phase 3: settlement to Bolt).
 * GET /api/v1/bolt/settlements and GET /api/v1/bolt/settlements/export.
 */
@RestController
@RequestMapping("/api/v1/bolt")
@RequiredArgsConstructor
@Slf4j
public class BoltSettlementController {

    private final SettlementService settlementService;

    @GetMapping("/settlements")
    public ResponseEntity<List<Map<String, Object>>> listPending(
            @RequestParam(defaultValue = "100") int limit) {
        List<Settlement> list = settlementService.listBoltSettlementsPending(Math.min(limit, 500));
        List<Map<String, Object>> body = list.stream()
                .map(s -> Map.<String, Object>of(
                        "settlementId", s.getId().toString(),
                        "requestId", s.getRequest().getId().toString(),
                        "boltJobId", s.getBoltJobId() != null ? s.getBoltJobId() : "",
                        "boltPayoutAmount", s.getBoltPayoutAmount() != null ? s.getBoltPayoutAmount() : 0,
                        "createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""))
                .collect(Collectors.toList());
        log.info("Listed {} Bolt settlements (pending)", list.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/settlements/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam(defaultValue = "500") int limit) {
        String csv = settlementService.exportBoltSettlementsCsv(Math.min(limit, 2000));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "bolt-settlements.csv");
        log.info("Exported Bolt settlements CSV (limit={})", limit);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}

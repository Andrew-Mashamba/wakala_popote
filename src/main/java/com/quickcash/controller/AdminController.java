package com.quickcash.controller;

import com.quickcash.domain.*;
import com.quickcash.dto.DepositResponse;
import com.quickcash.dto.KycApplicationResponse;
import com.quickcash.service.AdminService;
import com.quickcash.service.AuditLogService;
import com.quickcash.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin APIs. Require X-Admin-API-Key header. Logging via AdminService (admin.log).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;

    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> listRequests(@RequestParam(defaultValue = "50") int limit) {
        var list = adminService.listRequests(limit).stream()
                .map(AdminController::cashRequestToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/deposits")
    public ResponseEntity<List<DepositResponse>> listDeposits(@RequestParam(defaultValue = "50") int limit) {
        var list = adminService.listDeposits(limit).stream()
                .map(AdminController::depositToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<Map<String, Object>>> listSettlements(@RequestParam(defaultValue = "50") int limit) {
        var list = adminService.listSettlements(limit).stream()
                .map(AdminController::settlementToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/agents")
    public ResponseEntity<List<Map<String, Object>>> listAgents(@RequestParam(defaultValue = "50") int limit) {
        var list = adminService.listAgents(limit).stream()
                .map(AdminController::agentToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/agents/{id}")
    public ResponseEntity<Map<String, Object>> getAgent(@PathVariable UUID id) {
        Agent a = adminService.getAgent(id);
        return ResponseEntity.ok(agentToMap(a));
    }

    @PostMapping("/agents/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyAgent(@PathVariable UUID id) {
        Agent a = adminService.verifyAgent(id);
        return ResponseEntity.ok(agentToMap(a));
    }

    @PostMapping("/agents/{id}/suspend")
    public ResponseEntity<Map<String, Object>> suspendAgent(@PathVariable UUID id) {
        Agent a = adminService.suspendAgent(id);
        return ResponseEntity.ok(agentToMap(a));
    }

    @PostMapping("/agents/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateAgent(@PathVariable UUID id) {
        Agent a = adminService.activateAgent(id);
        return ResponseEntity.ok(agentToMap(a));
    }

    @PutMapping("/agents/{id}/tier")
    public ResponseEntity<Map<String, Object>> setAgentTier(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String tier = body.get("tier");
        Agent a = adminService.setAgentTier(id, tier);
        return ResponseEntity.ok(agentToMap(a));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<KycApplicationResponse>> listApplications(@RequestParam(defaultValue = "50") int limit) {
        var list = adminService.listApplications(limit).stream()
                .map(AdminController::applicationToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<KycApplicationResponse> getApplication(@PathVariable UUID id) {
        var app = adminService.getApplication(id);
        return ResponseEntity.ok(applicationToResponse(app));
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<KycApplicationResponse> approveApplication(@PathVariable UUID id) {
        var app = adminService.approveApplication(id);
        return ResponseEntity.ok(applicationToResponse(app));
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<KycApplicationResponse> rejectApplication(@PathVariable UUID id,
                                                                    @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        var app = adminService.rejectApplication(id, reason);
        return ResponseEntity.ok(applicationToResponse(app));
    }

    @PostMapping("/applications/{id}/manual-review")
    public ResponseEntity<KycApplicationResponse> manualReviewApplication(@PathVariable UUID id) {
        var app = adminService.manualReviewApplication(id);
        return ResponseEntity.ok(applicationToResponse(app));
    }

    @GetMapping("/compliance/flags")
    public ResponseEntity<List<Map<String, Object>>> listComplianceFlags() {
        var list = adminService.listComplianceFlags().stream()
                .map(AdminController::flagToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/compliance/clear/{id}")
    public ResponseEntity<Void> clearComplianceFlag(@PathVariable UUID id) {
        adminService.clearComplianceFlag(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fraud/alerts")
    public ResponseEntity<List<Map<String, Object>>> listFraudAlerts() {
        var list = adminService.listFraudAlerts().stream()
                .map(AdminController::flagToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/fraud/block/{id}")
    public ResponseEntity<Void> blockFraud(@PathVariable UUID id) {
        adminService.blockFraud(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>> reportsSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant fromInst = from != null && !from.isBlank() ? Instant.parse(from) : null;
        Instant toInst = to != null && !to.isBlank() ? Instant.parse(to) : null;
        return ResponseEntity.ok(reportService.getSummary(fromInst, toInst));
    }

    @GetMapping("/reports/export")
    public ResponseEntity<List<Map<String, String>>> reportsExport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1000") int limit) {
        Instant fromInst = from != null && !from.isBlank() ? Instant.parse(from) : null;
        Instant toInst = to != null && !to.isBlank() ? Instant.parse(to) : null;
        return ResponseEntity.ok(reportService.exportRequests(fromInst, toInst, Math.min(limit, 5000)));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<Map<String, Object>>> audit(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(defaultValue = "100") int limit) {
        List<AuditLog> list;
        if (entityType != null && entityId != null) {
            list = auditLogService.findByEntity(entityType, entityId, limit).getContent();
        } else if (actorId != null) {
            list = auditLogService.findByActor(actorId, limit).getContent();
        } else {
            Instant fromInst = from != null && !from.isBlank() ? Instant.parse(from) : Instant.now().minusSeconds(86400 * 7);
            Instant toInst = to != null && !to.isBlank() ? Instant.parse(to) : Instant.now();
            list = auditLogService.findByDateRange(fromInst, toInst, limit);
        }
        var result = list.stream().map(a -> Map.<String, Object>of(
                "id", a.getId().toString(),
                "action", a.getAction() != null ? a.getAction() : "",
                "entityType", a.getEntityType() != null ? a.getEntityType() : "",
                "entityId", a.getEntityId() != null ? a.getEntityId() : "",
                "actorId", a.getActorId() != null ? a.getActorId() : "",
                "actorType", a.getActorType() != null ? a.getActorType() : "",
                "details", a.getDetails() != null ? a.getDetails() : "",
                "createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private static Map<String, Object> cashRequestToMap(CashRequest r) {
        return Map.of(
                "id", r.getId().toString(),
                "status", r.getStatus().name(),
                "requestedAmount", r.getRequestedAmount() != null ? r.getRequestedAmount() : 0,
                "userId", r.getUser() != null ? r.getUser().getId().toString() : ""
        );
    }

    private static DepositResponse depositToResponse(DepositRequest d) {
        return DepositResponse.builder()
                .id(d.getId())
                .status(d.getStatus().name())
                .destinationBankCode(d.getDestinationBankCode())
                .destinationAccountNumber(d.getDestinationAccountNumber())
                .cashAmount(d.getCashAmount())
                .netDepositAmount(d.getNetDepositAmount())
                .createdAt(d.getCreatedAt())
                .assignedAgentId(d.getAssignedAgent() != null ? d.getAssignedAgent().getId() : null)
                .build();
    }

    private static Map<String, Object> settlementToMap(Settlement s) {
        return Map.of(
                "id", s.getId().toString(),
                "requestId", s.getRequest() != null && s.getRequest().getId() != null ? s.getRequest().getId().toString() : "",
                "clientDebitStatus", s.getClientDebitStatus() != null ? s.getClientDebitStatus().name() : "",
                "boltSettlementStatus", s.getBoltSettlementStatus() != null ? s.getBoltSettlementStatus().name() : ""
        );
    }

    private static Map<String, Object> agentToMap(Agent a) {
        return Map.of(
                "id", a.getId().toString(),
                "selcomAccountId", a.getSelcomAccountId() != null ? a.getSelcomAccountId() : "",
                "agentTier", a.getAgentTier() != null ? a.getAgentTier().name() : "",
                "selcomAccountStatus", a.getSelcomAccountStatus() != null ? a.getSelcomAccountStatus().name() : "",
                "isAvailable", Boolean.TRUE.equals(a.getIsAvailable()),
                "totalDeliveries", a.getTotalDeliveries() != null ? a.getTotalDeliveries() : 0,
                "totalEarnings", a.getTotalEarnings() != null ? a.getTotalEarnings() : 0
        );
    }

    private static KycApplicationResponse applicationToResponse(SelcomAccountApplication app) {
        return KycApplicationResponse.builder()
                .id(app.getId())
                .status(app.getStatus().name())
                .fullName(app.getFullName())
                .dateOfBirth(app.getDateOfBirth())
                .nidaNumber(app.getNidaNumber())
                .phoneNumber(app.getPhoneNumber())
                .phoneVerified(Boolean.TRUE.equals(app.getPhoneVerified()))
                .nidaVerified(Boolean.TRUE.equals(app.getNidaVerified()))
                .createdAt(app.getCreatedAt())
                .submittedAt(app.getSubmittedAt())
                .rejectionReason(app.getRejectionReason())
                .selcomAccountId(app.getSelcomAccountId())
                .build();
    }

    private static Map<String, Object> flagToMap(AdminFlag f) {
        return Map.of(
                "id", f.getId().toString(),
                "flagType", f.getFlagType().name(),
                "entityType", f.getEntityType() != null ? f.getEntityType() : "",
                "entityId", f.getEntityId() != null ? f.getEntityId() : "",
                "reason", f.getReason() != null ? f.getReason() : "",
                "resolved", Boolean.TRUE.equals(f.getResolved()),
                "blocked", Boolean.TRUE.equals(f.getBlocked()),
                "createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : ""
        );
    }
}

package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.Agent;
import com.quickcash.domain.AgentAssignment;
import com.quickcash.domain.CashRequest;
import com.quickcash.domain.DepositRequest;
import com.quickcash.dto.AgentRegisterRequest;
import com.quickcash.dto.CashRequestResponseV1;
import com.quickcash.dto.DepositResponse;
import com.quickcash.service.AgentService;
import com.quickcash.service.CashRequestService;
import com.quickcash.service.DepositRequestService;
import com.quickcash.service.QrCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final CashRequestService cashRequestService;
    private final DepositRequestService depositRequestService;
    private final QrCollectionService qrCollectionService;

    @GetMapping("/qr/scan/{token}")
    public ResponseEntity<Map<String, Object>> qrScan(@CurrentUser UUID userId, @PathVariable String token) {
        var qr = qrCollectionService.scanByToken(token, userId);
        return ResponseEntity.ok(Map.of(
                "qrId", qr.getId().toString(),
                "requestId", qr.getCashRequest() != null ? qr.getCashRequest().getId().toString() : "",
                "amount", qr.getAmount(),
                "status", qr.getStatus().name()
        ));
    }

    @PostMapping("/qr/complete/{token}")
    public ResponseEntity<Map<String, Object>> qrComplete(@CurrentUser UUID userId, @PathVariable String token) {
        var qr = qrCollectionService.completeByToken(token, userId);
        return ResponseEntity.ok(Map.of("qrId", qr.getId().toString(), "status", qr.getStatus().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@CurrentUser UUID userId, @RequestBody @Valid AgentRegisterRequest request) {
        Agent agent = agentService.register(userId, request.getSelcomAccountId(), request.getSelcomAccountName());
        return ResponseEntity.ok(Map.of(
                "agentId", agent.getId().toString(),
                "selcomAccountId", agent.getSelcomAccountId()
        ));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(@CurrentUser UUID userId) {
        Agent agent = agentService.getByUserId(userId);
        return ResponseEntity.ok(Map.of(
                "agentId", agent.getId().toString(),
                "selcomAccountId", agent.getSelcomAccountId(),
                "isAvailable", agent.getIsAvailable() != null ? agent.getIsAvailable() : false,
                "totalDeliveries", agent.getTotalDeliveries() != null ? agent.getTotalDeliveries() : 0,
                "totalEarnings", agent.getTotalEarnings() != null ? agent.getTotalEarnings() : 0,
                "rating", agent.getRating() != null ? agent.getRating() : 5.0
        ));
    }

    @PutMapping("/availability")
    public ResponseEntity<Map<String, Boolean>> setAvailability(@CurrentUser UUID userId, @RequestBody Map<String, Boolean> body) {
        boolean available = Boolean.TRUE.equals(body.get("available"));
        agentService.setAvailability(userId, available);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PutMapping("/location")
    public ResponseEntity<Void> updateLocation(@CurrentUser UUID userId, @RequestBody Map<String, Double> body) {
        Double lat = body.get("latitude");
        Double lng = body.get("longitude");
        if (lat != null && lng != null) {
            agentService.updateLocation(userId, lat, lng);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/requests")
    public ResponseEntity<List<CashRequestResponseV1>> listRequests(@CurrentUser UUID userId, @RequestParam(defaultValue = "20") int limit) {
        var list = agentService.listAvailableRequests(userId, Math.min(limit, 50)).stream()
                .map(CashRequestV1Controller::toResponseStatic)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Map<String, String>> accept(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody(required = false) Map<String, Double> location) {
        Double lat = location != null ? location.get("latitude") : null;
        Double lng = location != null ? location.get("longitude") : null;
        AgentAssignment a = agentService.accept(userId, id, lat, lng);
        return ResponseEntity.ok(Map.of("assignmentId", a.getId().toString(), "requestId", id.toString()));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<Void> reject(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        agentService.reject(userId, id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/en-route")
    public ResponseEntity<Void> enRoute(@CurrentUser UUID userId, @PathVariable UUID id) {
        agentService.enRoute(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/arrived")
    public ResponseEntity<Void> arrived(@CurrentUser UUID userId, @PathVariable UUID id) {
        agentService.arrived(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/deliver")
    public ResponseEntity<Map<String, String>> deliver(@CurrentUser UUID userId, @PathVariable UUID id,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String recipientOtp = body != null ? body.get("otp") : null;
        agentService.deliver(userId, id, recipientOtp);
        return ResponseEntity.ok(Map.of("requestId", id.toString(), "status", "DELIVERED"));
    }

    @GetMapping("/earnings")
    public ResponseEntity<Map<String, Object>> earnings(@CurrentUser UUID userId, @RequestParam(defaultValue = "20") int limit) {
        Agent agent = agentService.getByUserId(userId);
        var history = agentService.listEarningsHistory(userId, Math.min(limit, 50)).stream()
                .map(e -> Map.<String, Object>of(
                        "requestId", e.getRequestId().toString(),
                        "assignmentId", e.getAssignmentId().toString(),
                        "amount", e.getAmount(),
                        "completedAt", e.getCompletedAt() != null ? e.getCompletedAt().toString() : null))
                .toList();
        return ResponseEntity.ok(Map.of(
                "totalEarnings", agent.getTotalEarnings() != null ? agent.getTotalEarnings() : 0,
                "totalDeliveries", agent.getTotalDeliveries() != null ? agent.getTotalDeliveries() : 0,
                "history", history
        ));
    }

    @GetMapping("/float")
    public ResponseEntity<Map<String, Object>> floatBalance(@CurrentUser UUID userId) {
        Agent agent = agentService.getByUserId(userId);
        return ResponseEntity.ok(Map.of("availableCash", agent.getAvailableCash() != null ? agent.getAvailableCash() : 0));
    }

    // --- Deposit (agent) APIs ---
    @GetMapping("/deposits")
    public ResponseEntity<List<DepositResponse>> listDeposits(@CurrentUser UUID userId, @RequestParam(defaultValue = "20") int limit) {
        var list = depositRequestService.listForAgent(userId, limit).stream()
                .map(AgentController::depositToResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/deposits/available")
    public ResponseEntity<List<DepositResponse>> listAvailableDeposits(@CurrentUser UUID userId, @RequestParam(defaultValue = "20") int limit) {
        var list = depositRequestService.listAvailableForAgent(userId, limit).stream()
                .map(AgentController::depositToResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/deposits/{id}")
    public ResponseEntity<DepositResponse> getDeposit(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.getByIdAndAgent(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    @PostMapping("/deposits/{id}/accept")
    public ResponseEntity<DepositResponse> acceptDeposit(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.agentAccept(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    @PostMapping("/deposits/{id}/reject")
    public ResponseEntity<Void> rejectDeposit(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        depositRequestService.agentReject(id, userId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deposits/{id}/en-route")
    public ResponseEntity<DepositResponse> depositEnRoute(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.agentEnRoute(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    @PostMapping("/deposits/{id}/arrived")
    public ResponseEntity<DepositResponse> depositArrived(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.agentArrived(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    @PostMapping("/deposits/{id}/collect")
    public ResponseEntity<DepositResponse> depositCollect(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.agentCollect(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    @PostMapping("/deposits/{id}/complete")
    public ResponseEntity<DepositResponse> depositComplete(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.agentComplete(id, userId);
        return ResponseEntity.ok(depositToResponse(d));
    }

    private static DepositResponse depositToResponse(DepositRequest d) {
        return DepositResponse.builder()
                .id(d.getId())
                .status(d.getStatus().name())
                .destinationBankCode(d.getDestinationBankCode())
                .destinationAccountNumber(d.getDestinationAccountNumber())
                .destinationAccountName(d.getDestinationAccountName())
                .cashAmount(d.getCashAmount())
                .serviceFee(d.getServiceFee())
                .netDepositAmount(d.getNetDepositAmount())
                .collectionAddress(d.getCollectionAddress())
                .collectionLat(d.getCollectionLat())
                .collectionLng(d.getCollectionLng())
                .assignedAgentId(d.getAssignedAgent() != null ? d.getAssignedAgent().getId() : null)
                .createdAt(d.getCreatedAt())
                .agentAssignedAt(d.getAgentAssignedAt())
                .cashCollectedAt(d.getCashCollectedAt())
                .completedAt(d.getCompletedAt())
                .failureReason(d.getFailureReason())
                .cancellationReason(d.getCancellationReason())
                .build();
    }
}

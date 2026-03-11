package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.DepositRequest;
import com.quickcash.dto.DepositRequestCreate;
import com.quickcash.dto.DepositResponse;
import com.quickcash.dto.DepositTrackResponse;
import com.quickcash.service.DepositRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositRequestService depositRequestService;

    @PostMapping("/request")
    public ResponseEntity<DepositResponse> create(@CurrentUser UUID userId, @RequestBody @Valid DepositRequestCreate request) {
        DepositRequest d = depositRequestService.create(userId, request);
        return ResponseEntity.ok(toResponse(d));
    }

    @GetMapping
    public ResponseEntity<List<DepositResponse>> list(@CurrentUser UUID userId,
                                                      @RequestParam(defaultValue = "20") int limit) {
        var list = depositRequestService.listByClient(userId, limit).stream()
                .map(DepositController::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositResponse> get(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.getByIdAndClient(id, userId);
        return ResponseEntity.ok(toResponse(d));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<DepositResponse> cancel(@CurrentUser UUID userId, @PathVariable UUID id,
                                                  @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        DepositRequest d = depositRequestService.cancel(id, userId, reason);
        return ResponseEntity.ok(toResponse(d));
    }

    @GetMapping("/{id}/track")
    public ResponseEntity<DepositTrackResponse> track(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositTrackResponse info = depositRequestService.getTrackInfo(id, userId);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/{id}/confirm-collection")
    public ResponseEntity<DepositResponse> confirmCollection(@CurrentUser UUID userId, @PathVariable UUID id) {
        DepositRequest d = depositRequestService.confirmCollection(id, userId);
        return ResponseEntity.ok(toResponse(d));
    }

    private static DepositResponse toResponse(DepositRequest d) {
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

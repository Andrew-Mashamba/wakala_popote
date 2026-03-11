package com.quickcash.controller;

import com.quickcash.auth.CurrentUser;
import com.quickcash.domain.CashRequest;
import com.quickcash.dto.CashRequestCreateV1;
import com.quickcash.dto.CashRequestResponseV1;
import com.quickcash.dto.QrGenerateResponse;
import com.quickcash.dto.SendCashRequest;
import com.quickcash.service.CashRequestService;
import com.quickcash.service.QrCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cash")
@RequiredArgsConstructor
public class CashRequestV1Controller {

    private final CashRequestService cashRequestService;
    private final QrCollectionService qrCollectionService;

    @PostMapping("/request")
    public ResponseEntity<CashRequestResponseV1> create(@CurrentUser UUID userId,
                                                        @RequestBody @Valid CashRequestCreateV1 request) {
        var created = cashRequestService.createRequestV1(userId, request);
        return ResponseEntity.ok(toResponse(created));
    }

    @PostMapping("/send")
    public ResponseEntity<CashRequestResponseV1> send(@CurrentUser UUID userId,
                                                      @RequestBody @Valid SendCashRequest request) {
        var created = cashRequestService.createSendRequest(userId, request);
        return ResponseEntity.ok(toResponse(created));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<CashRequestResponseV1>> list(@CurrentUser UUID userId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        var list = cashRequestService.listByUserId(userId.toString(), Math.min(limit, 100)).stream()
                .map(CashRequestV1Controller::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<CashRequestResponseV1> get(@CurrentUser UUID userId, @PathVariable UUID id) {
        var r = cashRequestService.getByIdAndUser(id, userId);
        return ResponseEntity.ok(toResponse(r));
    }

    @PostMapping("/requests/{id}/cancel")
    public ResponseEntity<CashRequestResponseV1> cancel(@CurrentUser UUID userId, @PathVariable UUID id) {
        var r = cashRequestService.cancel(id, userId);
        return ResponseEntity.ok(toResponse(r));
    }

    @PostMapping("/requests/{id}/collect")
    public ResponseEntity<CashRequestResponseV1> collect(@CurrentUser UUID userId, @PathVariable UUID id) {
        var r = cashRequestService.collectPayment(id, userId);
        return ResponseEntity.ok(toResponse(r));
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<QrGenerateResponse> qrGenerate(@CurrentUser UUID userId,
                                                          @RequestBody java.util.Map<String, Object> body) {
        java.math.BigDecimal amount = body.get("amount") != null ? new java.math.BigDecimal(body.get("amount").toString()) : null;
        UUID paymentMethodId = body.get("paymentMethodId") != null ? UUID.fromString(body.get("paymentMethodId").toString()) : null;
        Boolean collectNow = body.get("collectNow") != null && Boolean.TRUE.equals(Boolean.parseBoolean(body.get("collectNow").toString()));
        if (amount == null) throw new IllegalArgumentException("amount required");
        var qr = qrCollectionService.generate(userId, amount, paymentMethodId, collectNow);
        return ResponseEntity.ok(QrGenerateResponse.builder()
                .requestId(qr.getCashRequest() != null ? qr.getCashRequest().getId() : null)
                .qrToken(qr.getQrToken())
                .expiresAt(qr.getExpiresAt())
                .expiresInSeconds(30 * 60)
                .build());
    }

    @GetMapping("/requests/{id}/track")
    public ResponseEntity<TrackResponse> track(@CurrentUser UUID userId, @PathVariable UUID id) {
        var info = cashRequestService.getTrackInfo(id, userId);
        return ResponseEntity.ok(TrackResponse.builder()
                .requestId(info.getRequestId())
                .status(info.getStatus())
                .trackingUrl(info.getTrackingUrl())
                .agentLat(info.getAgentLat())
                .agentLng(info.getAgentLng())
                .assignmentStatus(info.getAssignmentStatus())
                .build());
    }

    /** Public for reuse from AgentController. */
    public static CashRequestResponseV1 toResponseStatic(CashRequest r) {
        return toResponse(r);
    }

    private static CashRequestResponseV1 toResponse(CashRequest r) {
        return CashRequestResponseV1.builder()
                .id(r.getId())
                .requestType(r.getRequestType())
                .status(r.getStatus().name())
                .principalAmount(r.getPrincipalAmount() != null ? r.getPrincipalAmount() : r.getRequestedAmount())
                .serviceFee(r.getServiceFee())
                .transportFee(r.getTransportFee())
                .totalClientCharge(r.getTotalClientCharge())
                .latitude(r.getUserLatitude())
                .longitude(r.getUserLongitude())
                .deliveryAddress(r.getDeliveryAddress())
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class TrackResponse {
        private UUID requestId;
        private String status;
        private String trackingUrl;
        private Double agentLat;
        private Double agentLng;
        private String assignmentStatus;
    }
}

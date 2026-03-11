package com.quickcash.controller;

import com.quickcash.domain.B2bBatch;
import com.quickcash.domain.B2bBatchItem;
import com.quickcash.dto.B2bDisbursementRequest;
import com.quickcash.service.B2bService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
public class B2bController {

    private final B2bService b2bService;

    @PostMapping("/disbursements")
    public ResponseEntity<B2bDisbursementResponse> createDisbursement(@RequestBody @Valid B2bDisbursementRequest request) {
        B2bBatch batch = b2bService.createDisbursementBatch(request);
        List<B2bBatchItem> items = b2bService.getBatchItems(batch.getId());
        return ResponseEntity.ok(B2bDisbursementResponse.builder()
                .batchId(batch.getId())
                .status(batch.getStatus().name())
                .itemCount(batch.getItemCount())
                .totalAmount(batch.getTotalAmount())
                .items(items.stream().map(i -> B2bItemResponse.builder()
                        .id(i.getId())
                        .recipientPhone(i.getRecipientPhone())
                        .amount(i.getAmount())
                        .reference(i.getReference())
                        .status(i.getStatus().name())
                        .requestId(i.getCashRequest() != null ? i.getCashRequest().getId() : null)
                        .build()).collect(Collectors.toList()))
                .build());
    }

    @GetMapping("/disbursements/{batchId}")
    public ResponseEntity<B2bDisbursementResponse> getBatch(@PathVariable UUID batchId) {
        B2bBatch batch = b2bService.getBatch(batchId);
        List<B2bBatchItem> items = b2bService.getBatchItems(batchId);
        return ResponseEntity.ok(B2bDisbursementResponse.builder()
                .batchId(batch.getId())
                .status(batch.getStatus().name())
                .itemCount(batch.getItemCount())
                .totalAmount(batch.getTotalAmount())
                .items(items.stream().map(i -> B2bItemResponse.builder()
                        .id(i.getId())
                        .recipientPhone(i.getRecipientPhone())
                        .amount(i.getAmount())
                        .reference(i.getReference())
                        .status(i.getStatus().name())
                        .requestId(i.getCashRequest() != null ? i.getCashRequest().getId() : null)
                        .build()).collect(Collectors.toList()))
                .build());
    }

    @lombok.Data
    @lombok.Builder
    public static class B2bDisbursementResponse {
        private UUID batchId;
        private String status;
        private Integer itemCount;
        private java.math.BigDecimal totalAmount;
        private List<B2bItemResponse> items;
    }

    @lombok.Data
    @lombok.Builder
    public static class B2bItemResponse {
        private UUID id;
        private String recipientPhone;
        private java.math.BigDecimal amount;
        private String reference;
        private String status;
        private UUID requestId;
    }
}

package com.quickcash.service;

import com.quickcash.domain.B2bBatch;
import com.quickcash.domain.B2bBatchItem;
import com.quickcash.domain.CashRequest;
import com.quickcash.dto.B2bDisbursementItem;
import com.quickcash.dto.B2bDisbursementRequest;
import com.quickcash.dto.SendCashRequest;
import com.quickcash.repository.B2bBatchItemRepository;
import com.quickcash.repository.B2bBatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * B2B bulk disbursement. Logs to b2b.log.
 */
@Service
@RequiredArgsConstructor
public class B2bService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.b2b");

    private final B2bBatchRepository batchRepository;
    private final B2bBatchItemRepository itemRepository;
    private final CashRequestService cashRequestService;

    @Transactional
    public B2bBatch createDisbursementBatch(B2bDisbursementRequest req) {
        B2bBatch batch = B2bBatch.builder()
                .businessId(req.getBusinessUserId().toString())
                .status(B2bBatch.BatchStatus.PROCESSING)
                .itemCount(req.getItems().size())
                .totalAmount(BigDecimal.ZERO)
                .build();
        batch = batchRepository.save(batch);
        BigDecimal total = BigDecimal.ZERO;
        int created = 0;
        List<B2bBatchItem> items = new ArrayList<>();
        for (B2bDisbursementItem dto : req.getItems()) {
            B2bBatchItem item = B2bBatchItem.builder()
                    .batch(batch)
                    .recipientPhone(dto.getRecipientPhone())
                    .recipientName(dto.getRecipientName())
                    .amount(dto.getAmount())
                    .reference(dto.getReference())
                    .status(B2bBatchItem.ItemStatus.PENDING)
                    .build();
            try {
                SendCashRequest sendReq = new SendCashRequest();
                sendReq.setRecipientPhone(dto.getRecipientPhone());
                sendReq.setRecipientName(dto.getRecipientName());
                sendReq.setAmount(dto.getAmount());
                sendReq.setDeliveryLatitude(dto.getDeliveryLatitude() != null ? dto.getDeliveryLatitude() : 0.0);
                sendReq.setDeliveryLongitude(dto.getDeliveryLongitude() != null ? dto.getDeliveryLongitude() : 0.0);
                sendReq.setPaymentMethodId(null);
                sendReq.setCollectNow(false);
                CashRequest cr = cashRequestService.createSendRequest(req.getBusinessUserId(), sendReq);
                item.setCashRequest(cr);
                item.setStatus(B2bBatchItem.ItemStatus.CREATED);
                total = total.add(dto.getAmount());
                created++;
            } catch (Exception e) {
                log.warn("B2B item failed: batchId={}, recipient={}, error={}", batch.getId(), dto.getRecipientPhone(), e.getMessage());
                item.setStatus(B2bBatchItem.ItemStatus.FAILED);
            }
            items.add(itemRepository.save(item));
        }
        batch.setTotalAmount(total);
        batch.setStatus(created == req.getItems().size() ? B2bBatch.BatchStatus.COMPLETED : B2bBatch.BatchStatus.PARTIAL_FAILED);
        batch = batchRepository.save(batch);
        log.info("B2B batch created: batchId={}, businessUserId={}, items={}, created={}, totalAmount={}",
                batch.getId(), req.getBusinessUserId(), req.getItems().size(), created, total);
        return batch;
    }

    public B2bBatch getBatch(UUID batchId) {
        return batchRepository.findById(batchId).orElseThrow(() -> new com.quickcash.exception.ResourceNotFoundException("B2bBatch", batchId.toString()));
    }

    public List<B2bBatchItem> getBatchItems(UUID batchId) {
        B2bBatch batch = getBatch(batchId);
        return itemRepository.findByBatchOrderById(batch);
    }
}

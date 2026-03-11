package com.quickcash.service;

import com.quickcash.domain.B2bBatch;
import com.quickcash.dto.B2bDisbursementItem;
import com.quickcash.dto.B2bDisbursementRequest;
import com.quickcash.repository.B2bBatchItemRepository;
import com.quickcash.repository.B2bBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class B2bServiceTest {

    @Mock
    B2bBatchRepository batchRepository;
    @Mock
    B2bBatchItemRepository itemRepository;
    @Mock
    CashRequestService cashRequestService;

    @InjectMocks
    B2bService b2bService;

    @Test
    void createDisbursementBatch_creates_batch_and_items() {
        UUID businessUserId = UUID.randomUUID();
        B2bDisbursementItem item = new B2bDisbursementItem();
        item.setRecipientPhone("255712345678");
        item.setRecipientName("Worker");
        item.setAmount(new BigDecimal("50000"));
        B2bDisbursementRequest req = new B2bDisbursementRequest();
        req.setBusinessUserId(businessUserId);
        req.setItems(List.of(item));

        when(batchRepository.save(any(B2bBatch.class))).thenAnswer(i -> {
            B2bBatch b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(cashRequestService.createSendRequest(eq(businessUserId), any())).thenReturn(
                com.quickcash.domain.CashRequest.builder().id(UUID.randomUUID()).build());
        when(itemRepository.save(any(com.quickcash.domain.B2bBatchItem.class))).thenAnswer(i -> i.getArgument(0));

        B2bBatch result = b2bService.createDisbursementBatch(req);

        assertThat(result.getItemCount()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(B2bBatch.BatchStatus.COMPLETED);
        verify(batchRepository, atLeastOnce()).save(any(B2bBatch.class));
        verify(cashRequestService).createSendRequest(eq(businessUserId), any());
    }
}

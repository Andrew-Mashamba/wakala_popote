package com.quickcash.service;

import com.quickcash.domain.IdempotencyRecord;
import com.quickcash.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    IdempotencyRecordRepository repository;

    @InjectMocks
    IdempotencyService idempotencyService;

    @Test
    void findValid_returns_empty_when_not_found() {
        when(repository.findByIdempotencyKeyAndUserId("key1", "user1")).thenReturn(Optional.empty());

        Optional<IdempotencyRecord> result = idempotencyService.findValid("key1", "user1");

        assertThat(result).isEmpty();
    }

    @Test
    void findValid_returns_empty_when_expired() {
        IdempotencyRecord expired = IdempotencyRecord.builder()
                .idempotencyKey("key1")
                .userId("user1")
                .responseStatus(200)
                .responseBody("{}")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(repository.findByIdempotencyKeyAndUserId("key1", "user1")).thenReturn(Optional.of(expired));

        Optional<IdempotencyRecord> result = idempotencyService.findValid("key1", "user1");

        assertThat(result).isEmpty();
    }

    @Test
    void findValid_returns_record_when_not_expired() {
        IdempotencyRecord valid = IdempotencyRecord.builder()
                .idempotencyKey("key1")
                .userId("user1")
                .responseStatus(200)
                .responseBody("{\"id\":\"x\"}")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(repository.findByIdempotencyKeyAndUserId("key1", "user1")).thenReturn(Optional.of(valid));

        Optional<IdempotencyRecord> result = idempotencyService.findValid("key1", "user1");

        assertThat(result).hasValue(valid);
    }

    @Test
    void save_persists_record_with_24h_ttl() {
        IdempotencyRecord saved = IdempotencyRecord.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("key1")
                .userId("user1")
                .responseStatus(201)
                .responseBody("{\"id\":\"123\"}")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(24 * 3600))
                .build();
        when(repository.save(any(IdempotencyRecord.class))).thenReturn(saved);

        IdempotencyRecord result = idempotencyService.save("key1", "user1", 201, "{\"id\":\"123\"}");

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<IdempotencyRecord> cap = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(repository).save(cap.capture());
        IdempotencyRecord captured = cap.getValue();
        assertThat(captured.getIdempotencyKey()).isEqualTo("key1");
        assertThat(captured.getUserId()).isEqualTo("user1");
        assertThat(captured.getResponseStatus()).isEqualTo(201);
        assertThat(captured.getResponseBody()).isEqualTo("{\"id\":\"123\"}");
        assertThat(captured.getExpiresAt()).isAfter(Instant.now());
    }
}

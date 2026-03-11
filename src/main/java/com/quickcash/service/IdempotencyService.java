package com.quickcash.service;

import com.quickcash.domain.IdempotencyRecord;
import com.quickcash.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Idempotency key handling for cash request and send. Logs to idempotency.log.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.idempotency");
    private static final int TTL_HOURS = 24;

    private final IdempotencyRecordRepository repository;

    public Optional<IdempotencyRecord> findValid(String key, String userId) {
        return repository.findByIdempotencyKeyAndUserId(key, userId)
                .filter(r -> r.getExpiresAt() != null && Instant.now().isBefore(r.getExpiresAt()));
    }

    @Transactional
    public IdempotencyRecord save(String key, String userId, int responseStatus, String responseBody) {
        Instant expiresAt = Instant.now().plusSeconds(TTL_HOURS * 3600L);
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .userId(userId)
                .responseStatus(responseStatus)
                .responseBody(responseBody != null ? responseBody : "")
                .expiresAt(expiresAt)
                .build();
        record = repository.save(record);
        log.info("Idempotency saved: key={}, userId={}, status={}", key, userId, responseStatus);
        return record;
    }
}

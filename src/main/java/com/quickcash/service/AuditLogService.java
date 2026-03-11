package com.quickcash.service;

import com.quickcash.domain.AuditLog;
import com.quickcash.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit trail for key actions. Logs to audit.log.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.audit");

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog log(String action, String entityType, String entityId, String actorId, String actorType, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actorId(actorId)
                .actorType(actorType != null ? actorType : "USER")
                .details(details)
                .ipAddress(ipAddress)
                .build();
        entry = auditLogRepository.save(entry);
        log.info("audit: action={}, entity={}/{}, actor={}/{}", action, entityType, entityId, actorId, actorType);
        return entry;
    }

    public AuditLog log(String action, String entityType, UUID entityId, UUID actorId, String actorType, String details) {
        return log(action, entityType, entityId != null ? entityId.toString() : null,
                actorId != null ? actorId.toString() : null, actorType, details, null);
    }

    public Page<AuditLog> findByEntity(String entityType, String entityId, int limit) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, PageRequest.of(0, limit));
    }

    public Page<AuditLog> findByActor(String actorId, int limit) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, PageRequest.of(0, limit));
    }

    public List<AuditLog> findByDateRange(Instant from, Instant to, int limit) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, PageRequest.of(0, limit)).getContent();
    }
}

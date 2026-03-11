package com.quickcash.service;

import com.quickcash.domain.AuditLog;
import com.quickcash.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    AuditLogRepository auditLogRepository;

    @InjectMocks
    AuditLogService auditLogService;

    @Test
    void log_saves_entry_and_returns_it() {
        AuditLog saved = AuditLog.builder().id(UUID.randomUUID()).action("TEST").build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        AuditLog result = auditLogService.log("TEST", "CashRequest", "e1", "a1", "USER", "details", "127.0.0.1");

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("TEST");
        assertThat(cap.getValue().getEntityType()).isEqualTo("CashRequest");
        assertThat(cap.getValue().getEntityId()).isEqualTo("e1");
        assertThat(cap.getValue().getActorId()).isEqualTo("a1");
        assertThat(cap.getValue().getActorType()).isEqualTo("USER");
        assertThat(cap.getValue().getDetails()).isEqualTo("details");
        assertThat(cap.getValue().getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void log_with_uuid_entity_and_actor_calls_string_overload() {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AuditLog saved = AuditLog.builder().id(UUID.randomUUID()).build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        auditLogService.log("ACTION", "Entity", entityId, actorId, "ADMIN", "d");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getEntityId()).isEqualTo(entityId.toString());
        assertThat(cap.getValue().getActorId()).isEqualTo(actorId.toString());
    }

    @Test
    void findByEntity_delegates_to_repository() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("CashRequest", "id1", pr)).thenReturn(page);

        Page<AuditLog> result = auditLogService.findByEntity("CashRequest", "id1", 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    void findByActor_delegates_to_repository() {
        PageRequest pr = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByActorIdOrderByCreatedAtDesc("actor1", pr)).thenReturn(page);

        Page<AuditLog> result = auditLogService.findByActor("actor1", 20);

        assertThat(result).isSameAs(page);
    }

    @Test
    void findByDateRange_returns_content() {
        Instant from = Instant.EPOCH;
        Instant to = Instant.now();
        PageRequest pr = PageRequest.of(0, 50);
        AuditLog entry = AuditLog.builder().id(UUID.randomUUID()).action("A").build();
        when(auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(eq(from), eq(to), eq(pr)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        List<AuditLog> result = auditLogService.findByDateRange(from, to, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("A");
    }
}

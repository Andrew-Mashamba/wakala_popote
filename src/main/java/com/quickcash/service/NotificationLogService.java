package com.quickcash.service;

import com.quickcash.domain.NotificationLog;
import com.quickcash.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public NotificationLog log(UUID userId, UUID requestId, NotificationLog.Channel channel,
                               String notificationType, String title, String body, String data,
                               NotificationLog.NotificationStatus status, String error) {
        var entry = NotificationLog.builder()
                .userId(userId)
                .requestId(requestId)
                .channel(channel)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .data(data)
                .status(status)
                .error(error)
                .sentAt(status == NotificationLog.NotificationStatus.SENT || status == NotificationLog.NotificationStatus.DELIVERED ? Instant.now() : null)
                .build();
        entry = notificationLogRepository.save(entry);
        log.debug("Notification logged: id={}, channel={}, type={}, status={}", entry.getId(), channel, notificationType, status);
        return entry;
    }

    @Transactional
    public void markSent(UUID logId, boolean delivered) {
        notificationLogRepository.findById(logId).ifPresent(entry -> {
            entry.setStatus(delivered ? NotificationLog.NotificationStatus.DELIVERED : NotificationLog.NotificationStatus.SENT);
            entry.setSentAt(Instant.now());
            if (delivered) entry.setDeliveredAt(Instant.now());
            notificationLogRepository.save(entry);
        });
    }

    @Transactional
    public void markFailed(UUID logId, String error) {
        notificationLogRepository.findById(logId).ifPresent(entry -> {
            entry.setStatus(NotificationLog.NotificationStatus.FAILED);
            entry.setError(error);
            notificationLogRepository.save(entry);
            log.warn("Notification failed: id={}, error={}", logId, error);
        });
    }
}

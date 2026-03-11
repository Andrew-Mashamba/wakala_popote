package com.quickcash.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.quickcash.domain.NotificationLog;
import com.quickcash.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Sends FCM messages and logs to notification_log. When Firebase is not initialized, logs only (no send).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationService {

    private final NotificationLogService notificationLogService;

    /**
     * Send FCM to a single token and log result.
     */
    public boolean sendToToken(UUID userId, UUID requestId, String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM skipped: no token for userId={}", userId);
            return false;
        }
        var logEntry = notificationLogService.log(userId, requestId, NotificationLog.Channel.FCM,
                "cash_request", title, body, data != null ? data.toString() : null,
                NotificationLog.NotificationStatus.PENDING, null);
        boolean ok = doSend(fcmToken, title, body, data, logEntry.getId());
        if (ok) {
            notificationLogService.markSent(logEntry.getId(), false);
        } else {
            notificationLogService.markFailed(logEntry.getId(), "Send failed or Firebase not configured");
        }
        return ok;
    }

    private boolean doSend(String token, String title, String body, Map<String, String> data, UUID logId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("FCM not configured; notification logged only (logId={})", logId);
            return false;
        }
        try {
            var builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build());
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }
            String msgId = FirebaseMessaging.getInstance().send(builder.build());
            log.info("FCM sent: messageId={}, logId={}", msgId, logId);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed: token={}..., error={}", token.substring(0, Math.min(10, token.length())), e.getMessage());
            return false;
        }
    }
}

package com.quickcash.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS OTP delivery (e.g. Africa's Talking, Twilio). Stub when not configured. Logs to sms.log.
 */
@Service
public class SmsOtpService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.sms");

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:}")
    private String provider;

    /**
     * Send OTP or message to phone. When SMS is not configured, logs only (stub).
     *
     * @param phoneNumber E.164 or national format (e.g. 255712345678)
     * @param message     Plain text (e.g. "Your Quick Cash OTP: 847291")
     * @return true if sent (or stub), false on provider failure
     */
    public boolean sendOtp(String phoneNumber, String message) {
        if (!smsEnabled || provider == null || provider.isBlank()) {
            log.info("SMS stub: would send to {} message length={} (configure app.sms.enabled and app.sms.provider for real SMS)",
                    maskPhone(phoneNumber), message != null ? message.length() : 0);
            return true;
        }
        try {
            boolean ok = doSend(phoneNumber, message);
            log.info("SMS sent: to={}, success={}, provider={}", maskPhone(phoneNumber), ok, provider);
            return ok;
        } catch (Exception e) {
            log.warn("SMS send failed: to={}, error={}", maskPhone(phoneNumber), e.getMessage());
            return false;
        }
    }

    private boolean doSend(String phoneNumber, String message) {
        // Placeholder for Africa's Talking / Twilio API call
        return true;
    }

    private static String maskPhone(String s) {
        if (s == null || s.length() < 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }
}

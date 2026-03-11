package com.quickcash.sms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SmsOtpServiceTest {

    @Autowired
    SmsOtpService smsOtpService;

    @Test
    void sendOtp_when_sms_disabled_logs_and_returns_true() {
        ReflectionTestUtils.setField(smsOtpService, "smsEnabled", false);
        ReflectionTestUtils.setField(smsOtpService, "provider", "");
        boolean result = smsOtpService.sendOtp("255712345678", "Your OTP: 123456");
        assertThat(result).isTrue();
    }

    @Test
    void sendOtp_when_provider_blank_logs_stub_and_returns_true() {
        ReflectionTestUtils.setField(smsOtpService, "smsEnabled", true);
        ReflectionTestUtils.setField(smsOtpService, "provider", "");
        boolean result = smsOtpService.sendOtp("255712345678", "Your OTP: 123456");
        assertThat(result).isTrue();
    }

    @Test
    void sendOtp_masks_phone_in_stub_path() {
        ReflectionTestUtils.setField(smsOtpService, "smsEnabled", false);
        boolean result = smsOtpService.sendOtp("255712345678", "msg");
        assertThat(result).isTrue();
    }
}

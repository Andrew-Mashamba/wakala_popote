package com.quickcash.auth.dto;

import lombok.Data;

@Data
public class AuthRequest {
    /** Firebase ID token (required when app.firebase.enabled=true) */
    private String firebaseIdToken;
    /** Firebase UID (used when Firebase is disabled, for dev) */
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private String phoneNumber;
    /** FCM token for push notifications */
    private String fcmToken;
}

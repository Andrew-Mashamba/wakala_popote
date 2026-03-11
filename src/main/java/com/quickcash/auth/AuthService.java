package com.quickcash.auth;

import com.quickcash.auth.dto.AuthRequest;
import com.quickcash.auth.dto.AuthResponse;
import com.quickcash.domain.User;
import com.quickcash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Register or login: verify Firebase token (if enabled) or accept uid, create/update user, return userId + JWT.
     */
    public AuthResponse registerOrLogin(AuthRequest request) {
        String uid = resolveUid(request);
        User user = userRepository.findByUid(uid)
                .map(existing -> updateExistingUser(existing, request))
                .orElseGet(() -> createNewUser(uid, request));
        user = userRepository.save(user);
        String token = jwtService.createToken(user.getId(), user.getUid());
        return AuthResponse.builder()
                .userId(user.getId().toString())
                .accessToken(token)
                .expiresInMs(expirationMs)
                .build();
    }

    private String resolveUid(AuthRequest request) {
        if (request.getFirebaseIdToken() != null && !request.getFirebaseIdToken().isBlank()) {
            String verified = firebaseTokenVerifier.verifyIdToken(request.getFirebaseIdToken());
            if (verified != null) return verified;
        }
        if (request.getUid() != null && !request.getUid().isBlank()) {
            return request.getUid();
        }
        throw new IllegalArgumentException("Either firebaseIdToken or uid is required");
    }

    private User updateExistingUser(User user, AuthRequest request) {
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getFcmToken() != null) user.setFcmToken(request.getFcmToken());
        return user;
    }

    private User createNewUser(String uid, AuthRequest request) {
        return User.builder()
                .uid(uid)
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .photoUrl(request.getPhotoUrl())
                .phoneNumber(request.getPhoneNumber())
                .fcmToken(request.getFcmToken())
                .build();
    }
}

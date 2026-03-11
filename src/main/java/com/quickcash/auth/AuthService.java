package com.quickcash.auth;

import com.quickcash.auth.dto.AuthRequest;
import com.quickcash.auth.dto.AuthResponse;
import com.quickcash.domain.User;
import com.quickcash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    /**
     * Register with phone, name, NIDA, PIN. Creates user and returns JWT. Returns 409 if phone already registered.
     */
    public AuthResponse registerWithPhonePin(com.quickcash.auth.dto.PhonePinRegisterRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }
        User user = User.builder()
                .id(UUID.randomUUID())
                .uid(UUID.randomUUID().toString())
                .phoneNumber(request.getPhoneNumber())
                .displayName(request.getDisplayName())
                .nidaNumber(request.getNidaNumber())
                .photoUrl(request.getPhotoUrl())
                .pinHash(passwordEncoder.encode(request.getPin()))
                .build();
        user = userRepository.save(user);
        String token = jwtService.createToken(user.getId(), user.getUid());
        return AuthResponse.builder()
                .userId(user.getId().toString())
                .accessToken(token)
                .expiresInMs(expirationMs)
                .build();
    }

    /**
     * Login with phone number and 4-digit PIN. Returns JWT or 401.
     */
    public AuthResponse loginWithPhonePin(String phoneNumber, String pin) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone or PIN"));
        if (user.getPinHash() == null || user.getPinHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone or PIN");
        }
        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid phone or PIN");
        }
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
                .id(UUID.randomUUID())
                .uid(uid)
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .photoUrl(request.getPhotoUrl())
                .phoneNumber(request.getPhoneNumber())
                .fcmToken(request.getFcmToken())
                .build();
    }
}

package com.quickcash.auth;

import com.quickcash.auth.dto.AuthRequest;
import com.quickcash.auth.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.registerOrLogin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.registerOrLogin(request));
    }

    /**
     * Refresh access token (P2). Accepts valid Bearer token; returns new token. Token must not be expired.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authorization.substring(7);
        try {
            java.util.UUID userId = jwtService.getUserIdFromToken(token);
            String uid = jwtService.getUidFromToken(token);
            if (uid == null) uid = "";
            String newToken = jwtService.createToken(userId, uid);
            return ResponseEntity.ok(AuthResponse.builder()
                    .userId(userId.toString())
                    .accessToken(newToken)
                    .expiresInMs(jwtService.getExpirationMs())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}

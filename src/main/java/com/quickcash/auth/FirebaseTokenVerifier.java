package com.quickcash.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FirebaseTokenVerifier {

    @Value("${app.firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Verifies Firebase ID token and returns the UID.
     * If Firebase is not enabled (dev), returns null and caller may use request body uid.
     */
    public String verifyIdToken(String idToken) {
        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            return null;
        }
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return decodedToken.getUid();
        } catch (FirebaseAuthException e) {
            log.debug("Firebase token verification failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid Firebase token");
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}

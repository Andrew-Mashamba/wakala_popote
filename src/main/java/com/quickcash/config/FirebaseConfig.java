package com.quickcash.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (!firebaseEnabled || credentialsPath == null || credentialsPath.isBlank()) {
            log.info("Firebase not configured; auth will accept uid in request body (dev mode)");
            return;
        }
        try (FileInputStream fis = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(fis))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized");
            }
        } catch (IOException e) {
            log.warn("Firebase credentials not loaded: {}", e.getMessage());
        }
    }
}

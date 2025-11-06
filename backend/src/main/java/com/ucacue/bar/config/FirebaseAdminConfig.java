package com.ucacue.bar.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseAdminConfig {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void init() {
        try {
            if (!firebaseEnabled) {
                log.info("Firebase disabled via configuration");
                return;
            }
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp already initialized");
                return;
            }

            GoogleCredentials credentials = null;

            // Try classpath resource first (e.g., classpath:firebase/xxx.json)
            if (serviceAccountPath != null && serviceAccountPath.startsWith("classpath:")) {
                String cp = serviceAccountPath.replace("classpath:", "");
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(cp);
                if (is != null) {
                    credentials = GoogleCredentials.fromStream(is);
                    log.info("Loaded Firebase credentials from classpath: {}", cp);
                } else {
                    log.warn("Firebase service account not found at classpath path: {}", cp);
                }
            }

            // Fallback: try as plain path on classpath
            if (credentials == null && serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                try (InputStream is = this.getClass().getResourceAsStream(serviceAccountPath)) {
                    if (is != null) {
                        credentials = GoogleCredentials.fromStream(is);
                        log.info("Loaded Firebase credentials from path: {}", serviceAccountPath);
                    }
                } catch (Exception ignored) {}
            }

            if (credentials == null && serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                java.nio.file.Path path = java.nio.file.Paths.get(serviceAccountPath);
                if (java.nio.file.Files.exists(path)) {
                    try (InputStream is = java.nio.file.Files.newInputStream(path)) {
                        credentials = GoogleCredentials.fromStream(is);
                        log.info("Loaded Firebase credentials from filesystem path: {}", path);
                    }
                } else {
                    log.warn("Firebase service account path does not exist: {}", path);
                }
            }

            // Final fallback: Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS)
            if (credentials == null) {
                log.warn("Using Application Default Credentials for Firebase (set GOOGLE_APPLICATION_CREDENTIALS env var)");
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin: {}", e.getMessage());
        }
    }
}

package com.ucacue.bar.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseAdminConfig {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;
    @Value("${firebase.auth-enabled:${FIREBASE_AUTH_ENABLED:false}}")
    private boolean firebaseAuthEnabled;
    @Value("${firebase.push-enabled:${FIREBASE_PUSH_ENABLED:false}}")
    private boolean firebasePushEnabled;

    @PostConstruct
    public void init() {
        try {
            if (!firebaseEnabled && !firebaseAuthEnabled && !firebasePushEnabled) {
                log.info("Firebase disabled via configuration");
                return;
            }
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp already initialized");
                return;
            }

            GoogleCredentials credentials = null;

            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                throw new IllegalStateException("Firebase enabled but service-account-path is not configured");
            }

            if (serviceAccountPath.startsWith("classpath:")) {
                String cp = serviceAccountPath.replace("classpath:", "");
                try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(cp)) {
                    if (is == null) {
                        throw new IllegalStateException("Firebase service account not found at classpath path: " + cp);
                    }
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                java.nio.file.Path path = java.nio.file.Paths.get(serviceAccountPath);
                if (!java.nio.file.Files.exists(path)) {
                    throw new IllegalStateException("Firebase service account path does not exist: " + path);
                }
                try (InputStream is = java.nio.file.Files.newInputStream(path)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);
            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseApp.initializeApp(optionsBuilder.build());
            log.info("Firebase Admin initialized successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Firebase Admin", e);
        }
    }
}

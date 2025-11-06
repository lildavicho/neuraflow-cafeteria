package com.ucacue.bar.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseService.class);
    private static final String RESOURCES_PREFIX = "src/main/resources/";
    private static final String CLASSPATH_PREFIX = "classpath:";

    // Ruta configurada en application.yml (firebase.service-account-path)
    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    // ID de proyecto (firebase.project-id)
    @Value("${firebase.project-id:}")
    private String projectId;

    @PostConstruct
    public void init() throws IOException {
        try {
            // Si ya está inicializado (p. ej., via FirebaseAdminConfig), no hacer nada
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            // Si no hay ruta configurada, omitir (usará FirebaseAdminConfig o ADC)
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                return;
            }

            String path = serviceAccountPath.trim();
            InputStream serviceAccountStream;

            // Normalize common path formats to classpath resource names
            if (path.startsWith(CLASSPATH_PREFIX)) {
                String cp = path.substring(CLASSPATH_PREFIX.length());
                serviceAccountStream = new ClassPathResource(cp).getInputStream();
            } else if (path.contains(RESOURCES_PREFIX)) {
                String cp = path.substring(path.indexOf(RESOURCES_PREFIX) + RESOURCES_PREFIX.length());
                serviceAccountStream = new ClassPathResource(cp).getInputStream();
            } else {
                ClassPathResource cpr = new ClassPathResource(path);
                if (cpr.exists()) {
                    serviceAccountStream = cpr.getInputStream();
                } else {
                    // Fall back to filesystem absolute/relative path
                    serviceAccountStream = new FileInputStream(path);
                }
            }

            try (InputStream serviceAccount = serviceAccountStream) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId != null ? projectId : null)
                        .build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
            }

        } catch (Exception e) {
            log.error("Error al inicializar Firebase Admin", e);
        }
    }
}



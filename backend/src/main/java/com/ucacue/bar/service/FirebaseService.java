package com.ucacue.bar.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseService {

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
            if (path.startsWith("classpath:")) {
                String cp = path.substring("classpath:".length());
                serviceAccountStream = new ClassPathResource(cp).getInputStream();
            } else if (path.contains("src/main/resources/")) {
                String cp = path.substring(path.indexOf("src/main/resources/") + "src/main/resources/".length());
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
            System.err.println("❌ Error al inicializar Firebase Admin:");
            e.printStackTrace();
        }
    }
}



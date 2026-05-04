package com.ucacue.bar.erp.accounting.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sri")
@Getter
@Setter
public class SriProperties {

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
    private int authorizationPollDelayMs = 1500;
    private boolean autoQueryAfterReception = true;
    private Signature signature = new Signature();
    private Ride ride = new Ride();
    private Endpoint test = new Endpoint();
    private Endpoint production = new Endpoint();

    @Getter
    @Setter
    public static class Signature {
        private String mode = "NONE";
        private String pkcs12Path;
        private String pkcs12Password;
        private String keyAlias;
        private String passwordMasterKey;
    }

    @Getter
    @Setter
    public static class Ride {
        private String storagePath = "storage/sri/ride";
        private String storageProvider = "LOCAL";
        private String publicBaseUrl;
        private String supabaseUrl;
        private String supabaseServiceRoleKey;
        private String supabaseBucket = "facturas-sri";
        private String defaultSender = "soporte@insightvisionia.cloud";
        private String defaultSenderName = "Insight Vision IA";
        private boolean emailEnabled = true;
        private boolean centralRelayEnabled = false;
        private long signedUrlTtlSeconds = 3600;
        private Resend resend = new Resend();
    }

    @Getter
    @Setter
    public static class Resend {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://api.resend.com";
        private String fromEmail = "soporte@insightvisionia.cloud";
        private String fromName = "Insight Vision IA";
        private String replyTo = "soporte@insightvisionia.cloud";
        private String webhookSecret;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 15000;
        private long maxAttachmentBytes = 10_000_000L;
        private long maxTotalAttachmentBytes = 20_000_000L;
    }

    @Getter
    @Setter
    public static class Endpoint {
        private String receptionUrl;
        private String authorizationUrl;
    }
}

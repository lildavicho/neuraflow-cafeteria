package com.ucacue.bar.erp.accounting.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.service.EmailService.EmailAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ResendEmailClient {

    private final SriProperties sriProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailClient(SriProperties sriProperties, ObjectMapper objectMapper) {
        this.sriProperties = sriProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(sriProperties.getRide().getResend().getConnectTimeoutMs()))
                .build();
    }

    public boolean isReady() {
        SriProperties.Resend resend = sriProperties.getRide().getResend();
        return resend.isEnabled()
                && resend.getApiKey() != null
                && !resend.getApiKey().isBlank()
                && resend.getFromEmail() != null
                && !resend.getFromEmail().isBlank();
    }

    public ResendSendResult send(SendRequest request) {
        SriProperties.Resend resend = sriProperties.getRide().getResend();
        if (!isReady()) {
            return ResendSendResult.disabled("DISABLED", "Resend no esta configurado. Define RESEND_API_KEY.");
        }
        if (request.to() == null || request.to().isBlank()) {
            return ResendSendResult.disabled("NO_RECIPIENT", "Destinatario vacio");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            String fromName = firstNonBlank(request.fromName(), resend.getFromName(), "Insight Vision IA");
            String fromEmail = firstNonBlank(request.fromEmail(), resend.getFromEmail(), "soporte@insightvisionia.cloud");
            payload.put("from", fromName + " <" + fromEmail + ">");
            payload.put("to", List.of(request.to().trim()));
            payload.put("subject", request.subject() == null ? "" : request.subject());
            payload.put("html", request.html() == null ? "" : request.html());

            String replyTo = firstNonBlank(request.replyTo(), resend.getReplyTo());
            if (replyTo != null && !replyTo.isBlank()) {
                payload.put("reply_to", replyTo.trim());
            }

            List<EmailAttachment> attachments = request.attachments();
            if (attachments != null && !attachments.isEmpty()) {
                List<Map<String, Object>> attachPayload = new ArrayList<>(attachments.size());
                for (EmailAttachment attachment : attachments) {
                    if (attachment == null || attachment.content() == null || attachment.content().length == 0) {
                        continue;
                    }
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("filename", attachment.fileName());
                    entry.put("content", Base64.getEncoder().encodeToString(attachment.content()));
                    if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
                        entry.put("content_type", attachment.contentType());
                    }
                    attachPayload.add(entry);
                }
                if (!attachPayload.isEmpty()) {
                    payload.put("attachments", attachPayload);
                }
            }

            String body = objectMapper.writeValueAsString(payload);
            String baseUrl = resend.getBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.resend.com";
            }
            URI uri = URI.create(baseUrl.replaceAll("/+$", "") + "/emails");

            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(resend.getReadTimeoutMs()))
                    .header("Authorization", "Bearer " + resend.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                String messageId = extractMessageId(response.body());
                if (messageId == null || messageId.isBlank()) {
                    log.warn("Resend accepted email with status={} but response did not include message id", status);
                }
                return new ResendSendResult(
                        true,
                        "RESEND",
                        messageId,
                        status,
                        null,
                        null,
                        false,
                        java.time.Instant.now(),
                        truncate(response.body()));
            }

            String errorMessage = extractError(response.body());
            String errorCode = extractErrorCode(response.body(), status);
            boolean retryable = status == 408 || status == 429 || status >= 500;
            log.warn("Resend rejected email status={} retryable={} body={}", status, retryable, truncate(response.body()));
            return new ResendSendResult(
                    false,
                    "RESEND",
                    null,
                    status,
                    errorCode,
                    "Resend HTTP " + status + ": " + errorMessage,
                    retryable,
                    null,
                    truncate(response.body()));
        } catch (java.net.http.HttpTimeoutException ex) {
            log.warn("Resend timeout sending invoice email: {}", ex.getMessage());
            return new ResendSendResult(false, "RESEND", null, 0, "TIMEOUT",
                    "Resend no respondio a tiempo. Se reintentara automaticamente.", true, null, null);
        } catch (java.io.IOException ex) {
            log.warn("Resend network error: {}", ex.getMessage());
            return new ResendSendResult(false, "RESEND", null, 0, "NETWORK_ERROR",
                    "Error de red al enviar por Resend. Se reintentara automaticamente.", true, null, null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ResendSendResult(false, "RESEND", null, 0, "INTERRUPTED",
                    "Envio interrumpido", true, null, null);
        } catch (Exception ex) {
            log.warn("Resend send failed: {}", ex.getMessage());
            return new ResendSendResult(false, "RESEND", null, 0, "SEND_FAILED",
                    "No se pudo enviar el correo por Resend: " + safe(ex.getMessage()), false, null, null);
        }
    }

    private String extractMessageId(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object id = parsed.get("id");
            return id instanceof String s && !s.isBlank() ? s : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractError(String body) {
        if (body == null || body.isBlank()) {
            return "respuesta vacia";
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object message = parsed.get("message");
            if (message instanceof String s && !s.isBlank()) {
                return s;
            }
            Object error = parsed.get("error");
            if (error instanceof String s && !s.isBlank()) {
                return s;
            }
            if (error instanceof Map<?, ?> nested) {
                Object nestedMessage = nested.get("message");
                if (nestedMessage instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }
        return truncate(body);
    }

    private String extractErrorCode(String body, int status) {
        if (body != null && !body.isBlank()) {
            try {
                Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
                Object name = parsed.get("name");
                if (name instanceof String s && !s.isBlank()) {
                    return s;
                }
                Object error = parsed.get("error");
                if (error instanceof Map<?, ?> nested) {
                    Object code = nested.get("code");
                    if (code instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "HTTP_" + status;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 240 ? value.substring(0, 240) + "..." : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record SendRequest(
            String to,
            String subject,
            String html,
            String fromEmail,
            String replyTo,
            String fromName,
            List<EmailAttachment> attachments) {
    }
}

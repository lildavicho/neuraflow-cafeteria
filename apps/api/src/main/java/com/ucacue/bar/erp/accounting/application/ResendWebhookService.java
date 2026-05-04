package com.ucacue.bar.erp.accounting.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ResendWebhookEventEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.InvoiceEmailLogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ResendWebhookEventRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendWebhookService {

    private static final long WEBHOOK_TOLERANCE_SECONDS = 300;

    private final SriProperties sriProperties;
    private final ObjectMapper objectMapper;
    private final ResendWebhookEventRepository resendWebhookEventRepository;
    private final InvoiceEmailLogRepository invoiceEmailLogRepository;
    private final TaxDocumentRepository taxDocumentRepository;

    @Transactional
    public WebhookProcessResult process(HttpHeaders headers, String rawBody) {
        Map<String, Object> payload = parsePayload(rawBody);
        String providerMessageId = resolveProviderMessageId(payload);
        String eventType = stringValue(payload.get("type"));
        SignatureVerification signatureVerification = verifySignature(headers, rawBody);

        ResendWebhookEventEntity event = new ResendWebhookEventEntity();
        event.setProviderMessageId(providerMessageId);
        event.setEventType(eventType);
        event.setPayloadJson(payload);
        event.setHeadersJson(headersToMap(headers));
        event.setSignatureValid(signatureVerification.valid());

        if (!signatureVerification.valid()) {
            event.setStatus("REJECTED");
            event.setErrorMessage(signatureVerification.reason());
            resendWebhookEventRepository.save(event);
            throw new UnauthorizedException(signatureVerification.reason());
        }

        String status = mapStatus(eventType);
        event.setStatus("PROCESSED");
        event.setProcessedAt(OffsetDateTime.now());

        if (providerMessageId != null && !providerMessageId.isBlank()) {
            invoiceEmailLogRepository.findFirstByProviderMessageIdOrderByCreatedAtDesc(providerMessageId)
                    .ifPresent(log -> {
                    if (log.getTenantId() == null) {
                        event.setStatus("NO_TENANT");
                        event.setErrorMessage("Matched email log has no tenant_id");
                        return;
                    }
                    event.setTenantId(log.getTenantId());
                    boolean transitioned = applyLogStatus(log, status);
                    if (transitioned) {
                        updateDocumentSummary(log, status);
                    }
                });
        }
        resendWebhookEventRepository.save(event);
        return new WebhookProcessResult(true, providerMessageId, eventType, status);
    }

    private SignatureVerification verifySignature(HttpHeaders headers, String rawBody) {
        String secret = sriProperties.getRide().getResend().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            return SignatureVerification.invalid("Resend webhook secret is not configured");
        }
        String svixId = headers.getFirst("svix-id");
        String svixTimestamp = headers.getFirst("svix-timestamp");
        String svixSignature = headers.getFirst("svix-signature");
        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            return SignatureVerification.invalid("Invalid Resend webhook signature");
        }
        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(svixTimestamp.trim());
        } catch (NumberFormatException ex) {
            return SignatureVerification.invalid("Invalid Resend webhook timestamp");
        }
        long skewSeconds = Math.abs(Instant.now().getEpochSecond() - timestampSeconds);
        if (skewSeconds > WEBHOOK_TOLERANCE_SECONDS) {
            return SignatureVerification.invalid("Expired Resend webhook timestamp");
        }
        String signedPayload = svixId + "." + svixTimestamp + "." + rawBody;
        try {
            String cleanSecret = secret.startsWith("whsec_") ? secret.substring("whsec_".length()) : secret;
            byte[] key = Base64.getDecoder().decode(cleanSecret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
            for (String signaturePart : svixSignature.split(" ")) {
                String candidate = signaturePart.contains(",")
                        ? signaturePart.substring(signaturePart.indexOf(',') + 1)
                        : signaturePart;
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8))) {
                    return SignatureVerification.ok();
                }
            }
            return SignatureVerification.invalid("Invalid Resend webhook signature");
        } catch (Exception ex) {
            return SignatureVerification.invalid("Invalid Resend webhook signature");
        }
    }

    private boolean applyLogStatus(InvoiceEmailLogEntity log, String status) {
        String currentStatus = log.getStatus();
        if (!shouldTransition(currentStatus, status)) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        log.setStatus(status);
        switch (status) {
            case "DELIVERED" -> log.setDeliveredAt(now);
            case "BOUNCED" -> log.setBouncedAt(now);
            case "COMPLAINED" -> log.setComplainedAt(now);
            case "OPENED" -> log.setOpenedAt(now);
            case "CLICKED" -> log.setClickedAt(now);
            default -> {
            }
        }
        invoiceEmailLogRepository.save(log);
        return true;
    }

    private boolean shouldTransition(String currentStatus, String nextStatus) {
        if (nextStatus == null || nextStatus.isBlank()) {
            return false;
        }
        if (currentStatus == null || currentStatus.isBlank() || currentStatus.equals(nextStatus)) {
            return true;
        }
        if (isTerminal(currentStatus)) {
            return false;
        }
        if (isTerminal(nextStatus)) {
            return true;
        }
        return priority(nextStatus) >= priority(currentStatus);
    }

    private boolean isTerminal(String status) {
        return "FAILED".equals(status) || "BOUNCED".equals(status) || "COMPLAINED".equals(status);
    }

    private int priority(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "SENT" -> 1;
            case "DELIVERED" -> 2;
            case "OPENED" -> 3;
            case "CLICKED" -> 4;
            default -> 1;
        };
    }

    private void updateDocumentSummary(InvoiceEmailLogEntity log, String status) {
        taxDocumentRepository.findByTenantIdAndId(log.getTenantId(), log.getTaxDocumentId())
                .ifPresent(document -> {
                    document.setRideEmailStatus(status);
                    document.setRideEmailProvider(log.getProvider());
                    document.setRideEmailMessageId(log.getProviderMessageId());
                    if ("BOUNCED".equals(status) || "COMPLAINED".equals(status)) {
                        document.setRideEmailError("Resend webhook status: " + status);
                    }
                    taxDocumentRepository.save(document);
                });
    }

    private String mapStatus(String eventType) {
        if (eventType == null) {
            return "WEBHOOK_RECEIVED";
        }
        return switch (eventType) {
            case "email.delivered" -> "DELIVERED";
            case "email.bounced" -> "BOUNCED";
            case "email.complained" -> "COMPLAINED";
            case "email.opened" -> "OPENED";
            case "email.clicked" -> "CLICKED";
            default -> "WEBHOOK_RECEIVED";
        };
    }

    private String resolveProviderMessageId(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> map) {
            String id = firstNonBlank(
                    stringValue(map.get("id")),
                    stringValue(map.get("email_id")),
                    stringValue(map.get("message_id")));
            if (id != null) {
                return id;
            }
        }
        return firstNonBlank(
                stringValue(payload.get("id")),
                stringValue(payload.get("email_id")),
                stringValue(payload.get("message_id")));
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of("raw", rawBody == null ? "" : rawBody);
        }
    }

    private Map<String, Object> headersToMap(HttpHeaders headers) {
        Map<String, Object> map = new LinkedHashMap<>();
        putHeader(map, headers, "svix-id");
        putHeader(map, headers, "svix-timestamp");
        putHeader(map, headers, "content-type");
        putHeader(map, headers, "user-agent");
        return map;
    }

    private void putHeader(Map<String, Object> map, HttpHeaders headers, String headerName) {
        String value = headers.getFirst(headerName);
        if (value != null && !value.isBlank()) {
            map.put(headerName, value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record WebhookProcessResult(
            boolean success,
            String providerMessageId,
            String eventType,
            String status) {
    }

    private record SignatureVerification(
            boolean valid,
            String reason) {

        private static SignatureVerification ok() {
            return new SignatureVerification(true, null);
        }

        private static SignatureVerification invalid(String reason) {
            return new SignatureVerification(false, reason);
        }
    }
}

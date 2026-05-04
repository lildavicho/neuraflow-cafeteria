package com.ucacue.bar.erp.accounting.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ResendWebhookEventEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.InvoiceEmailLogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ResendWebhookEventRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResendWebhookServiceTest {

    private static final byte[] SECRET_BYTES = "test-resend-webhook-secret-32".getBytes(StandardCharsets.UTF_8);
    private static final String SECRET = "whsec_" + Base64.getEncoder().encodeToString(SECRET_BYTES);
    private static final String BODY = "{\"type\":\"email.delivered\",\"data\":{\"id\":\"email-123\"}}";

    private final ResendWebhookEventRepository eventRepository = mock(ResendWebhookEventRepository.class);
    private final InvoiceEmailLogRepository logRepository = mock(InvoiceEmailLogRepository.class);
    private final TaxDocumentRepository documentRepository = mock(TaxDocumentRepository.class);
    private final ResendWebhookService service = service();

    @Test
    void acceptsValidSignatureWithinTimestampWindow() {
        when(logRepository.findFirstByProviderMessageIdOrderByCreatedAtDesc(anyString()))
                .thenReturn(Optional.empty());

        var result = service.process(headers(Instant.now().getEpochSecond(), true), BODY);

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("email-123");
        ArgumentCaptor<ResendWebhookEventEntity> captor = ArgumentCaptor.forClass(ResendWebhookEventEntity.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getSignatureValid()).isTrue();
        assertThat(captor.getValue().getHeadersJson()).doesNotContainKey("svix-signature");
    }

    @Test
    void rejectsMissingTimestamp() {
        HttpHeaders headers = headers(Instant.now().getEpochSecond(), true);
        headers.remove("svix-timestamp");

        assertThatThrownBy(() -> service.process(headers, BODY))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsOldTimestampEvenWithValidSignature() {
        long timestamp = Instant.now().minusSeconds(301).getEpochSecond();

        assertThatThrownBy(() -> service.process(headers(timestamp, true), BODY))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void rejectsFutureTimestampOutsideWindow() {
        long timestamp = Instant.now().plusSeconds(301).getEpochSecond();

        assertThatThrownBy(() -> service.process(headers(timestamp, true), BODY))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void rejectsMalformedTimestamp() {
        HttpHeaders headers = headers(Instant.now().getEpochSecond(), true);
        headers.set("svix-timestamp", "not-a-number");
        headers.set("svix-signature", "v1," + signature("not-a-number"));

        assertThatThrownBy(() -> service.process(headers, BODY))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("timestamp");
    }

    private ResendWebhookService service() {
        SriProperties properties = new SriProperties();
        properties.getRide().getResend().setWebhookSecret(SECRET);
        return new ResendWebhookService(properties, new ObjectMapper(), eventRepository, logRepository, documentRepository);
    }

    private HttpHeaders headers(long timestamp, boolean validSignature) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("svix-id", "msg_123");
        headers.set("svix-timestamp", String.valueOf(timestamp));
        headers.set("svix-signature", "v1," + (validSignature ? signature(String.valueOf(timestamp)) : "invalid"));
        headers.set("content-type", "application/json");
        headers.set("authorization", "Bearer should-not-be-saved");
        return headers;
    }

    private String signature(String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"));
            String signedPayload = "msg_123." + timestamp + "." + BODY;
            return Base64.getEncoder().encodeToString(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

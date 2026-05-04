package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "resend_webhook_events", indexes = {
        @Index(name = "idx_resend_webhook_events_message", columnList = "provider_message_id"),
        @Index(name = "idx_resend_webhook_events_tenant_received", columnList = "tenant_id,received_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ResendWebhookEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "event_type", length = 120)
    private String eventType;

    @Column(name = "provider_message_id", length = 180)
    private String providerMessageId;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid = Boolean.FALSE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "json")
    private Map<String, Object> payloadJson = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers_json", nullable = false, columnDefinition = "json")
    private Map<String, Object> headersJson = new HashMap<>();

    @Column(nullable = false, length = 40)
    private String status = "RECEIVED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
}

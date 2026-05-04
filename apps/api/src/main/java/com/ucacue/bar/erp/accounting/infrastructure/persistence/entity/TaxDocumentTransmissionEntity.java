package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_document_transmissions", indexes = {
        @Index(name = "idx_tax_document_transmissions_document_phase", columnList = "tax_document_id,phase,attempted_at"),
        @Index(name = "idx_tax_document_transmissions_status", columnList = "provider_status,success")
})
@Getter
@Setter
@NoArgsConstructor
public class TaxDocumentTransmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_document_id", nullable = false)
    private TaxDocumentEntity taxDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransmissionPhase phase;

    @Column(name = "endpoint_url", nullable = false, length = 255)
    private String endpointUrl;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "provider_status", length = 60)
    private String providerStatus;

    @Column(name = "provider_message", columnDefinition = "TEXT")
    private String providerMessage;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "transport_error", nullable = false)
    private Boolean transportError = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean success = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum TransmissionPhase {
        RECEPTION,
        AUTHORIZATION
    }
}

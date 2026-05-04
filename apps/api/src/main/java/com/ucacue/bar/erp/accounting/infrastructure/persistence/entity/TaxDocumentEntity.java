package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_documents", indexes = {
        @Index(name = "idx_tax_documents_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_tax_documents_source", columnList = "source_type,source_id"),
        @Index(name = "idx_tax_documents_access_key", columnList = "access_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class TaxDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", nullable = false, length = 20)
    private AccountingSourceModule sourceModule;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false)
    private AccountingDocumentTypeEntity documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_id")
    private DocumentSequenceEntity sequence;

    @Column(name = "establishment_code", length = 10)
    private String establishmentCode;

    @Column(name = "emission_point_code", length = 10)
    private String emissionPointCode;

    @Column(name = "sequential_number", length = 20)
    private String sequentialNumber;

    @Column(name = "environment_code", length = 10)
    private String environmentCode;

    @Column(name = "access_key", length = 80, unique = true)
    private String accessKey;

    @Column(name = "authorization_code", length = 80)
    private String authorizationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxDocumentStatus status = TaxDocumentStatus.DRAFT;

    @Column(name = "reception_status", length = 40)
    private String receptionStatus;

    @Column(name = "authorization_status", length = 40)
    private String authorizationStatus;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "last_status_at")
    private LocalDateTime lastStatusAt;

    @Column(name = "buyer_identification", length = 30)
    private String buyerIdentification;

    @Column(name = "buyer_name", length = 150)
    private String buyerName;

    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "xml_payload", columnDefinition = "LONGTEXT")
    private String xmlPayload;

    @Column(name = "signed_xml_payload", columnDefinition = "LONGTEXT")
    private String signedXmlPayload;

    @Column(name = "ride_pdf_path", length = 500)
    private String ridePdfPath;

    @Column(name = "ride_pdf_url", length = 500)
    private String ridePdfUrl;

    @Column(name = "ride_pdf_generated_at")
    private LocalDateTime ridePdfGeneratedAt;

    @Column(name = "ride_xml_path", length = 500)
    private String rideXmlPath;

    @Column(name = "ride_xml_url", length = 500)
    private String rideXmlUrl;

    @Column(name = "ride_email_status", length = 30)
    private String rideEmailStatus;

    @Column(name = "ride_email_sent_at")
    private LocalDateTime rideEmailSentAt;

    @Column(name = "ride_email_error", columnDefinition = "TEXT")
    private String rideEmailError;

    @Column(name = "ride_email_attempts", nullable = false)
    private Integer rideEmailAttempts = 0;

    @Column(name = "ride_email_next_retry_at")
    private LocalDateTime rideEmailNextRetryAt;

    @Column(name = "ride_email_last_attempt_at")
    private LocalDateTime rideEmailLastAttemptAt;

    @Column(name = "ride_email_provider", length = 40)
    private String rideEmailProvider;

    @Column(name = "ride_email_message_id", length = 180)
    private String rideEmailMessageId;

    @Column(name = "ride_email_template_code", length = 80)
    private String rideEmailTemplateCode;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "last_provider_message", columnDefinition = "TEXT")
    private String lastProviderMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

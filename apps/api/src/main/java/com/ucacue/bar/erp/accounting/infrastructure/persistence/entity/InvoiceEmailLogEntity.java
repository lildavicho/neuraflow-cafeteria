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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice_email_logs", indexes = {
        @Index(name = "idx_invoice_email_logs_tenant_document", columnList = "tenant_id,tax_document_id,created_at"),
        @Index(name = "idx_invoice_email_logs_tenant_status_created", columnList = "tenant_id,status,created_at"),
        @Index(name = "idx_invoice_email_logs_provider_message", columnList = "provider_message_id")
})
@Getter
@Setter
@NoArgsConstructor
public class InvoiceEmailLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tax_document_id", nullable = false)
    private Long taxDocumentId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_message_id", length = 180)
    private String providerMessageId;

    @Column(name = "from_email", nullable = false, length = 180)
    private String fromEmail;

    @Column(name = "from_name", length = 180)
    private String fromName;

    @Column(name = "reply_to_email", length = 180)
    private String replyToEmail;

    @Column(name = "to_email", length = 180)
    private String toEmail;

    @Column(length = 300)
    private String subject;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(nullable = false, length = 40)
    private String status = "PENDING";

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Boolean retryable = Boolean.FALSE;

    @Column(name = "attachment_count", nullable = false)
    private Integer attachmentCount = 0;

    @Column(name = "attachment_total_bytes", nullable = false)
    private Long attachmentTotalBytes = 0L;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "bounced_at")
    private OffsetDateTime bouncedAt;

    @Column(name = "complained_at")
    private OffsetDateTime complainedAt;

    @Column(name = "opened_at")
    private OffsetDateTime openedAt;

    @Column(name = "clicked_at")
    private OffsetDateTime clickedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.repository.OrderRepository;
import com.ucacue.bar.service.EmailService;
import com.ucacue.bar.service.EmailService.EmailSendResult;
import com.ucacue.bar.service.EmailService.EmailAttachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SriAutomationService {

    private static final int MAX_EMAIL_ATTEMPTS = 3;
    private static final Duration EMAIL_RETRY_DELAY = Duration.ofMinutes(5);

    private final TaxDocumentRepository taxDocumentRepository;
    private final OrderRepository orderRepository;
    private final SriRideService sriRideService;
    private final RideStorageService rideStorageService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final InvoiceEmailLogService invoiceEmailLogService;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final SriProperties sriProperties;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentAuthorized(SriDocumentAuthorizedEvent event) {
        try {
            processAuthorizedDocument(event.tenantId(), event.taxDocumentId());
        } catch (Exception ex) {
            log.warn("SRI automation failed tenant={} document={} reason={}",
                    event.tenantId(), event.taxDocumentId(), ex.getMessage());
        }
    }

    @Transactional
    public AutomationResult processAuthorizedDocument(Long tenantId, Long taxDocumentId) {
        return processAuthorizedDocument(tenantId, taxDocumentId, false);
    }

    @Transactional
    public AutomationResult regenerateRide(Long tenantId, Long taxDocumentId) {
        return processAuthorizedDocument(tenantId, taxDocumentId, true);
    }

    @Transactional
    public AutomationResult resendRide(Long tenantId, Long taxDocumentId, String recipientOverride) {
        return processAuthorizedDocument(tenantId, taxDocumentId, true, recipientOverride);
    }

    @Scheduled(fixedDelayString = "${app.sri.ride.email-retry-scan-ms:60000}")
    @Transactional
    public void retryPendingEmails() {
        List<TaxDocumentEntity> dueDocuments = taxDocumentRepository
                .findTop20ByRideEmailStatusAndRideEmailNextRetryAtLessThanEqualOrderByRideEmailNextRetryAtAsc(
                        "RETRY_PENDING",
                        LocalDateTime.now());
        for (TaxDocumentEntity document : dueDocuments) {
            try {
                processAuthorizedDocument(document.getTenantId(), document.getId(), false);
            } catch (Exception ex) {
                log.warn("SRI RIDE email retry failed tenant={} document={} reason={}",
                        document.getTenantId(), document.getId(), ex.getMessage());
            }
        }
    }

    private AutomationResult processAuthorizedDocument(Long tenantId, Long taxDocumentId, boolean manualRegeneration) {
        return processAuthorizedDocument(tenantId, taxDocumentId, manualRegeneration, null);
    }

    private AutomationResult processAuthorizedDocument(Long tenantId,
                                                       Long taxDocumentId,
                                                       boolean manualRegeneration,
                                                       String recipientOverride) {
        TaxDocumentEntity document = taxDocumentRepository.findByTenantIdAndIdForUpdate(tenantId, taxDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento tributario no encontrado"));
        if (document.getStatus() != TaxDocumentStatus.AUTHORIZED) {
            return new AutomationResult(document.getId(), "SKIPPED", "El documento no esta autorizado");
        }
        if (!manualRegeneration && ("SENT".equals(document.getRideEmailStatus())
                || "DELIVERED".equals(document.getRideEmailStatus()))
                && document.getRideEmailSentAt() != null) {
            return new AutomationResult(document.getId(), "SENT", "RIDE ya enviado previamente");
        }
        if (!manualRegeneration && "SENDING".equals(document.getRideEmailStatus())) {
            return new AutomationResult(document.getId(), "SENDING", "Envio en progreso");
        }
        document.setRideEmailStatus("SENDING");
        document.setRideEmailError(null);
        taxDocumentRepository.saveAndFlush(document);

        String recipient = firstNonBlank(recipientOverride, resolveRecipientEmail(document));
        String templateCode = manualRegeneration ? "SRI_INVOICE_RESEND" : "SRI_INVOICE_ISSUED";
        SriRideService.RidePdfResult pdf;
        RideStorageService.StoredRideFile storedPdf;
        RideStorageService.StoredRideFile storedXml = null;
        try {
            pdf = sriRideService.generatePdfForTenant(tenantId, taxDocumentId);
            storedPdf = rideStorageService.storeRidePdf(document, pdf.content());
            String xml = signedOrUnsignedXml(document);
            if (xml != null && !xml.isBlank()) {
                storedXml = rideStorageService.storeSignedXml(document, xml);
            }
        } catch (Exception ex) {
            String error = simplifyError(ex);
            document.setRideEmailStatus("PDF_FAILED");
            document.setRideEmailError(error);
            document.setRideEmailLastAttemptAt(LocalDateTime.now());
            document.setRideEmailProvider("INTERNAL");
            document.setRideEmailTemplateCode(templateCode);
            invoiceEmailLogService.recordFailure(
                    document,
                    recipient,
                    "Comprobante electronico " + safe(document.getSequentialNumber()),
                    templateCode,
                    "PDF_FAILED",
                    "PDF_FAILED",
                    error,
                    0,
                    0L);
            taxDocumentRepository.save(document);
            return new AutomationResult(document.getId(), "PDF_FAILED", error);
        }

        document.setRidePdfPath(storedPdf.path());
        document.setRidePdfUrl(storedPdf.publicUrl());
        document.setRidePdfGeneratedAt(LocalDateTime.now());
        if (storedXml != null) {
            document.setRideXmlPath(storedXml.path());
            document.setRideXmlUrl(storedXml.publicUrl());
        }

        if (recipient == null || recipient.isBlank()) {
            document.setRideEmailStatus("NO_CUSTOMER_EMAIL");
            document.setRideEmailError("El cliente no tiene correo registrado");
            document.setRideEmailNextRetryAt(null);
            document.setRideEmailLastAttemptAt(LocalDateTime.now());
            document.setRideEmailProvider("INTERNAL");
            document.setRideEmailTemplateCode(templateCode);
            List<EmailAttachment> generatedAttachments = buildAttachments(document, pdf);
            AttachmentSummary generatedAttachmentSummary = summarize(generatedAttachments);
            invoiceEmailLogService.recordFailure(
                    document,
                    null,
                    "Comprobante electronico " + safe(document.getSequentialNumber()),
                    templateCode,
                    "NO_CUSTOMER_EMAIL",
                    "NO_CUSTOMER_EMAIL",
                    "El cliente no tiene correo registrado",
                    generatedAttachmentSummary.count(),
                    generatedAttachmentSummary.totalBytes());
            taxDocumentRepository.save(document);
            return new AutomationResult(document.getId(), "NO_CUSTOMER_EMAIL", "PDF generado; cliente sin correo");
        }

        if (manualRegeneration) {
            document.setRideEmailAttempts(0);
            document.setRideEmailError(null);
            document.setRideEmailNextRetryAt(null);
        }

        EmailTemplateService.RenderedEmailTemplate renderedTemplate = emailTemplateService.render(
                tenantId,
                templateCode,
                templateVariables(document, recipient));
        List<EmailAttachment> attachments;
        try {
            attachments = buildAttachments(document, pdf);
        } catch (Exception ex) {
            String error = simplifyError(ex);
            document.setRideEmailStatus("FAILED");
            document.setRideEmailError(error);
            document.setRideEmailLastAttemptAt(LocalDateTime.now());
            document.setRideEmailProvider("INTERNAL");
            document.setRideEmailTemplateCode(templateCode);
            invoiceEmailLogService.recordFailure(
                    document,
                    recipient,
                    renderedTemplate.subject(),
                    renderedTemplate.templateCode(),
                    "FAILED",
                    "ATTACHMENT_FAILED",
                    error,
                    0,
                    0L);
            taxDocumentRepository.save(document);
            return new AutomationResult(document.getId(), "FAILED", error);
        }
        AttachmentSummary attachmentSummary = summarize(attachments);
        InvoiceEmailLogEntity emailLog = invoiceEmailLogService.createPending(
                document,
                recipient,
                renderedTemplate.subject(),
                renderedTemplate.templateCode(),
                attachmentSummary.count(),
                attachmentSummary.totalBytes());

        EmailSendResult delivery = emailService.sendSriDocument(
                tenantId,
                recipient,
                renderedTemplate.subject(),
                renderedTemplate.htmlBody(),
                attachments);

        invoiceEmailLogService.markResult(emailLog, delivery);
        applyDeliveryResult(document, delivery, renderedTemplate.templateCode());
        taxDocumentRepository.save(document);
        String message = manualRegeneration && delivery.success()
                ? "RIDE regenerado y correo enviado"
                : delivery.errorMessage();
        return new AutomationResult(document.getId(), document.getRideEmailStatus(), message);
    }

    private void applyDeliveryResult(TaxDocumentEntity document, EmailSendResult delivery, String templateCode) {
        int attempts = document.getRideEmailAttempts() == null ? 0 : document.getRideEmailAttempts();
        document.setRideEmailAttempts(attempts + 1);
        document.setRideEmailLastAttemptAt(LocalDateTime.now());
        document.setRideEmailProvider(delivery.provider());
        document.setRideEmailMessageId(delivery.providerMessageId());
        document.setRideEmailTemplateCode(templateCode);
        if (delivery.success()) {
            document.setRideEmailStatus("SENT");
            document.setRideEmailError(null);
            document.setRideEmailSentAt(LocalDateTime.now());
            document.setRideEmailNextRetryAt(null);
            return;
        }

        int nextAttempts = attempts + 1;
        document.setRideEmailSentAt(null);
        document.setRideEmailError(delivery.errorMessage());
        if (delivery.retryable() && nextAttempts < MAX_EMAIL_ATTEMPTS) {
            document.setRideEmailStatus("RETRY_PENDING");
            document.setRideEmailNextRetryAt(LocalDateTime.now().plus(EMAIL_RETRY_DELAY));
        } else {
            document.setRideEmailStatus(delivery.status());
            document.setRideEmailNextRetryAt(null);
        }
    }

    private List<EmailAttachment> buildAttachments(TaxDocumentEntity document, SriRideService.RidePdfResult pdf) {
        List<EmailAttachment> attachments = new ArrayList<>();
        attachments.add(new EmailAttachment(pdf.fileName(), pdf.content(), "application/pdf"));
        String xml = signedOrUnsignedXml(document);
        if (xml != null && !xml.isBlank()) {
            String xmlName = "sri-" + safe(document.getAccessKey()) + ".xml";
            attachments.add(new EmailAttachment(xmlName, xml.getBytes(StandardCharsets.UTF_8), "application/xml"));
        }
        return attachments;
    }

    private String signedOrUnsignedXml(TaxDocumentEntity document) {
        return document.getSignedXmlPayload() != null && !document.getSignedXmlPayload().isBlank()
                ? document.getSignedXmlPayload()
                : document.getXmlPayload();
    }

    private java.util.Map<String, Object> templateVariables(TaxDocumentEntity document, String recipient) {
        SriConfigurationEntity sriConfiguration = sriConfigurationRepository.findByTenantId(document.getTenantId()).orElse(null);
        String issuerName = sriConfiguration != null
                ? firstNonBlank(sriConfiguration.getIssuerTradeName(), sriConfiguration.getIssuerLegalName(), "Insight Vision IA")
                : "Insight Vision IA";
        String issuerRuc = sriConfiguration != null ? safe(sriConfiguration.getIssuerRuc()) : "";
        return java.util.Map.of(
                "issuerName", issuerName,
                "issuerRuc", issuerRuc,
                "customerName", firstNonBlank(document.getBuyerName(), "Cliente"),
                "customerEmail", safe(recipient),
                "documentNumber", safe(document.getSequentialNumber()),
                "issueDate", document.getIssueDate() != null ? document.getIssueDate().toString() : "",
                "totalAmount", document.getTotalAmount() != null ? document.getTotalAmount().toPlainString() : "0.00",
                "authorizationNumber", safe(document.getAuthorizationCode()),
                "supportEmail", firstNonBlank(sriProperties.getRide().getResend().getReplyTo(), "soporte@insightvisionia.cloud"));
    }

    private AttachmentSummary summarize(List<EmailAttachment> attachments) {
        int count = 0;
        long totalBytes = 0L;
        if (attachments != null) {
            for (EmailAttachment attachment : attachments) {
                if (attachment != null && attachment.content() != null) {
                    count++;
                    totalBytes += attachment.content().length;
                }
            }
        }
        return new AttachmentSummary(count, totalBytes);
    }

    private String resolveRecipientEmail(TaxDocumentEntity document) {
        if (document.getSourceType() == null || !document.getSourceType().startsWith("ORDER_")) {
            return null;
        }
        return orderRepository.findByTenantIdAndId(document.getTenantId(), document.getSourceId())
                .map(order -> {
                    if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
                        return order.getCustomerEmail().trim();
                    }
                    if (order.getUser() != null && order.getUser().getEmail() != null
                            && !order.getUser().getEmail().isBlank()) {
                        return order.getUser().getEmail().trim();
                    }
                    return null;
                })
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String simplifyError(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        String message = ex.getMessage().replaceAll("\\s+", " ").trim();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public record AutomationResult(
            Long documentId,
            String emailStatus,
            String message) {
    }

    private record AttachmentSummary(
            int count,
            long totalBytes) {
    }
}

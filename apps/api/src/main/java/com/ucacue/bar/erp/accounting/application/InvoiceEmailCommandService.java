package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceEmailCommandService {

    private static final int MAX_MANUAL_RESENDS_PER_HOUR = 3;

    private final SriAutomationService sriAutomationService;
    private final TaxDocumentRepository taxDocumentRepository;
    private final InvoiceEmailLogService invoiceEmailLogService;

    @Transactional
    public SriAutomationService.AutomationResult send(Long tenantId, Long documentId) {
        requireDocument(tenantId, documentId);
        return sriAutomationService.processAuthorizedDocument(tenantId, documentId);
    }

    @Transactional
    public SriAutomationService.AutomationResult resend(Long tenantId, Long documentId, ResendInvoiceEmailRequest request) {
        requireDocument(tenantId, documentId);
        long recentResends = invoiceEmailLogService.history(tenantId, documentId).stream()
                .filter(log -> "SRI_INVOICE_RESEND".equals(log.getTemplateCode()))
                .filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(OffsetDateTime.now().minusHours(1)))
                .count();
        if (recentResends >= MAX_MANUAL_RESENDS_PER_HOUR) {
            throw new BadRequestException("Limite de reenvios por factura excedido");
        }
        return sriAutomationService.resendRide(tenantId, documentId, request != null ? request.toEmail() : null);
    }

    @Transactional(readOnly = true)
    public InvoiceEmailHistoryResponse history(Long tenantId, Long documentId) {
        TaxDocumentEntity document = requireDocument(tenantId, documentId);
        List<InvoiceEmailLogItem> items = invoiceEmailLogService.history(tenantId, documentId).stream()
                .map(InvoiceEmailLogItem::from)
                .toList();
        return new InvoiceEmailHistoryResponse(document.getId(), document.getRideEmailStatus(), items);
    }

    @Transactional(readOnly = true)
    public List<InvoiceEmailLogItem> logs(Long tenantId, LocalDate from, LocalDate to, String status, int limit) {
        return invoiceEmailLogService.logs(tenantId, from, to, status, limit).stream()
                .map(InvoiceEmailLogItem::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceEmailLogService.EmailStatistics statistics(Long tenantId, LocalDate from, LocalDate to) {
        return invoiceEmailLogService.statistics(tenantId, from, to);
    }

    private TaxDocumentEntity requireDocument(Long tenantId, Long documentId) {
        return taxDocumentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado"));
    }

    public record ResendInvoiceEmailRequest(
            String toEmail,
            String reason) {
    }

    public record InvoiceEmailHistoryResponse(
            Long documentId,
            String emailStatus,
            List<InvoiceEmailLogItem> items) {
    }

    public record InvoiceEmailLogItem(
            Long id,
            Integer attemptNumber,
            String provider,
            String providerMessageId,
            String fromEmail,
            String toEmail,
            String subject,
            String templateCode,
            String status,
            String errorCode,
            String errorMessage,
            OffsetDateTime createdAt,
            OffsetDateTime sentAt) {

        private static InvoiceEmailLogItem from(InvoiceEmailLogEntity log) {
            return new InvoiceEmailLogItem(
                    log.getId(),
                    log.getAttemptNumber(),
                    log.getProvider(),
                    log.getProviderMessageId(),
                    log.getFromEmail(),
                    log.getToEmail(),
                    log.getSubject(),
                    log.getTemplateCode(),
                    log.getStatus(),
                    log.getErrorCode(),
                    log.getErrorMessage(),
                    log.getCreatedAt(),
                    log.getSentAt());
        }
    }
}

package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.InvoiceEmailLogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.InvoiceEmailLogRepository;
import com.ucacue.bar.service.EmailService.EmailSendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceEmailLogService {

    private final InvoiceEmailLogRepository invoiceEmailLogRepository;

    @Transactional
    public InvoiceEmailLogEntity createPending(TaxDocumentEntity document,
                                               String toEmail,
                                               String subject,
                                               String templateCode,
                                               int attachmentCount,
                                               long attachmentTotalBytes) {
        long previousAttempts = invoiceEmailLogRepository.countByTenantIdAndTaxDocumentId(
                document.getTenantId(),
                document.getId());
        InvoiceEmailLogEntity log = new InvoiceEmailLogEntity();
        log.setTenantId(document.getTenantId());
        log.setTaxDocumentId(document.getId());
        log.setAttemptNumber((int) previousAttempts + 1);
        log.setProvider("PENDING");
        log.setFromEmail("soporte@insightvisionia.cloud");
        log.setFromName("Insight Vision IA");
        log.setReplyToEmail("soporte@insightvisionia.cloud");
        log.setToEmail(toEmail);
        log.setSubject(subject);
        log.setTemplateCode(templateCode);
        log.setStatus("PENDING");
        log.setAttachmentCount(attachmentCount);
        log.setAttachmentTotalBytes(attachmentTotalBytes);
        return invoiceEmailLogRepository.save(log);
    }

    @Transactional
    public InvoiceEmailLogEntity recordFailure(TaxDocumentEntity document,
                                               String toEmail,
                                               String subject,
                                               String templateCode,
                                               String status,
                                               String errorCode,
                                               String errorMessage,
                                               int attachmentCount,
                                               long attachmentTotalBytes) {
        long previousAttempts = invoiceEmailLogRepository.countByTenantIdAndTaxDocumentId(
                document.getTenantId(),
                document.getId());
        InvoiceEmailLogEntity log = new InvoiceEmailLogEntity();
        log.setTenantId(document.getTenantId());
        log.setTaxDocumentId(document.getId());
        log.setAttemptNumber((int) previousAttempts + 1);
        log.setProvider("INTERNAL");
        log.setFromEmail("soporte@insightvisionia.cloud");
        log.setFromName("Insight Vision IA");
        log.setReplyToEmail("soporte@insightvisionia.cloud");
        log.setToEmail(toEmail);
        log.setSubject(subject);
        log.setTemplateCode(templateCode);
        log.setStatus(status);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        log.setRetryable(false);
        log.setAttachmentCount(attachmentCount);
        log.setAttachmentTotalBytes(attachmentTotalBytes);
        return invoiceEmailLogRepository.save(log);
    }

    @Transactional
    public InvoiceEmailLogEntity markResult(InvoiceEmailLogEntity log, EmailSendResult result) {
        log.setProvider(result.provider());
        log.setProviderMessageId(result.providerMessageId());
        log.setFromEmail(firstNonBlank(result.fromEmail(), "soporte@insightvisionia.cloud"));
        log.setFromName(firstNonBlank(result.fromName(), "Insight Vision IA"));
        log.setReplyToEmail(firstNonBlank(result.replyToEmail(), "soporte@insightvisionia.cloud"));
        log.setToEmail(firstNonBlank(result.toEmail(), log.getToEmail()));
        log.setSubject(firstNonBlank(result.subject(), log.getSubject()));
        log.setStatus(result.success() ? "SENT" : "FAILED");
        log.setErrorCode(result.errorCode());
        log.setErrorMessage(result.errorMessage());
        log.setRetryable(result.retryable());
        log.setAttachmentCount(result.attachmentCount());
        log.setAttachmentTotalBytes(result.attachmentTotalBytes());
        log.setSentAt(result.sentAt() != null ? result.sentAt().atOffset(ZoneOffset.UTC) : null);
        return invoiceEmailLogRepository.save(log);
    }

    @Transactional
    public void markWebhookStatus(String providerMessageId, String status) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return;
        }
        invoiceEmailLogRepository.findFirstByProviderMessageIdOrderByCreatedAtDesc(providerMessageId)
                .ifPresent(log -> {
                    if (!shouldTransition(log.getStatus(), status)) {
                        return;
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
                });
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

    @Transactional(readOnly = true)
    public List<InvoiceEmailLogEntity> history(Long tenantId, Long taxDocumentId) {
        return invoiceEmailLogRepository.findByTenantIdAndTaxDocumentIdOrderByCreatedAtDesc(tenantId, taxDocumentId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceEmailLogEntity> logs(Long tenantId, LocalDate from, LocalDate to, String status, int limit) {
        OffsetDateTime start = from != null ? from.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime end = to != null ? to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        return invoiceEmailLogRepository.searchByTenant(
                tenantId,
                start,
                end,
                status == null || status.isBlank() ? null : status.trim().toUpperCase(),
                PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    @Transactional(readOnly = true)
    public EmailStatistics statistics(Long tenantId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();
        OffsetDateTime start = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : invoiceEmailLogRepository.countByStatus(tenantId, start, end)) {
            byStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (Object[] row : invoiceEmailLogRepository.countByDay(tenantId, start, end)) {
            byDay.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return new EmailStatistics(startDate, endDate, byStatus, byDay);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record EmailStatistics(
            LocalDate from,
            LocalDate to,
            Map<String, Long> byStatus,
            Map<String, Long> byDay) {
    }
}

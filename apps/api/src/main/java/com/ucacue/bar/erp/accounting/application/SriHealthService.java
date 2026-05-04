package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SriHealthService {

    private static final List<TaxDocumentStatus> TRANSIENT_STATUSES = List.of(
            TaxDocumentStatus.PENDING_VALIDATION,
            TaxDocumentStatus.READY_TO_SEND,
            TaxDocumentStatus.SENT);
    private static final Duration STUCK_THRESHOLD = Duration.ofHours(2);
    private static final int REAP_BATCH_LIMIT = 20;

    private final TaxDocumentRepository taxDocumentRepository;
    private final TenantContextResolver tenantContextResolver;
    private final SriAutomationService sriAutomationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public HealthSnapshot snapshotForCurrentTenant() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return buildSnapshot(tenantId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "sri-health", key = "'code:' + #tenantCode")
    public HealthSnapshot snapshotForTenantCode(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return buildSnapshot(tenantId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "sri-health", key = "#tenantId")
    public HealthSnapshot snapshot(Long tenantId) {
        return buildSnapshot(tenantId);
    }

    private HealthSnapshot buildSnapshot(Long tenantId) {
        Object[] summary = taxDocumentRepository.healthSummaryByTenantId(tenantId);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        int index = 0;
        for (TaxDocumentStatus status : TaxDocumentStatus.values()) {
            statusCounts.put(status.name(), summaryLong(summary, index++));
        }
        long awaitingPdf = summaryLong(summary, index++);
        long emailRetryPending = summaryLong(summary, index++);
        long emailPdfFailed = summaryLong(summary, index++);
        long emailNoCustomerEmail = summaryLong(summary, index++);
        long emailDelivered = summaryLong(summary, index);

        LocalDateTime threshold = LocalDateTime.now().minus(STUCK_THRESHOLD);
        List<TaxDocumentEntity> stuck = taxDocumentRepository
                .findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
                        tenantId, TRANSIENT_STATUSES, threshold);
        List<StuckDocument> stuckDocs = stuck.stream()
                .map(d -> new StuckDocument(
                        d.getId(),
                        d.getDocumentType() != null ? d.getDocumentType().getDocumentCode() : null,
                        d.getSequentialNumber(),
                        d.getStatus() != null ? d.getStatus().name() : null,
                        d.getLastStatusAt(),
                        d.getLastProviderMessage()))
                .toList();

        return new HealthSnapshot(
                tenantId,
                LocalDateTime.now(),
                statusCounts,
                stuckDocs,
                Math.toIntExact(Math.min(awaitingPdf, Integer.MAX_VALUE)),
                emailRetryPending,
                emailPdfFailed,
                emailNoCustomerEmail,
                emailDelivered,
                STUCK_THRESHOLD.toMinutes());
    }

    private long summaryLong(Object[] summary, int index) {
        Object unwrapped = unwrap(summary);
        if (unwrapped instanceof Object[] array) {
            return array.length > index ? toLong(array[index]) : 0L;
        }
        return index == 0 ? toLong(unwrapped) : 0L;
    }

    private long toLong(Object value) {
        Object unwrapped = unwrap(value);
        return unwrapped instanceof Number number ? number.longValue() : 0L;
    }

    private Object unwrap(Object value) {
        if (value instanceof Object[] array && array.length == 1) {
            return unwrap(array[0]);
        }
        return value;
    }

    @Transactional
    public ReapResult reapForCurrentTenant() {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        return reap(tenantId);
    }

    @Transactional
    public ReapResult reap(Long tenantId) {
        List<TaxDocumentEntity> awaitingPdf = taxDocumentRepository
                .findTop20ByTenantIdAndStatusAndRidePdfPathIsNullOrderByCreatedAtAsc(
                        tenantId, TaxDocumentStatus.AUTHORIZED);
        int pdfRequeued = 0;
        List<Long> requeuedIds = new ArrayList<>();
        for (TaxDocumentEntity document : awaitingPdf) {
            try {
                sriAutomationService.processAuthorizedDocument(tenantId, document.getId());
                pdfRequeued++;
                requeuedIds.add(document.getId());
                if (pdfRequeued >= REAP_BATCH_LIMIT) break;
            } catch (Exception ex) {
                log.warn("SRI reap failed tenant={} document={} reason={}",
                        tenantId, document.getId(), ex.getMessage());
            }
        }

        LocalDateTime threshold = LocalDateTime.now().minus(STUCK_THRESHOLD);
        List<TaxDocumentEntity> stuck = taxDocumentRepository
                .findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
                        tenantId, TRANSIENT_STATUSES, threshold);
        int flagged = stuck.size();
        if (flagged > 0) {
            String stuckIds = stuck.stream().map(d -> String.valueOf(d.getId())).reduce((a, b) -> a + "," + b).orElse("");
            auditService.logAction(null, "SRI_HEALTH_REAP", "TAX_DOCUMENT", null,
                    "tenant=" + tenantId + " stuck=" + stuckIds + " requeued=" + requeuedIds);
        } else if (pdfRequeued > 0) {
            auditService.logAction(null, "SRI_HEALTH_REAP", "TAX_DOCUMENT", null,
                    "tenant=" + tenantId + " requeued=" + requeuedIds);
        }

        return new ReapResult(tenantId, pdfRequeued, flagged, LocalDateTime.now());
    }

    @Scheduled(cron = "${app.sri.health.daily-cron:0 0 6 * * *}")
    public void dailyHealthLog() {
        if (log.isInfoEnabled()) {
            log.info("SRI daily health probe executed at {}", LocalDateTime.now());
        }
    }

    public record HealthSnapshot(
            Long tenantId,
            LocalDateTime computedAt,
            Map<String, Long> documentsByStatus,
            List<StuckDocument> stuckDocuments,
            int authorizedAwaitingPdf,
            long emailRetryPending,
            long emailPdfFailed,
            long emailNoCustomerEmail,
            long emailDelivered,
            long stuckThresholdMinutes) {
    }

    public record StuckDocument(
            Long id,
            String documentCode,
            String sequentialNumber,
            String status,
            LocalDateTime lastStatusAt,
            String lastProviderMessage) {
    }

    public record ReapResult(
            Long tenantId,
            int authorizedRequeuedForPdf,
            int stuckDocumentsFlagged,
            LocalDateTime executedAt) {
    }
}

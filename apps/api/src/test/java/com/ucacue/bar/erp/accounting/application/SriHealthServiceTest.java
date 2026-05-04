package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.application.SriHealthService.HealthSnapshot;
import com.ucacue.bar.erp.accounting.application.SriHealthService.ReapResult;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SriHealthService — snapshot + reaper")
class SriHealthServiceTest {

    private static final Long TENANT = 9L;

    @Mock private TaxDocumentRepository taxDocumentRepository;
    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private SriAutomationService sriAutomationService;
    @Mock private AuditService auditService;

    @InjectMocks private SriHealthService service;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(TENANT);
        tenant.setTenantCode("default");
        lenient().when(tenantContextResolver.resolveCurrent()).thenReturn(tenant);

        lenient().when(taxDocumentRepository.healthSummaryByTenantId(TENANT))
                .thenReturn(new Object[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L});
        lenient().when(taxDocumentRepository
                .findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
                        eq(TENANT), anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of());
        lenient().when(taxDocumentRepository
                .findTop20ByTenantIdAndStatusAndRidePdfPathIsNullOrderByCreatedAtAsc(
                        eq(TENANT), any(TaxDocumentStatus.class)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("snapshot aggregates per-status counts, email pipeline counts and stuck documents")
    void snapshot_aggregatesAll() {
        when(taxDocumentRepository.healthSummaryByTenantId(TENANT))
                .thenReturn(new Object[]{0L, 0L, 0L, 4L, 12L, 0L, 0L, 1L, 2L, 1L, 3L, 8L});

        TaxDocumentEntity stuck = document(101L, "01", "000000123", TaxDocumentStatus.SENT,
                LocalDateTime.now().minusHours(6), "EN_PROCESO");
        when(taxDocumentRepository.findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
                eq(TENANT), anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(stuck));

        HealthSnapshot snapshot = service.snapshotForCurrentTenant();

        assertThat(snapshot.tenantId()).isEqualTo(TENANT);
        assertThat(snapshot.documentsByStatus())
                .containsEntry("AUTHORIZED", 12L)
                .containsEntry("SENT", 4L)
                .containsEntry("DRAFT", 0L);
        assertThat(snapshot.emailRetryPending()).isEqualTo(2L);
        assertThat(snapshot.emailPdfFailed()).isEqualTo(1L);
        assertThat(snapshot.emailNoCustomerEmail()).isEqualTo(3L);
        assertThat(snapshot.emailDelivered()).isEqualTo(8L);
        assertThat(snapshot.authorizedAwaitingPdf()).isEqualTo(1);
        assertThat(snapshot.stuckDocuments()).hasSize(1);
        assertThat(snapshot.stuckDocuments().get(0).id()).isEqualTo(101L);
        assertThat(snapshot.stuckDocuments().get(0).documentCode()).isEqualTo("01");
        assertThat(snapshot.stuckDocuments().get(0).status()).isEqualTo("SENT");
        assertThat(snapshot.stuckThresholdMinutes()).isEqualTo(120L);
    }

    @Test
    @DisplayName("reap calls automation for AUTHORIZED documents missing RIDE PDF and audits the run")
    void reap_requeuesAuthorizedAndAudits() {
        TaxDocumentEntity awaiting1 = document(11L, "01", "000000011", TaxDocumentStatus.AUTHORIZED,
                LocalDateTime.now().minusHours(1), null);
        TaxDocumentEntity awaiting2 = document(12L, "01", "000000012", TaxDocumentStatus.AUTHORIZED,
                LocalDateTime.now().minusHours(1), null);
        when(taxDocumentRepository.findTop20ByTenantIdAndStatusAndRidePdfPathIsNullOrderByCreatedAtAsc(
                TENANT, TaxDocumentStatus.AUTHORIZED)).thenReturn(List.of(awaiting1, awaiting2));
        when(sriAutomationService.processAuthorizedDocument(eq(TENANT), any(Long.class)))
                .thenReturn(new SriAutomationService.AutomationResult(11L, "SENT", "ok"));

        ReapResult result = service.reapForCurrentTenant();

        assertThat(result.authorizedRequeuedForPdf()).isEqualTo(2);
        assertThat(result.stuckDocumentsFlagged()).isEqualTo(0);
        verify(sriAutomationService).processAuthorizedDocument(TENANT, 11L);
        verify(sriAutomationService).processAuthorizedDocument(TENANT, 12L);
        verify(auditService, times(1)).logAction(any(), eq("SRI_HEALTH_REAP"), eq("TAX_DOCUMENT"), any(), any());
    }

    @Test
    @DisplayName("reap audits stuck flag with comma-separated ids when transient docs are found")
    void reap_flagsStuckDocuments() {
        TaxDocumentEntity stuck1 = document(31L, "01", "000000031", TaxDocumentStatus.SENT,
                LocalDateTime.now().minusHours(5), "EN_PROCESO");
        TaxDocumentEntity stuck2 = document(32L, "01", "000000032", TaxDocumentStatus.READY_TO_SEND,
                LocalDateTime.now().minusHours(4), null);
        when(taxDocumentRepository.findTop20ByTenantIdAndStatusInAndLastStatusAtBeforeOrderByLastStatusAtAsc(
                eq(TENANT), anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(stuck1, stuck2));

        ReapResult result = service.reapForCurrentTenant();

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditService).logAction(any(), eq("SRI_HEALTH_REAP"), eq("TAX_DOCUMENT"), any(), details.capture());
        assertThat(details.getValue())
                .contains("tenant=9")
                .contains("stuck=31,32");
        assertThat(result.stuckDocumentsFlagged()).isEqualTo(2);
        assertThat(result.authorizedRequeuedForPdf()).isEqualTo(0);
    }

    @Test
    @DisplayName("reap is a no-op when nothing needs attention and skips the audit log")
    void reap_noopWhenClean() {
        ReapResult result = service.reapForCurrentTenant();

        assertThat(result.authorizedRequeuedForPdf()).isZero();
        assertThat(result.stuckDocumentsFlagged()).isZero();
        verifyNoInteractions(auditService);
        verifyNoInteractions(sriAutomationService);
    }

    @Test
    @DisplayName("reap continues processing the rest of the batch when one automation call fails")
    void reap_continuesAfterFailure() {
        TaxDocumentEntity a = document(50L, "01", "000000050", TaxDocumentStatus.AUTHORIZED,
                LocalDateTime.now(), null);
        TaxDocumentEntity b = document(51L, "01", "000000051", TaxDocumentStatus.AUTHORIZED,
                LocalDateTime.now(), null);
        when(taxDocumentRepository.findTop20ByTenantIdAndStatusAndRidePdfPathIsNullOrderByCreatedAtAsc(
                TENANT, TaxDocumentStatus.AUTHORIZED)).thenReturn(List.of(a, b));
        when(sriAutomationService.processAuthorizedDocument(TENANT, 50L))
                .thenThrow(new RuntimeException("boom"));
        when(sriAutomationService.processAuthorizedDocument(TENANT, 51L))
                .thenReturn(new SriAutomationService.AutomationResult(51L, "SENT", "ok"));

        ReapResult result = service.reapForCurrentTenant();

        assertThat(result.authorizedRequeuedForPdf()).isEqualTo(1);
        verify(sriAutomationService).processAuthorizedDocument(TENANT, 50L);
        verify(sriAutomationService).processAuthorizedDocument(TENANT, 51L);
    }

    private TaxDocumentEntity document(Long id, String docCode, String sequential, TaxDocumentStatus status,
                                       LocalDateTime lastStatusAt, String providerMessage) {
        AccountingDocumentTypeEntity type = new AccountingDocumentTypeEntity();
        type.setDocumentCode(docCode);
        TaxDocumentEntity d = new TaxDocumentEntity();
        d.setId(id);
        d.setTenantId(TENANT);
        d.setSequentialNumber(sequential);
        d.setStatus(status);
        d.setDocumentType(type);
        d.setLastStatusAt(lastStatusAt);
        d.setLastProviderMessage(providerMessage);
        return d;
    }

    @SuppressWarnings("unused")
    private static Collection<TaxDocumentStatus> anyTransientStatuses() {
        return List.of();
    }
}

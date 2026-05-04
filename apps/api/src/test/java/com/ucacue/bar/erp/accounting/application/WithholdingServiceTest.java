package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.application.WithholdingService.ConceptRequest;
import com.ucacue.bar.erp.accounting.application.WithholdingService.ProposeRequest;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingDocumentStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingSupplierRegime;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.WithholdingTaxType;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingMatrixEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingMatrixRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.repository.PurchaseRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithholdingService — propose / confirm / cancel flow")
class WithholdingServiceTest {

    private static final Long TENANT = 1L;
    private static final Long PURCHASE = 100L;

    @Mock private WithholdingMatrixRepository matrixRepository;
    @Mock private WithholdingDocumentRepository documentRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private AuditService auditService;

    @InjectMocks private WithholdingService service;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(TENANT);
        tenant.setTenantCode("default");
        lenient().when(tenantContextResolver.resolveCurrent()).thenReturn(tenant);

        PurchaseEntity purchase = purchase(PURCHASE, "0987654321001", "Distribuidora ACME",
                new BigDecimal("1000.00"), new BigDecimal("150.00"));
        lenient().when(purchaseRepository.findById(PURCHASE)).thenReturn(Optional.of(purchase));
        lenient().when(documentRepository.findByTenantIdAndPurchaseId(TENANT, PURCHASE)).thenReturn(List.of());

        lenient().when(documentRepository.save(any(WithholdingDocumentEntity.class)))
                .thenAnswer(inv -> {
                    WithholdingDocumentEntity d = inv.getArgument(0);
                    if (d.getId() == null) d.setId(55L);
                    return d;
                });
    }

    private PurchaseEntity purchase(Long id, String rucProveedor, String name, BigDecimal subtotal, BigDecimal tax) {
        PurchaseEntity p = new PurchaseEntity();
        p.setId(id);
        p.setTenantId(TENANT);
        p.setSupplierIdentification(rucProveedor);
        p.setSupplierName(name);
        p.setSubtotal(subtotal);
        p.setTax(tax);
        p.setTotal(subtotal.add(tax));
        return p;
    }

    private WithholdingMatrixEntity matrix(Long id, String code, WithholdingTaxType type,
                                           WithholdingSupplierRegime regime, BigDecimal rate, BigDecimal min) {
        WithholdingMatrixEntity m = new WithholdingMatrixEntity();
        m.setId(id);
        m.setTenantId(TENANT);
        m.setConceptCode(code);
        m.setConceptName("concept-" + code);
        m.setTaxType(type);
        m.setSupplierRegime(regime);
        m.setRatePercentage(rate);
        m.setMinBaseAmount(min);
        m.setActive(true);
        return m;
    }

    @Test
    @DisplayName("propose: income + VAT concepts computed from purchase subtotal and tax")
    void propose_computesIncomeAndVat() {
        WithholdingMatrixEntity income = matrix(1L, "312", WithholdingTaxType.INCOME,
                WithholdingSupplierRegime.COMPANY, new BigDecimal("1.75"), BigDecimal.ZERO);
        WithholdingMatrixEntity vat = matrix(2L, "721", WithholdingTaxType.VAT,
                WithholdingSupplierRegime.COMPANY, new BigDecimal("30"), BigDecimal.ZERO);
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                TENANT, "312", WithholdingSupplierRegime.COMPANY, WithholdingTaxType.INCOME))
                .thenReturn(Optional.of(income));
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                TENANT, "721", WithholdingSupplierRegime.COMPANY, WithholdingTaxType.VAT))
                .thenReturn(Optional.of(vat));

        WithholdingDocumentEntity doc = service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, LocalDate.of(2026, 4, 19), List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, "01", "001-001-000000001"),
                new ConceptRequest("721", WithholdingTaxType.VAT, null, "01", "001-001-000000001")
        )));

        assertThat(doc.getStatus()).isEqualTo(WithholdingDocumentStatus.PROPOSED);
        assertThat(doc.getLines()).hasSize(2);
        assertThat(doc.getIncomeWithheld()).isEqualByComparingTo("17.50");   // 1000 * 1.75%
        assertThat(doc.getIvaWithheld()).isEqualByComparingTo("45.00");      // 150 * 30%
        assertThat(doc.getTotalWithheld()).isEqualByComparingTo("62.50");
        assertThat(doc.getSupplierIdentification()).isEqualTo("0987654321001");
    }

    @Test
    @DisplayName("propose: baseOverride replaces the auto-resolved purchase amount")
    void propose_baseOverrideTakesPrecedence() {
        WithholdingMatrixEntity income = matrix(1L, "303", WithholdingTaxType.INCOME,
                WithholdingSupplierRegime.NATURAL_PERSON, new BigDecimal("10"), BigDecimal.ZERO);
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                TENANT, "303", WithholdingSupplierRegime.NATURAL_PERSON, WithholdingTaxType.INCOME))
                .thenReturn(Optional.of(income));

        WithholdingDocumentEntity doc = service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.NATURAL_PERSON, null, List.of(
                new ConceptRequest("303", WithholdingTaxType.INCOME, new BigDecimal("500.00"), null, null)
        )));

        assertThat(doc.getLines()).singleElement()
                .extracting("baseAmount", "withheldAmount")
                .containsExactly(new BigDecimal("500.00"), new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("propose: base below min threshold skips line; throws if all skipped")
    void propose_allBelowThreshold_throws() {
        WithholdingMatrixEntity income = matrix(1L, "312", WithholdingTaxType.INCOME,
                WithholdingSupplierRegime.COMPANY, new BigDecimal("1.75"), new BigDecimal("99999.00"));
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                TENANT, "312", WithholdingSupplierRegime.COMPANY, WithholdingTaxType.INCOME))
                .thenReturn(Optional.of(income));

        assertThatThrownBy(() -> service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, null, List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, null, null)
        ))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("umbral minimo");
    }

    @Test
    @DisplayName("propose: duplicate active retention for same purchase rejected")
    void propose_duplicateRejected() {
        WithholdingDocumentEntity existing = new WithholdingDocumentEntity();
        existing.setStatus(WithholdingDocumentStatus.PROPOSED);
        when(documentRepository.findByTenantIdAndPurchaseId(TENANT, PURCHASE))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, null, List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, null, null)
        ))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Ya existe una retencion");
    }

    @Test
    @DisplayName("propose: cancelled prior retention does not block new proposal")
    void propose_cancelledPriorDoesNotBlock() {
        WithholdingDocumentEntity cancelled = new WithholdingDocumentEntity();
        cancelled.setStatus(WithholdingDocumentStatus.CANCELLED);
        when(documentRepository.findByTenantIdAndPurchaseId(TENANT, PURCHASE))
                .thenReturn(List.of(cancelled));
        WithholdingMatrixEntity income = matrix(1L, "312", WithholdingTaxType.INCOME,
                WithholdingSupplierRegime.COMPANY, new BigDecimal("1.75"), BigDecimal.ZERO);
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                TENANT, "312", WithholdingSupplierRegime.COMPANY, WithholdingTaxType.INCOME))
                .thenReturn(Optional.of(income));

        WithholdingDocumentEntity doc = service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, null, List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, null, null)
        )));

        assertThat(doc.getStatus()).isEqualTo(WithholdingDocumentStatus.PROPOSED);
    }

    @Test
    @DisplayName("propose: unknown matrix code — BadRequestException")
    void propose_unknownConcept_throws() {
        when(matrixRepository.findByTenantIdAndConceptCodeAndSupplierRegimeAndTaxType(
                any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, null, List.of(
                new ConceptRequest("999", WithholdingTaxType.INCOME, null, null, null)
        ))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No existe matriz");
    }

    @Test
    @DisplayName("propose: missing regime — BadRequestException")
    void propose_missingRegime_throws() {
        assertThatThrownBy(() -> service.proposeForPurchase(PURCHASE, new ProposeRequest(
                null, null, List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, null, null)
        ))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("regimen");
    }

    @Test
    @DisplayName("propose: purchase of another tenant invisible")
    void propose_purchaseCrossTenant_notFound() {
        PurchaseEntity other = purchase(PURCHASE, "x", "other", new BigDecimal("100"), new BigDecimal("15"));
        other.setTenantId(999L);
        when(purchaseRepository.findById(PURCHASE)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.proposeForPurchase(PURCHASE, new ProposeRequest(
                WithholdingSupplierRegime.COMPANY, null, List.of(
                new ConceptRequest("312", WithholdingTaxType.INCOME, null, null, null)
        ))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("confirm: PROPOSED → CONFIRMED")
    void confirm_transitionsStatus() {
        WithholdingDocumentEntity doc = new WithholdingDocumentEntity();
        doc.setId(55L);
        doc.setTenantId(TENANT);
        doc.setStatus(WithholdingDocumentStatus.PROPOSED);
        when(documentRepository.findByTenantIdAndId(TENANT, 55L)).thenReturn(Optional.of(doc));

        WithholdingDocumentEntity confirmed = service.confirm(55L);

        assertThat(confirmed.getStatus()).isEqualTo(WithholdingDocumentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirm: non-PROPOSED rejected")
    void confirm_nonProposedRejected() {
        WithholdingDocumentEntity doc = new WithholdingDocumentEntity();
        doc.setId(55L);
        doc.setTenantId(TENANT);
        doc.setStatus(WithholdingDocumentStatus.AUTHORIZED);
        when(documentRepository.findByTenantIdAndId(TENANT, 55L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.confirm(55L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cancel: records reason and timestamp")
    void cancel_recordsReasonAndTimestamp() {
        WithholdingDocumentEntity doc = new WithholdingDocumentEntity();
        doc.setId(55L);
        doc.setTenantId(TENANT);
        doc.setStatus(WithholdingDocumentStatus.PROPOSED);
        when(documentRepository.findByTenantIdAndId(TENANT, 55L)).thenReturn(Optional.of(doc));

        WithholdingDocumentEntity cancelled = service.cancel(55L, "error de datos");

        assertThat(cancelled.getStatus()).isEqualTo(WithholdingDocumentStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("error de datos");
        assertThat(cancelled.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel: authorized retention cannot be cancelled")
    void cancel_authorizedRejected() {
        WithholdingDocumentEntity doc = new WithholdingDocumentEntity();
        doc.setId(55L);
        doc.setTenantId(TENANT);
        doc.setStatus(WithholdingDocumentStatus.AUTHORIZED);
        when(documentRepository.findByTenantIdAndId(TENANT, 55L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.cancel(55L, "any"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("autorizada");
    }
}

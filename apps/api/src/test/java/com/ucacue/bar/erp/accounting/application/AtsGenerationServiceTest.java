package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AtsPeriodStatus;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AtsPeriodEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.WithholdingDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AtsPeriodRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.WithholdingDocumentRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.entity.PurchaseEntity;
import com.ucacue.bar.erp.purchases.infrastructure.persistence.repository.PurchaseRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtsGenerationService — generate, submit, close flow")
class AtsGenerationServiceTest {

    private static final Long TENANT = 1L;

    @Mock private AtsPeriodRepository atsPeriodRepository;
    @Mock private TaxDocumentRepository taxDocumentRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private WithholdingDocumentRepository withholdingDocumentRepository;
    @Mock private SriConfigurationRepository sriConfigurationRepository;
    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private AuditService auditService;

    @InjectMocks private AtsGenerationService service;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(TENANT);
        tenant.setTenantCode("default");
        tenant.setLegalName("UCACUE BAR SA");
        lenient().when(tenantContextResolver.resolveCurrent()).thenReturn(tenant);

        SriConfigurationEntity cfg = new SriConfigurationEntity();
        cfg.setTenantId(TENANT);
        cfg.setIssuerRuc("1790012345001");
        cfg.setIssuerLegalName("UCACUE BAR SA");
        lenient().when(sriConfigurationRepository.findByTenantId(TENANT)).thenReturn(Optional.of(cfg));

        lenient().when(atsPeriodRepository.save(any(AtsPeriodEntity.class))).thenAnswer(inv -> {
            AtsPeriodEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(77L);
            return e;
        });
    }

    private TaxDocumentEntity sale(Long id, LocalDate issueDate, String buyerRuc, BigDecimal subtotal, BigDecimal tax) {
        TaxDocumentEntity d = new TaxDocumentEntity();
        d.setId(id);
        d.setTenantId(TENANT);
        d.setSourceModule(AccountingSourceModule.POS);
        d.setSourceType("ORDER");
        d.setSourceId(id);
        d.setStatus(TaxDocumentStatus.AUTHORIZED);
        d.setIssueDate(issueDate);
        d.setBuyerIdentification(buyerRuc);
        d.setBuyerName("Cliente " + id);
        d.setSubtotalAmount(subtotal);
        d.setTaxAmount(tax);
        d.setTotalAmount(subtotal.add(tax));
        return d;
    }

    private PurchaseEntity purchase(Long id, LocalDateTime receivedAt, String supplierRuc, BigDecimal subtotal, BigDecimal tax) {
        PurchaseEntity p = new PurchaseEntity();
        p.setId(id);
        p.setTenantId(TENANT);
        p.setSupplierIdentification(supplierRuc);
        p.setSupplierName("Proveedor " + id);
        p.setExternalDocumentNumber("001-001-00000001" + id);
        p.setReceivedAt(receivedAt);
        p.setSubtotal(subtotal);
        p.setTax(tax);
        p.setTotal(subtotal.add(tax));
        return p;
    }

    private WithholdingDocumentEntity withholding(Long id, LocalDate issueDate, String supplierRuc, BigDecimal total) {
        WithholdingDocumentEntity w = new WithholdingDocumentEntity();
        w.setId(id);
        w.setTenantId(TENANT);
        w.setSupplierIdentification(supplierRuc);
        w.setIssueDate(issueDate);
        w.setSequentialNumber("000000" + id);
        w.setTotalWithheld(total);
        return w;
    }

    @Test
    @DisplayName("generate: aggregates sales/purchases/withholdings and produces XML with totals")
    void generate_aggregatesAndXml() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        List<TaxDocumentEntity> sales = List.of(
                sale(1L, LocalDate.of(2026, 3, 5), "0987654321001",
                        new BigDecimal("100.00"), new BigDecimal("15.00")),
                sale(2L, LocalDate.of(2026, 3, 20), "1710123456",
                        new BigDecimal("50.00"), new BigDecimal("7.50")));
        List<PurchaseEntity> purchases = List.of(
                purchase(10L, LocalDateTime.of(2026, 3, 10, 9, 0), "0990000001001",
                        new BigDecimal("200.00"), new BigDecimal("30.00")));
        List<WithholdingDocumentEntity> withholdings = List.of(
                withholding(20L, LocalDate.of(2026, 3, 11), "0990000001001", new BigDecimal("9.00")));

        when(atsPeriodRepository.findForUpdate(TENANT, 2026, 3))
                .thenReturn(Optional.empty());
        when(taxDocumentRepository.findByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateAsc(
                TENANT, TaxDocumentStatus.AUTHORIZED, from, to)).thenReturn(sales);
        when(purchaseRepository.findReceivedBetweenByTenantId(
                eq(TENANT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(purchases);
        when(withholdingDocumentRepository.findByTenantIdAndIssueDateBetweenOrderByIssueDateAsc(
                TENANT, from, to)).thenReturn(withholdings);

        AtsPeriodEntity result = service.generate(2026, 3);

        assertThat(result.getStatus()).isEqualTo(AtsPeriodStatus.GENERATED);
        assertThat(result.getSalesCount()).isEqualTo(2);
        assertThat(result.getPurchasesCount()).isEqualTo(1);
        assertThat(result.getWithholdingsCount()).isEqualTo(1);
        assertThat(result.getTotalSales()).isEqualByComparingTo("172.50");
        assertThat(result.getTotalPurchases()).isEqualByComparingTo("230.00");
        assertThat(result.getTotalWithheld()).isEqualByComparingTo("9.00");
        assertThat(result.getGeneratedAt()).isNotNull();

        String xml = result.getXmlPayload();
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<IdInformante>1790012345001</IdInformante>");
        assertThat(xml).contains("<razonSocial>UCACUE BAR SA</razonSocial>");
        assertThat(xml).contains("<Anio>2026</Anio>");
        assertThat(xml).contains("<Mes>03</Mes>");
        assertThat(xml).contains("<ventas>");
        assertThat(xml).contains("<compras>");
        assertThat(xml).contains("<retenciones>");
        assertThat(xml).contains("0987654321001");
    }

    @Test
    @DisplayName("generate: empty period still produces valid XML with zero totals")
    void generate_emptyPeriod() {
        when(atsPeriodRepository.findForUpdate(TENANT, 2026, 1))
                .thenReturn(Optional.empty());
        when(taxDocumentRepository.findByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateAsc(
                eq(TENANT), eq(TaxDocumentStatus.AUTHORIZED), any(), any())).thenReturn(List.of());
        when(purchaseRepository.findReceivedBetweenByTenantId(
                eq(TENANT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(withholdingDocumentRepository.findByTenantIdAndIssueDateBetweenOrderByIssueDateAsc(
                eq(TENANT), any(), any())).thenReturn(List.of());

        AtsPeriodEntity result = service.generate(2026, 1);

        assertThat(result.getStatus()).isEqualTo(AtsPeriodStatus.GENERATED);
        assertThat(result.getSalesCount()).isZero();
        assertThat(result.getPurchasesCount()).isZero();
        assertThat(result.getWithholdingsCount()).isZero();
        assertThat(result.getTotalSales()).isEqualByComparingTo("0");
        assertThat(result.getXmlPayload()).contains("<Mes>01</Mes>").doesNotContain("<ventas>");
    }

    @Test
    @DisplayName("generate: regenerating an existing DRAFT period updates the same row")
    void generate_updatesExistingDraft() {
        AtsPeriodEntity existing = new AtsPeriodEntity();
        existing.setId(99L);
        existing.setTenantId(TENANT);
        existing.setFiscalYear(2026);
        existing.setFiscalMonth(2);
        existing.setStatus(AtsPeriodStatus.DRAFT);
        when(atsPeriodRepository.findForUpdate(TENANT, 2026, 2))
                .thenReturn(Optional.of(existing));
        when(taxDocumentRepository.findByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateAsc(
                eq(TENANT), eq(TaxDocumentStatus.AUTHORIZED), any(), any())).thenReturn(List.of());
        when(purchaseRepository.findReceivedBetweenByTenantId(
                eq(TENANT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(withholdingDocumentRepository.findByTenantIdAndIssueDateBetweenOrderByIssueDateAsc(
                eq(TENANT), any(), any())).thenReturn(List.of());

        AtsPeriodEntity result = service.generate(2026, 2);

        ArgumentCaptor<AtsPeriodEntity> captor = ArgumentCaptor.forClass(AtsPeriodEntity.class);
        org.mockito.Mockito.verify(atsPeriodRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(AtsPeriodStatus.GENERATED);
    }

    @Test
    @DisplayName("generate: period already SUBMITTED cannot be regenerated")
    void generate_submittedCannotRegenerate() {
        AtsPeriodEntity existing = new AtsPeriodEntity();
        existing.setId(99L);
        existing.setTenantId(TENANT);
        existing.setFiscalYear(2026);
        existing.setFiscalMonth(2);
        existing.setStatus(AtsPeriodStatus.SUBMITTED);
        when(atsPeriodRepository.findForUpdate(TENANT, 2026, 2))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.generate(2026, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SUBMITTED");
    }

    @Test
    @DisplayName("generate: invalid month rejected")
    void generate_invalidMonth() {
        assertThatThrownBy(() -> service.generate(2026, 13))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("markSubmitted: GENERATED → SUBMITTED with timestamp")
    void markSubmitted_transitions() {
        AtsPeriodEntity entity = new AtsPeriodEntity();
        entity.setId(77L);
        entity.setTenantId(TENANT);
        entity.setStatus(AtsPeriodStatus.GENERATED);
        when(atsPeriodRepository.findByTenantIdAndId(TENANT, 77L)).thenReturn(Optional.of(entity));

        AtsPeriodEntity result = service.markSubmitted(77L);

        assertThat(result.getStatus()).isEqualTo(AtsPeriodStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("markSubmitted: DRAFT cannot be marked submitted")
    void markSubmitted_draftRejected() {
        AtsPeriodEntity entity = new AtsPeriodEntity();
        entity.setId(77L);
        entity.setTenantId(TENANT);
        entity.setStatus(AtsPeriodStatus.DRAFT);
        when(atsPeriodRepository.findByTenantIdAndId(TENANT, 77L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.markSubmitted(77L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("close: SUBMITTED → CLOSED")
    void close_transitions() {
        AtsPeriodEntity entity = new AtsPeriodEntity();
        entity.setId(77L);
        entity.setTenantId(TENANT);
        entity.setStatus(AtsPeriodStatus.SUBMITTED);
        when(atsPeriodRepository.findByTenantIdAndId(TENANT, 77L)).thenReturn(Optional.of(entity));

        AtsPeriodEntity result = service.close(77L);

        assertThat(result.getStatus()).isEqualTo(AtsPeriodStatus.CLOSED);
    }
}

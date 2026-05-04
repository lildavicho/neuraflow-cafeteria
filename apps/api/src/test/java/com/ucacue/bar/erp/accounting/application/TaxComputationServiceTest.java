package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.entity.ProductEntity;
import com.ucacue.bar.erp.accounting.application.TaxComputationService.TaxLineResult;
import com.ucacue.bar.erp.accounting.application.TaxComputationService.TaxSummary;
import com.ucacue.bar.erp.accounting.application.TaxComputationService.TaxableLine;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ProductTaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ProductTaxRuleRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxComputationService — fiscalDate aware + mixed baskets + overrides")
class TaxComputationServiceTest {

    private static final Long TENANT = 1L;
    private static final Long PRODUCT_IVA = 10L;
    private static final Long PRODUCT_EXEMPT = 20L;
    private static final LocalDate D_2024 = LocalDate.of(2024, 3, 15);
    private static final LocalDate D_2026 = LocalDate.of(2026, 3, 15);

    @Mock private TaxRuleRepository taxRuleRepository;
    @Mock private ProductTaxRuleRepository productTaxRuleRepository;

    private TaxComputationService service;

    @BeforeEach
    void setUp() {
        service = new TaxComputationService(productTaxRuleRepository, taxRuleRepository);
        lenient().when(productTaxRuleRepository.findByTenantIdAndProductIdInAndActiveTrue(eq(TENANT), any()))
                .thenReturn(List.of());
    }

    private TaxRuleEntity rule(long id, String code, BigDecimal rate, LocalDate from, LocalDate until) {
        TaxRuleEntity r = new TaxRuleEntity();
        r.setId(id);
        r.setTenantId(TENANT);
        r.setTaxCode(code);
        r.setTaxName("IVA " + code);
        r.setTaxScope(TaxScope.DOCUMENT);
        r.setComputationType(TaxComputationType.PERCENTAGE);
        r.setRate(rate);
        r.setActive(true);
        r.setValidFrom(from);
        r.setValidUntil(until);
        r.setIncludedInPrice(false);
        return r;
    }

    @Test
    @DisplayName("IVA 15% on single line — straight percentage")
    void iva15_singleLine() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 2, new BigDecimal("50.00"))), D_2026);

        assertThat(summary.subtotal()).isEqualByComparingTo("100.00");
        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
        assertThat(summary.total()).isEqualByComparingTo("115.00");
    }

    @Test
    @DisplayName("IVA 12% on backdated 2024 document — rule validity respected")
    void iva12_backdatedDocument() {
        TaxRuleEntity iva12 = rule(1L, "IVA-12", new BigDecimal("12"),
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 3, 31));
        TaxRuleEntity iva15 = rule(2L, "IVA-15", new BigDecimal("15"),
                LocalDate.of(2024, 4, 1), null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15, iva12));

        TaxSummary summary = service.calculate(TENANT,
                List.of(new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))), D_2024);

        assertThat(summary.taxAmount()).isEqualByComparingTo("12.00");
        assertThat(summary.total()).isEqualByComparingTo("112.00");
        assertThat(summary.lines()).singleElement()
                .extracting(TaxLineResult::taxRuleId).isEqualTo(1L);
    }

    @Test
    @DisplayName("IVA 15% on post-2024 document — same rule set picks the newer one")
    void iva15_currentDocument() {
        TaxRuleEntity iva12 = rule(1L, "IVA-12", new BigDecimal("12"),
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 3, 31));
        TaxRuleEntity iva15 = rule(2L, "IVA-15", new BigDecimal("15"),
                LocalDate.of(2024, 4, 1), null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15, iva12));

        TaxSummary summary = service.calculate(TENANT,
                List.of(new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))), D_2026);

        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
        assertThat(summary.lines()).singleElement()
                .extracting(TaxLineResult::taxRuleId).isEqualTo(2L);
    }

    @Test
    @DisplayName("Mixed basket — product override (0%) wins for one line, document IVA applies to rest")
    void mixedBasket_productOverrideWins() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        TaxRuleEntity exempt = rule(2L, "IVA-0", BigDecimal.ZERO, null, null);
        exempt.setTaxScope(TaxScope.PRODUCT);

        ProductEntity p = new ProductEntity();
        p.setId(PRODUCT_EXEMPT);
        ProductTaxRuleEntity override = new ProductTaxRuleEntity();
        override.setTenantId(TENANT);
        override.setProduct(p);
        override.setTaxRule(exempt);
        override.setActive(true);

        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));
        when(productTaxRuleRepository.findByTenantIdAndProductIdInAndActiveTrue(eq(TENANT), any()))
                .thenReturn(List.of(override));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00")),
                new TaxableLine(PRODUCT_EXEMPT, 1, new BigDecimal("50.00"))), D_2026);

        assertThat(summary.subtotal()).isEqualByComparingTo("150.00");
        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
        assertThat(summary.total()).isEqualByComparingTo("165.00");
    }

    @Test
    @DisplayName("Tax included in price — subtotal is divided out")
    void taxIncludedInPrice() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        iva15.setIncludedInPrice(true);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("115.00"))), D_2026);

        assertThat(summary.subtotal()).isEqualByComparingTo("100.00");
        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
        assertThat(summary.total()).isEqualByComparingTo("115.00");
    }

    @Test
    @DisplayName("Null fiscalDate falls back to today — still resolves current rule")
    void nullFiscalDate_fallsBackToToday() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"),
                LocalDate.now().minusYears(1), null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))), null);

        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("No rule valid on fiscalDate — BadRequestException")
    void noValidRule_throws() {
        TaxRuleEntity futureOnly = rule(1L, "IVA-15", new BigDecimal("15"),
                LocalDate.of(2030, 1, 1), null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(futureOnly));

        assertThatThrownBy(() -> service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))), D_2026))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No existe una regla tributaria activa");
    }

    @Test
    @DisplayName("Quantity <= 0 rejected")
    void invalidQuantity_throws() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        assertThatThrownBy(() -> service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 0, new BigDecimal("100.00"))), D_2026))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cantidad");
    }

    @Test
    @DisplayName("Negative price rejected")
    void negativePrice_throws() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        assertThatThrownBy(() -> service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("-5.00"))), D_2026))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("precio");
    }

    @Test
    @DisplayName("Inactive rule ignored even if dates match")
    void inactiveRule_ignored() {
        TaxRuleEntity active = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        TaxRuleEntity inactive = rule(2L, "IVA-LEGACY", new BigDecimal("12"), null, null);
        inactive.setActive(false);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(active, inactive));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))), D_2026);

        assertThat(summary.lines()).singleElement()
                .extracting(TaxLineResult::taxRuleId).isEqualTo(1L);
    }

    @Test
    @DisplayName("ICE fixed-amount rule — flat tax regardless of quantity/price gross")
    void fixedAmountTax() {
        TaxRuleEntity ice = rule(1L, "ICE-FIXED", new BigDecimal("7.50"), null, null);
        ice.setComputationType(TaxComputationType.FIXED);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(ice));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 3, new BigDecimal("100.00"))), D_2026);

        assertThat(summary.subtotal()).isEqualByComparingTo("300.00");
        assertThat(summary.taxAmount()).isEqualByComparingTo("7.50");
        assertThat(summary.total()).isEqualByComparingTo("307.50");
    }

    @Test
    @DisplayName("Backward-compat overload calculate(tenantId, lines) uses today")
    void backwardCompatOverload() {
        TaxRuleEntity iva15 = rule(1L, "IVA-15", new BigDecimal("15"), null, null);
        when(taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(TENANT, TaxScope.DOCUMENT))
                .thenReturn(List.of(iva15));

        TaxSummary summary = service.calculate(TENANT, List.of(
                new TaxableLine(PRODUCT_IVA, 1, new BigDecimal("100.00"))));

        assertThat(summary.taxAmount()).isEqualByComparingTo("15.00");
    }
}

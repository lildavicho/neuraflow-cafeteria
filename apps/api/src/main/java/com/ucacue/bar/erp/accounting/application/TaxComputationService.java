package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxComputationType;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxScope;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.ProductTaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxRuleEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.ProductTaxRuleRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxRuleRepository;
import com.ucacue.bar.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxComputationService {

    private final ProductTaxRuleRepository productTaxRuleRepository;
    private final TaxRuleRepository taxRuleRepository;

    public TaxSummary calculate(Long tenantId, List<TaxableLine> lines) {
        return calculate(tenantId, lines, LocalDate.now());
    }

    public TaxSummary calculate(Long tenantId, List<TaxableLine> lines, LocalDate fiscalDate) {
        LocalDate effectiveDate = fiscalDate != null ? fiscalDate : LocalDate.now();
        Map<Long, ProductTaxRuleEntity> productRules = loadProductRules(tenantId, lines);
        List<TaxRuleEntity> documentRules = taxRuleRepository.findByTenantIdAndTaxScopeAndActiveTrueOrderByRateDesc(
                tenantId, TaxScope.DOCUMENT);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<TaxLineResult> resolvedLines = lines.stream()
                .map(line -> {
                    if (line.quantity() == null || line.quantity() <= 0) {
                        throw new BadRequestException("La cantidad de la linea tributaria debe ser mayor a cero");
                    }
                    if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
                        throw new BadRequestException("El precio unitario de la linea tributaria no puede ser negativo");
                    }
                    BigDecimal gross = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    TaxRuleEntity rule = resolveRule(productRules.get(line.productId()), documentRules, effectiveDate);
                    TaxLineResult computed = computeLine(line.productId(), gross, rule);
                    return computed;
                })
                .toList();

        for (TaxLineResult line : resolvedLines) {
            subtotal = subtotal.add(line.subtotalAmount());
            totalTax = totalTax.add(line.taxAmount());
        }

        return new TaxSummary(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                totalTax.setScale(2, RoundingMode.HALF_UP),
                subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP),
                resolvedLines);
    }

    private Map<Long, ProductTaxRuleEntity> loadProductRules(Long tenantId, List<TaxableLine> lines) {
        List<Long> productIds = lines.stream()
                .map(TaxableLine::productId)
                .distinct()
                .toList();
        Map<Long, ProductTaxRuleEntity> mapping = new HashMap<>();
        for (ProductTaxRuleEntity row : productTaxRuleRepository.findByTenantIdAndProductIdInAndActiveTrue(tenantId, productIds)) {
            mapping.put(row.getProduct().getId(), row);
        }
        return mapping;
    }

    private TaxRuleEntity resolveRule(ProductTaxRuleEntity productTaxRule, List<TaxRuleEntity> documentRules, LocalDate fiscalDate) {
        if (productTaxRule != null) {
            TaxRuleEntity taxRule = productTaxRule.getTaxRule();
            if (taxRule != null && isValidOn(taxRule, fiscalDate)) {
                return taxRule;
            }
        }
        return documentRules.stream()
                .filter(rule -> isValidOn(rule, fiscalDate))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No existe una regla tributaria activa para la fecha fiscal " + fiscalDate));
    }

    private boolean isValidOn(TaxRuleEntity taxRule, LocalDate date) {
        boolean starts = taxRule.getValidFrom() == null || !date.isBefore(taxRule.getValidFrom());
        boolean ends = taxRule.getValidUntil() == null || !date.isAfter(taxRule.getValidUntil());
        return Boolean.TRUE.equals(taxRule.getActive()) && starts && ends;
    }

    private TaxLineResult computeLine(Long productId, BigDecimal gross, TaxRuleEntity rule) {
        BigDecimal rate = rule.getRate() != null ? rule.getRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = switch (rule.getComputationType()) {
            case FIXED -> rate;
            case PERCENTAGE -> gross.multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        };

        BigDecimal subtotal = gross;
        if (Boolean.TRUE.equals(rule.getIncludedInPrice()) && rule.getComputationType() == TaxComputationType.PERCENTAGE) {
            BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            subtotal = gross.divide(divisor, 2, RoundingMode.HALF_UP);
            taxAmount = gross.subtract(subtotal).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = Boolean.TRUE.equals(rule.getIncludedInPrice())
                ? gross
                : subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new TaxLineResult(productId, rule.getId(), subtotal, taxAmount, total);
    }

    public record TaxableLine(Long productId, Integer quantity, BigDecimal unitPrice) {
    }

    public record TaxLineResult(
            Long productId,
            Long taxRuleId,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount) {
    }

    public record TaxSummary(
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal total,
            List<TaxLineResult> lines) {
    }
}

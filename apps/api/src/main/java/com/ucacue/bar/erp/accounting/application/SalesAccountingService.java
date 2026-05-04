package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.entity.OrderEntity;
import com.ucacue.bar.entity.OrderItemEntity;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.AccountingSourceModule;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountCatalogEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.AccountingDocumentTypeEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountCatalogRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingDocumentTypeRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.AccountingEntryRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.DocumentSequenceRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesAccountingService {

    private static final String SOURCE_TYPE_ORDER_SALE = "ORDER_SALE";
    private static final String SOURCE_TYPE_ORDER_REFUND = "ORDER_REFUND";
    private static final BigDecimal RECONCILE_TOLERANCE = new BigDecimal("0.02");
    private static final String ACCOUNT_CASH = "1.1.01.01";
    private static final String ACCOUNT_BANK = "1.1.01.02";
    private static final String ACCOUNT_TAX_RECEIVABLE = "1.1.04.01";
    private static final String ACCOUNT_INVENTORY = "1.1.03.01";
    private static final String ACCOUNT_PAYABLE_TAX = "2.1.02.01";
    private static final String ACCOUNT_SALES = "4.1.01.01";
    private static final String ACCOUNT_COGS = "5.1.01.01";

    private final TenantContextResolver tenantContextResolver;
    private final AccountCatalogRepository accountCatalogRepository;
    private final AccountingDocumentTypeRepository accountingDocumentTypeRepository;
    private final AccountingEntryRepository accountingEntryRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final TaxDocumentRepository taxDocumentRepository;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final AccountingPostingService accountingPostingService;
    private final TaxComputationService taxComputationService;

    @Transactional
    public void postPaidOrder(OrderEntity order) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (accountingEntryRepository.findByTenantIdAndSourceTypeAndSourceId(tenantId, SOURCE_TYPE_ORDER_SALE, order.getId()).isPresent()) {
            return;
        }

        ReconciledTotals totals = reconcileTotals(tenantId, order);

        List<AccountingPostingService.PostAccountingEntryLineCommand> lines = new ArrayList<>();
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(tenantId, resolveCashAccountCode(order)),
                null,
                totals.total(),
                BigDecimal.ZERO,
                "Cobro de venta orden " + order.getId(),
                "ORDER:" + order.getId()));
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(tenantId, ACCOUNT_SALES),
                null,
                BigDecimal.ZERO,
                totals.subtotal(),
                "Ingreso por venta orden " + order.getId(),
                "ORDER:" + order.getId()));
        if (totals.tax().signum() > 0) {
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_PAYABLE_TAX),
                    null,
                    BigDecimal.ZERO,
                    totals.tax(),
                    "IVA venta orden " + order.getId(),
                    "ORDER:" + order.getId()));
        }

        BigDecimal costOfGoods = costOfGoods(order);
        if (costOfGoods.signum() > 0) {
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_COGS),
                    null,
                    costOfGoods,
                    BigDecimal.ZERO,
                    "Costo de ventas orden " + order.getId(),
                    "ORDER:" + order.getId()));
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_INVENTORY),
                    null,
                    BigDecimal.ZERO,
                    costOfGoods,
                    "Salida de inventario orden " + order.getId(),
                    "ORDER:" + order.getId()));
        }

        AccountingPostingService.CreateTaxDocumentCommand taxCommand =
                order.getDocumentType() == OrderEntity.DocumentType.FACTURA
                        ? buildSalesTaxDocument(tenantId, order, totals)
                        : null;

        accountingPostingService.post(new AccountingPostingService.PostAccountingEntryCommand(
                tenantId,
                AccountingSourceModule.POS,
                SOURCE_TYPE_ORDER_SALE,
                order.getId(),
                order.getPaidAt() != null ? order.getPaidAt().toLocalDate() : LocalDate.now(),
                "Venta POS orden " + order.getId(),
                order.getPaymentReference(),
                lines,
                null,
                null,
                taxCommand));
    }

    @Transactional
    public void postRefund(OrderEntity order) {
        Long tenantId = tenantContextResolver.resolveCurrent().getId();
        if (accountingEntryRepository.findByTenantIdAndSourceTypeAndSourceId(tenantId, SOURCE_TYPE_ORDER_REFUND, order.getId()).isPresent()) {
            return;
        }

        ReconciledTotals totals = reconcileTotals(tenantId, order);

        List<AccountingPostingService.PostAccountingEntryLineCommand> lines = new ArrayList<>();
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(tenantId, ACCOUNT_SALES),
                null,
                totals.subtotal(),
                BigDecimal.ZERO,
                "Reversion ingreso venta orden " + order.getId(),
                "ORDER:" + order.getId()));
        if (totals.tax().signum() > 0) {
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_PAYABLE_TAX),
                    null,
                    totals.tax(),
                    BigDecimal.ZERO,
                    "Reversion IVA venta orden " + order.getId(),
                    "ORDER:" + order.getId()));
        }
        lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                resolveAccountId(tenantId, resolveCashAccountCode(order)),
                null,
                BigDecimal.ZERO,
                totals.total(),
                "Salida por reembolso orden " + order.getId(),
                "ORDER:" + order.getId()));

        BigDecimal costOfGoods = costOfGoods(order);
        if (costOfGoods.signum() > 0) {
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_INVENTORY),
                    null,
                    costOfGoods,
                    BigDecimal.ZERO,
                    "Retorno inventario reembolso orden " + order.getId(),
                    "ORDER:" + order.getId()));
            lines.add(new AccountingPostingService.PostAccountingEntryLineCommand(
                    resolveAccountId(tenantId, ACCOUNT_COGS),
                    null,
                    BigDecimal.ZERO,
                    costOfGoods,
                    "Reversion costo de ventas orden " + order.getId(),
                    "ORDER:" + order.getId()));
        }

        boolean originalWasInvoiced = taxDocumentRepository
                .findByTenantIdAndSourceTypeAndSourceId(tenantId, SOURCE_TYPE_ORDER_SALE, order.getId())
                .isPresent();

        AccountingPostingService.CreateTaxDocumentCommand creditNoteCommand =
                originalWasInvoiced ? buildCreditNoteDocument(tenantId, order, totals) : null;

        accountingPostingService.post(new AccountingPostingService.PostAccountingEntryCommand(
                tenantId,
                AccountingSourceModule.POS,
                SOURCE_TYPE_ORDER_REFUND,
                order.getId(),
                order.getRefundedAt() != null ? order.getRefundedAt().toLocalDate() : LocalDate.now(),
                "Reembolso POS orden " + order.getId(),
                order.getPaymentReference(),
                lines,
                null,
                null,
                creditNoteCommand));

        taxDocumentRepository.findByTenantIdAndSourceTypeAndSourceId(tenantId, SOURCE_TYPE_ORDER_SALE, order.getId())
                .ifPresent(document -> document.setStatus(com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus.CANCELLED));
    }

    private ReconciledTotals reconcileTotals(Long tenantId, OrderEntity order) {
        TaxComputationService.TaxSummary summary = taxComputationService.calculate(
                tenantId,
                order.getItems().stream()
                        .map(item -> new TaxComputationService.TaxableLine(
                                item.getProduct().getId(),
                                item.getQuantity(),
                                item.getUnitPrice()))
                        .toList());

        BigDecimal computedSubtotal = amount(summary.subtotal());
        BigDecimal computedTax = amount(summary.taxAmount());
        BigDecimal computedTotal = amount(summary.total());

        BigDecimal orderSubtotal = amount(order.getSubtotal());
        BigDecimal orderTax = amount(order.getTax());
        BigDecimal orderTotal = amount(order.getTotal());

        if (computedSubtotal.subtract(orderSubtotal).abs().compareTo(RECONCILE_TOLERANCE) > 0
                || computedTax.subtract(orderTax).abs().compareTo(RECONCILE_TOLERANCE) > 0
                || computedTotal.subtract(orderTotal).abs().compareTo(RECONCILE_TOLERANCE) > 0) {
            log.warn("Orden {} con totales inconsistentes. UI subtotal={} tax={} total={}; backend subtotal={} tax={} total={}. Se usaran los del backend.",
                    order.getId(), orderSubtotal, orderTax, orderTotal,
                    computedSubtotal, computedTax, computedTotal);
        }

        List<AccountingPostingService.CreateTaxDocumentTaxLineCommand> taxLines = buildAggregatedTaxLines(summary);
        return new ReconciledTotals(computedSubtotal, computedTax, computedTotal, taxLines);
    }

    private List<AccountingPostingService.CreateTaxDocumentTaxLineCommand> buildAggregatedTaxLines(TaxComputationService.TaxSummary summary) {
        Map<String, TaxAccumulator> grouped = new LinkedHashMap<>();
        for (TaxComputationService.TaxLineResult line : summary.lines()) {
            BigDecimal subtotal = amount(line.subtotalAmount());
            BigDecimal taxAmount = amount(line.taxAmount());
            BigDecimal percentage = resolveTaxPercentage(subtotal, taxAmount);
            String key = (line.taxRuleId() != null ? line.taxRuleId().toString() : "n/a") + "|" + percentage;
            TaxAccumulator accumulator = grouped.computeIfAbsent(key,
                    ignored -> new TaxAccumulator(line.taxRuleId(), BigDecimal.ZERO, BigDecimal.ZERO, percentage));
            accumulator.taxableBase = accumulator.taxableBase.add(subtotal);
            accumulator.taxAmount = accumulator.taxAmount.add(taxAmount);
        }
        return grouped.values().stream()
                .map(item -> new AccountingPostingService.CreateTaxDocumentTaxLineCommand(
                        item.taxRuleId,
                        amount(item.taxableBase),
                        amount(item.taxAmount),
                        item.percentage))
                .toList();
    }

    private AccountingPostingService.CreateTaxDocumentCommand buildSalesTaxDocument(Long tenantId,
                                                                                   OrderEntity order,
                                                                                   ReconciledTotals totals) {
        AccountingDocumentTypeEntity documentType = accountingDocumentTypeRepository
                .findByTenantIdAndDocumentCode(tenantId, "SRI_INVOICE")
                .orElseGet(() -> accountingDocumentTypeRepository.findByTenantIdAndDocumentCode(tenantId, "SALES_INVOICE")
                        .orElseThrow(() -> new NotFoundException("No existe tipo documental de factura configurado")));

        Long sequenceId = Boolean.TRUE.equals(documentType.getRequiresSequence())
                ? documentSequenceRepository.findByTenantIdAndDocumentTypeDocumentCodeAndActiveTrue(tenantId, documentType.getDocumentCode())
                .map(sequence -> sequence.getId())
                .orElseThrow(() -> new NotFoundException("No existe una secuencia tributaria activa para la factura de venta"))
                : null;

        return new AccountingPostingService.CreateTaxDocumentCommand(
                documentType,
                sequenceId,
                resolveEnvironmentCode(tenantId),
                order.getPaidAt() != null ? order.getPaidAt().toLocalDate() : LocalDate.now(),
                resolveBuyerIdentification(order),
                resolveBuyerName(order),
                totals.subtotal(),
                totals.tax(),
                totals.total(),
                null,
                null,
                totals.taxLines());
    }

    private AccountingPostingService.CreateTaxDocumentCommand buildCreditNoteDocument(Long tenantId,
                                                                                      OrderEntity order,
                                                                                      ReconciledTotals totals) {
        AccountingDocumentTypeEntity documentType = accountingDocumentTypeRepository
                .findByTenantIdAndDocumentCode(tenantId, "SRI_CREDIT_NOTE")
                .orElseThrow(() -> new NotFoundException("No existe tipo documental de nota de credito configurado"));

        Long sequenceId = Boolean.TRUE.equals(documentType.getRequiresSequence())
                ? documentSequenceRepository.findByTenantIdAndDocumentTypeDocumentCodeAndActiveTrue(tenantId, documentType.getDocumentCode())
                .map(sequence -> sequence.getId())
                .orElseThrow(() -> new NotFoundException("No existe una secuencia tributaria activa para la nota de credito"))
                : null;

        return new AccountingPostingService.CreateTaxDocumentCommand(
                documentType,
                sequenceId,
                resolveEnvironmentCode(tenantId),
                order.getRefundedAt() != null ? order.getRefundedAt().toLocalDate() : LocalDate.now(),
                resolveBuyerIdentification(order),
                resolveBuyerName(order),
                totals.subtotal(),
                totals.tax(),
                totals.total(),
                null,
                null,
                totals.taxLines());
    }

    private String resolveBuyerIdentification(OrderEntity order) {
        if (order.getCustomerIdentification() != null && !order.getCustomerIdentification().isBlank()) {
            return order.getCustomerIdentification().trim();
        }
        if (order.getUser() != null && order.getUser().getIdentification() != null
                && !order.getUser().getIdentification().isBlank()) {
            return order.getUser().getIdentification().trim();
        }
        return "9999999999999";
    }

    private String resolveBuyerName(OrderEntity order) {
        if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            return order.getCustomerName().trim();
        }
        if (order.getUser() != null && order.getUser().getName() != null && !order.getUser().getName().isBlank()) {
            return order.getUser().getName().trim();
        }
        return "CONSUMIDOR FINAL";
    }

    private String resolveCashAccountCode(OrderEntity order) {
        return switch (order.getPaymentMethod()) {
            case CASH -> ACCOUNT_CASH;
            case CARD, TRANSFER -> ACCOUNT_BANK;
        };
    }

    private BigDecimal costOfGoods(OrderEntity order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemEntity item : order.getItems()) {
            BigDecimal unitCost = item.getProduct().getPurchasePrice() != null ? item.getProduct().getPurchasePrice() : BigDecimal.ZERO;
            total = total.add(unitCost.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return amount(total);
    }

    private BigDecimal resolveTaxPercentage(BigDecimal subtotal, BigDecimal taxAmount) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) == 0 || taxAmount == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return taxAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(subtotal, 4, RoundingMode.HALF_UP);
    }

    private Long resolveAccountId(Long tenantId, String accountCode) {
        AccountCatalogEntity account = accountCatalogRepository.findByTenantIdAndAccountCode(tenantId, accountCode)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta contable configurada: " + accountCode));
        return account.getId();
    }

    private BigDecimal amount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveEnvironmentCode(Long tenantId) {
        SriConfigurationEntity configuration = sriConfigurationRepository.findByTenantId(tenantId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new NotFoundException("No existe configuracion SRI activa para el tenant"));
        return configuration.getEnvironmentCode();
    }

    private record ReconciledTotals(
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            List<AccountingPostingService.CreateTaxDocumentTaxLineCommand> taxLines) {
    }

    private static final class TaxAccumulator {
        private final Long taxRuleId;
        private final BigDecimal percentage;
        private BigDecimal taxableBase;
        private BigDecimal taxAmount;

        private TaxAccumulator(Long taxRuleId, BigDecimal taxableBase, BigDecimal taxAmount, BigDecimal percentage) {
            this.taxRuleId = taxRuleId;
            this.taxableBase = taxableBase;
            this.taxAmount = taxAmount;
            this.percentage = percentage;
        }
    }
}

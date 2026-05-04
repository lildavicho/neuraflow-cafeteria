package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.AccountingManagementService;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.AccountView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.ApplyPaymentCommand;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.CreateManualReceivableCommand;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.DocumentTypeView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.JournalEntryView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.PayableView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.ProductTaxRuleView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.ReceivableView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.SequenceView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.TaxRuleView;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.UpsertAccountCommand;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.UpsertDocumentTypeCommand;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.UpsertSequenceCommand;
import com.ucacue.bar.erp.accounting.application.AccountingManagementService.UpsertTaxRuleCommand;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/erp/accounting")
@RequiredArgsConstructor
@Tag(name = "ERP Accounting", description = "Catalogo, impuestos, asientos, CxC y CxP del ERP")
public class AccountingManagementController {

    private final AccountingManagementService accountingManagementService;
    private final TenantFeatureService tenantFeatureService;
    private final TabularExportService tabularExportService;

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar catalogo de cuentas")
    public ResponseEntity<List<AccountView>> listAccounts(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listAccounts(tenantCode));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear cuenta contable")
    public ResponseEntity<AccountView> createAccount(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertAccountCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingManagementService.upsertAccount(tenantCode, command));
    }

    @PutMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar cuenta contable")
    public ResponseEntity<AccountView> updateAccount(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertAccountCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.upsertAccount(tenantCode, command));
    }

    @GetMapping("/tax-rules")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar reglas tributarias")
    public ResponseEntity<List<TaxRuleView>> listTaxRules(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listTaxRules(tenantCode));
    }

    @PostMapping("/tax-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear regla tributaria")
    public ResponseEntity<TaxRuleView> createTaxRule(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertTaxRuleCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingManagementService.upsertTaxRule(tenantCode, command));
    }

    @PutMapping("/tax-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar regla tributaria")
    public ResponseEntity<TaxRuleView> updateTaxRule(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertTaxRuleCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.upsertTaxRule(tenantCode, command));
    }

    @PostMapping("/tax-rules/product/{productId}/{taxRuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignar impuesto a producto")
    public ResponseEntity<ProductTaxRuleView> assignProductTaxRule(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long productId,
            @PathVariable Long taxRuleId) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.assignProductTaxRule(tenantCode, productId, taxRuleId));
    }

    @GetMapping("/document-types")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<List<DocumentTypeView>> listDocumentTypes(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listDocumentTypes(tenantCode));
    }

    @PostMapping("/document-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentTypeView> createDocumentType(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertDocumentTypeCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingManagementService.upsertDocumentType(tenantCode, command));
    }

    @PutMapping("/document-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentTypeView> updateDocumentType(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertDocumentTypeCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.upsertDocumentType(tenantCode, command));
    }

    @GetMapping("/sequences")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<List<SequenceView>> listSequences(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(accountingManagementService.listSequences(tenantCode));
    }

    @PostMapping("/sequences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SequenceView> createSequence(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertSequenceCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingManagementService.upsertSequence(tenantCode, command));
    }

    @PutMapping("/sequences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SequenceView> updateSequence(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody UpsertSequenceCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(accountingManagementService.upsertSequence(tenantCode, command));
    }

    @GetMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<List<JournalEntryView>> listJournalEntries(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listJournalEntries(tenantCode));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Exportar panorama contable")
    public ResponseEntity<byte[]> exportAccounting(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(defaultValue = "xlsx") String format) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        List<List<?>> rows = new ArrayList<>();

        accountingManagementService.listAccounts(tenantCode).forEach(account -> rows.add(List.of(
                "CATALOGO",
                account.accountCode(),
                account.active() ? "ACTIVA" : "INACTIVA",
                "",
                "",
                "",
                "",
                "",
                account.accountName() + " - " + account.accountType())));

        accountingManagementService.listTaxRules(tenantCode).forEach(rule -> rows.add(List.of(
                "IMPUESTO",
                rule.taxCode(),
                rule.active() ? "ACTIVO" : "INACTIVO",
                rule.validFrom(),
                rule.validUntil(),
                rule.rate(),
                rule.includedInPrice(),
                "",
                rule.taxName())));

        accountingManagementService.listReceivables(tenantCode).forEach(receivable -> rows.add(List.of(
                "CXC",
                receivable.sourceDocumentType() + " #" + receivable.sourceDocumentId(),
                receivable.status(),
                receivable.issueDate(),
                receivable.dueDate(),
                receivable.totalAmount(),
                receivable.paidAmount(),
                receivable.balance(),
                "Asiento #" + receivable.accountingEntryId())));

        accountingManagementService.listPayables(tenantCode).forEach(payable -> rows.add(List.of(
                "CXP",
                payable.sourceDocumentType() + " #" + payable.sourceDocumentId(),
                payable.status(),
                payable.issueDate(),
                payable.dueDate(),
                payable.totalAmount(),
                payable.paidAmount(),
                payable.balance(),
                "Asiento #" + payable.accountingEntryId())));

        accountingManagementService.listJournalEntries(tenantCode).forEach(entry -> rows.add(List.of(
                "ASIENTO",
                entry.entryNumber(),
                entry.status(),
                entry.entryDate(),
                "",
                entry.lines().stream().map(line -> line.debit() != null ? line.debit() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                entry.lines().stream().map(line -> line.credit() != null ? line.credit() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add),
                "",
                entry.description())));

        TabularExportService.ExportDocument document = tabularExportService.export(
                "Panorama contable",
                List.of("Seccion", "Referencia", "Estado", "Fecha", "Vence", "Debe/Total", "Haber/Pagado", "Saldo", "Detalle"),
                rows,
                format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=erp-accounting." + document.extension())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<List<ReceivableView>> listReceivables(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listReceivables(tenantCode));
    }

    @PostMapping("/receivables/manual")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<ReceivableView> createManualReceivable(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody CreateManualReceivableCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingManagementService.createManualReceivable(tenantCode, command));
    }

    @PostMapping("/receivables/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<ReceivableView> applyReceivablePayment(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @Valid @RequestBody ApplyPaymentCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.applyReceivablePayment(tenantCode, id, command));
    }

    @GetMapping("/payables")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<List<PayableView>> listPayables(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.listPayables(tenantCode));
    }

    @PostMapping("/payables/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<PayableView> applyPayablePayment(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @Valid @RequestBody ApplyPaymentCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.ACCOUNTING);
        return ResponseEntity.ok(accountingManagementService.applyPayablePayment(tenantCode, id, command));
    }
}


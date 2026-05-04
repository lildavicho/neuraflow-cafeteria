package com.ucacue.bar.erp.purchases.interfaces.http;

import com.ucacue.bar.erp.purchases.application.PurchaseService;
import com.ucacue.bar.erp.purchases.application.PurchaseService.CreatePurchaseCommand;
import com.ucacue.bar.erp.purchases.application.PurchaseService.PurchaseSummary;
import com.ucacue.bar.erp.purchases.application.PurchaseService.RegisterPurchasePaymentCommand;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/erp/purchases")
@RequiredArgsConstructor
@Tag(name = "ERP Purchases", description = "Compras inventariables del ERP")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final TenantFeatureService tenantFeatureService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar compras")
    public ResponseEntity<List<PurchaseSummary>> list(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.PURCHASES);
        return ResponseEntity.ok(purchaseService.list(tenantCode));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Exportar compras")
    public ResponseEntity<byte[]> export(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.PURCHASES);
        TabularExportService.ExportDocument document = purchaseService.export(tenantCode, format, status, search, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=erp-purchases." + document.extension())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Crear compra")
    public ResponseEntity<PurchaseSummary> create(@RequestHeader("X-Tenant-Code") String tenantCode,
            @Valid @RequestBody CreatePurchaseCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.PURCHASES);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(tenantCode, command));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Recibir compra y actualizar inventario")
    public ResponseEntity<PurchaseSummary> receive(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.PURCHASES);
        return ResponseEntity.ok(purchaseService.receive(tenantCode, id));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Pagar compra")
    public ResponseEntity<PurchaseSummary> pay(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @Valid @RequestBody RegisterPurchasePaymentCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.PURCHASES);
        return ResponseEntity.ok(purchaseService.pay(tenantCode, id, command));
    }
}

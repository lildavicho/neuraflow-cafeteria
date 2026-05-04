package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.SriDocumentService;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.SriConfigurationView;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentDiagnosticsView;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentStatusView;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentSummary;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentTransmissionDetailView;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentTransmissionSummary;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.UpsertSriConfigurationCommand;
import com.ucacue.bar.erp.accounting.application.SriHealthService;
import com.ucacue.bar.erp.accounting.application.SriHealthService.HealthSnapshot;
import com.ucacue.bar.erp.accounting.application.SriHealthService.ReapResult;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/erp/sri")
@RequiredArgsConstructor
@Tag(name = "ERP SRI", description = "Preparacion de facturacion electronica Ecuador dentro del ERP")
@RequireTenantModule(ModuleCode.SRI)
public class SriController {

    private final SriDocumentService sriDocumentService;
    private final TenantFeatureService tenantFeatureService;
    private final TabularExportService tabularExportService;
    private final SriHealthService sriHealthService;

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Listar documentos tributarios")
    public ResponseEntity<List<TaxDocumentSummary>> listDocuments(@RequestHeader("X-Tenant-Code") String tenantCode) {
        return ResponseEntity.ok(sriDocumentService.list(tenantCode));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Exportar comprobantes SRI")
    public ResponseEntity<byte[]> exportDocuments(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String status) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        List<List<?>> rows = new ArrayList<>();
        sriDocumentService.list(tenantCode).stream()
                .filter(item -> status == null || status.isBlank() || item.status().equalsIgnoreCase(status))
                .forEach(item -> rows.add(List.of(
                        item.documentCode(),
                        item.status(),
                        item.issueDate(),
                        item.sequentialNumber(),
                        item.buyerName(),
                        item.subtotal(),
                        item.tax(),
                        item.total(),
                        item.receptionStatus(),
                        item.authorizationStatus())));

        TabularExportService.ExportDocument document = tabularExportService.export(
                "Comprobantes SRI",
                List.of("Tipo", "Estado", "Fecha", "Secuencial", "Cliente", "Subtotal", "IVA", "Total", "Recepcion", "Autorizacion"),
                rows,
                format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=erp-sri." + document.extension())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener configuracion del emisor SRI")
    public ResponseEntity<SriConfigurationView> getConfiguration(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.getConfiguration(tenantCode));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar configuracion del emisor SRI")
    public ResponseEntity<SriConfigurationView> updateConfiguration(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestBody UpsertSriConfigurationCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.upsertConfiguration(tenantCode, command));
    }

    @PostMapping("/documents/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Validar documento tributario antes de SRI")
    public ResponseEntity<TaxDocumentSummary> validateDocument(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        return ResponseEntity.ok(sriDocumentService.validate(tenantCode, id));
    }

    @PostMapping("/documents/{id}/emit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Emitir documento tributario al SRI por flujo oficial")
    public ResponseEntity<TaxDocumentSummary> emitDocument(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.emit(tenantCode, id));
    }

    @PostMapping("/documents/{id}/poll-authorization")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar autorizacion oficial del documento tributario")
    public ResponseEntity<TaxDocumentSummary> pollAuthorization(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.pollAuthorization(tenantCode, id));
    }

    @GetMapping("/documents/{id}/transmissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar trazabilidad de transmisiones al SRI")
    public ResponseEntity<List<TaxDocumentTransmissionSummary>> listTransmissions(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.listTransmissions(tenantCode, id));
    }

    @GetMapping("/documents/{id}/transmissions/{transmissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener payload request/response de una transmision SRI")
    public ResponseEntity<TaxDocumentTransmissionDetailView> getTransmission(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @PathVariable Long transmissionId) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.getTransmission(tenantCode, id, transmissionId));
    }

    @GetMapping("/documents/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar estado operativo del documento tributario")
    public ResponseEntity<TaxDocumentStatusView> getStatus(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.getStatus(tenantCode, id));
    }

    @GetMapping("/documents/{id}/diagnostics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener diagnostico rapido para soporte operativo SRI")
    public ResponseEntity<TaxDocumentDiagnosticsView> getDiagnostics(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.getDiagnostics(tenantCode, id));
    }

    @PostMapping("/documents/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reenviar o reconsultar un documento SRI con estrategia segura")
    public ResponseEntity<TaxDocumentSummary> retryDocument(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @RequestParam(defaultValue = "AUTO") String mode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.retry(tenantCode, id, mode));
    }

    @PostMapping("/documents/{id}/authorize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marcar documento como autorizado")
    public ResponseEntity<TaxDocumentSummary> authorizeDocument(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id,
            @RequestParam String authorizationCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriDocumentService.markAuthorized(tenantCode, id, authorizationCode));
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Snapshot operativo SRI: documentos por estado, atascos, pipeline de correos")
    public ResponseEntity<HealthSnapshot> getHealth(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriHealthService.snapshotForTenantCode(tenantCode));
    }

    @PostMapping("/health/reap")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-encolar documentos AUTORIZADOS sin RIDE y registrar atascos")
    public ResponseEntity<ReapResult> reapHealth(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriHealthService.reapForCurrentTenant());
    }
}

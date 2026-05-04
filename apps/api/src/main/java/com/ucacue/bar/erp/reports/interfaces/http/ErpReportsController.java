package com.ucacue.bar.erp.reports.interfaces.http;

import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.reports.application.ErpReportsService;
import com.ucacue.bar.erp.shared.application.TabularExportService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/erp/reports")
@RequiredArgsConstructor
@Tag(name = "ERP Reports", description = "Reportes operativos del ERP")
public class ErpReportsController {

    private final ErpReportsService erpReportsService;
    private final TenantFeatureService tenantFeatureService;

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Obtener reporte de ventas ERP")
    public ResponseEntity<ErpReportsService.SalesReportView> salesReport(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.BASIC_REPORTS);
        return ResponseEntity.ok(erpReportsService.getSalesReport(tenantCode, from, to));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Exportar reporte de ventas ERP")
    public ResponseEntity<byte[]> exportSalesReport(
            @RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.BASIC_REPORTS);
        TabularExportService.ExportDocument document = erpReportsService.exportSalesReport(tenantCode, format, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=erp-sales-report." + document.extension())
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }
}

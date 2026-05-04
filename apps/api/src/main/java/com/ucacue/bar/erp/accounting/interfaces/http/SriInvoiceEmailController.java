package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.InvoiceEmailCommandService;
import com.ucacue.bar.erp.accounting.application.InvoiceEmailCommandService.InvoiceEmailHistoryResponse;
import com.ucacue.bar.erp.accounting.application.InvoiceEmailCommandService.InvoiceEmailLogItem;
import com.ucacue.bar.erp.accounting.application.InvoiceEmailCommandService.ResendInvoiceEmailRequest;
import com.ucacue.bar.erp.accounting.application.InvoiceEmailLogService.EmailStatistics;
import com.ucacue.bar.erp.accounting.application.SriAutomationService.AutomationResult;
import com.ucacue.bar.erp.accounting.application.TenantEmailSettingsService;
import com.ucacue.bar.erp.accounting.application.TenantEmailSettingsService.EmailSettingsResponse;
import com.ucacue.bar.erp.accounting.application.TenantEmailSettingsService.EmailTestResponse;
import com.ucacue.bar.erp.accounting.application.TenantEmailSettingsService.TestEmailRequest;
import com.ucacue.bar.erp.accounting.application.TenantEmailSettingsService.UpdateEmailSettingsRequest;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/erp/sri")
@RequiredArgsConstructor
@Tag(name = "ERP SRI Email", description = "Envio, reenvio, historial y metricas de correos de facturacion")
@RequireTenantModule(ModuleCode.SRI)
public class SriInvoiceEmailController {

    private final TenantContextResolver tenantContextResolver;
    private final TenantFeatureService tenantFeatureService;
    private final InvoiceEmailCommandService invoiceEmailCommandService;
    private final TenantEmailSettingsService tenantEmailSettingsService;

    @PostMapping("/documents/{id}/email/send")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Enviar factura por correo si esta pendiente o fallo previamente")
    public ResponseEntity<AutomationResult> send(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable Long id) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(invoiceEmailCommandService.send(tenant.getId(), id));
    }

    @PostMapping("/documents/{id}/email/resend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reenviar explicitamente factura por correo")
    public ResponseEntity<AutomationResult> resend(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable Long id,
            @RequestBody(required = false) ResendInvoiceEmailRequest request) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(invoiceEmailCommandService.resend(
                tenant.getId(),
                id,
                request == null ? new ResendInvoiceEmailRequest(null, null) : request));
    }

    @GetMapping("/documents/{id}/email/history")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Consultar historial de correos de una factura")
    public ResponseEntity<InvoiceEmailHistoryResponse> history(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable Long id) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(invoiceEmailCommandService.history(tenant.getId(), id));
    }

    @GetMapping("/email/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar logs de correos por tenant")
    public ResponseEntity<List<InvoiceEmailLogItem>> logs(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(invoiceEmailCommandService.logs(tenant.getId(), from, to, status, limit));
    }

    @GetMapping("/email/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar metricas de correos por tenant")
    public ResponseEntity<EmailStatistics> statistics(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(invoiceEmailCommandService.statistics(tenant.getId(), from, to));
    }

    @GetMapping("/email/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener configuracion de correo del tenant sin secretos")
    public ResponseEntity<EmailSettingsResponse> getSettings(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(tenantEmailSettingsService.get(tenant.getId()));
    }

    @PutMapping("/email/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar configuracion de correo del tenant")
    public ResponseEntity<EmailSettingsResponse> updateSettings(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody UpdateEmailSettingsRequest request) {
        TenantEntity tenant = resolveTenant(tenantCode);
        return ResponseEntity.ok(tenantEmailSettingsService.update(tenant.getId(), request));
    }

    @PostMapping("/email/settings/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar correo de prueba de configuracion")
    public ResponseEntity<EmailTestResponse> testSettings(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody(required = false) TestEmailRequest request,
            Authentication authentication) {
        TenantEntity tenant = resolveTenant(tenantCode);
        String authenticatedEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(tenantEmailSettingsService.test(
                tenant.getId(),
                request == null ? new TestEmailRequest(null) : request,
                authenticatedEmail));
    }

    private TenantEntity resolveTenant(String tenantCode) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        tenantFeatureService.assertModuleEnabled(tenant.getTenantCode(), ModuleCode.SRI);
        return tenant;
    }
}

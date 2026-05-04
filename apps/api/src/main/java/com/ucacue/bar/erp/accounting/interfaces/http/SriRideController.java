package com.ucacue.bar.erp.accounting.interfaces.http;

import com.ucacue.bar.erp.accounting.application.RideStorageService;
import com.ucacue.bar.erp.accounting.application.RideStorageService.StoredRideFile;
import com.ucacue.bar.erp.accounting.application.SriAutomationService;
import com.ucacue.bar.erp.accounting.application.SriAutomationService.AutomationResult;
import com.ucacue.bar.erp.accounting.application.SriRideService;
import com.ucacue.bar.erp.accounting.application.SriRideService.RidePdfResult;
import com.ucacue.bar.erp.accounting.application.SriRideService.RideRenderResult;
import com.ucacue.bar.erp.accounting.application.SriRideService.RideTemplateView;
import com.ucacue.bar.erp.accounting.application.SriRideService.UpsertRideTemplateCommand;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/erp/sri/ride")
@RequiredArgsConstructor
@Tag(name = "ERP SRI RIDE", description = "Motor dinamico HTML/PDF para representacion impresa de facturas SRI")
@RequireTenantModule(ModuleCode.SRI)
public class SriRideController {

    private final SriRideService sriRideService;
    private final RideStorageService rideStorageService;
    private final SriAutomationService sriAutomationService;
    private final TenantContextResolver tenantContextResolver;
    private final TenantFeatureService tenantFeatureService;
    private final TaxDocumentRepository taxDocumentRepository;

    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vista previa HTML del RIDE usando el ultimo documento o datos de ejemplo")
    public ResponseEntity<String> previewLatest(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        RideRenderResult result = sriRideService.previewLatest(tenantCode);
        return htmlResponse(result.html());
    }

    @GetMapping(value = "/documents/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Vista previa HTML del RIDE para un documento tributario")
    public ResponseEntity<String> previewDocument(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        RideRenderResult result = sriRideService.previewDocument(tenantCode, id);
        return htmlResponse(result.html());
    }

    @GetMapping(value = "/documents/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Generar PDF RIDE para un documento tributario")
    public ResponseEntity<byte[]> documentPdf(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        RidePdfResult result = sriRideService.generatePdf(tenantCode, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(result.fileName()).build().toString())
                .body(result.content());
    }

    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener plantilla RIDE activa del tenant")
    public ResponseEntity<RideTemplateView> getTemplate(@RequestHeader("X-Tenant-Code") String tenantCode) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriRideService.getCurrentTemplate(tenantCode));
    }

    @PutMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar plantilla RIDE editable del tenant")
    public ResponseEntity<RideTemplateView> saveTemplate(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestBody UpsertRideTemplateCommand command) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        return ResponseEntity.ok(sriRideService.saveTemplate(tenantCode, command));
    }

    @PostMapping(value = "/assets/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir logo del tenant para plantillas RIDE")
    public ResponseEntity<StoredRideFile> uploadLogo(@RequestHeader("X-Tenant-Code") String tenantCode,
            @RequestPart("file") MultipartFile file) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return ResponseEntity.ok(rideStorageService.storeLogo(tenantId, file));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Obtener URLs firmadas (PDF y XML) para descargar el RIDE almacenado")
    public ResponseEntity<DownloadLinksResponse> downloadLinks(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        TaxDocumentEntity document = taxDocumentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado"));
        String pdfUrl = document.getRidePdfPath() != null
                ? rideStorageService.signedUrl(document.getRidePdfPath())
                : null;
        String xmlUrl = document.getRideXmlPath() != null
                ? rideStorageService.signedUrl(document.getRideXmlPath())
                : null;
        return ResponseEntity.ok(new DownloadLinksResponse(
                document.getId(),
                document.getAccessKey(),
                pdfUrl,
                xmlUrl,
                rideStorageService.signedUrlExpiresAt()));
    }

    public record DownloadLinksResponse(
            Long documentId,
            String accessKey,
            String pdfUrl,
            String xmlUrl,
            java.time.Instant expiresAt) {
    }

    @PostMapping("/documents/{id}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerar RIDE y reintentar notificacion sin duplicar archivo")
    public ResponseEntity<AutomationResult> regenerateRide(@RequestHeader("X-Tenant-Code") String tenantCode,
            @PathVariable Long id) {
        tenantFeatureService.assertModuleEnabled(tenantCode, ModuleCode.SRI);
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return ResponseEntity.ok(sriAutomationService.regenerateRide(tenantId, id));
    }

    private ResponseEntity<String> htmlResponse(String html) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS))
                .header("Content-Security-Policy",
                        "default-src 'none'; img-src data: https: http:; style-src 'unsafe-inline' https:; font-src https: data:")
                .body(html);
    }
}

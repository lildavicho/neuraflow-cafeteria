package com.ucacue.bar.controller;

import com.ucacue.bar.erp.accounting.application.SriDocumentService;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentSummary;
import com.ucacue.bar.erp.accounting.application.SriDocumentService.TaxDocumentXmlView;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturacion SRI", description = "Endpoints que consume Angular para emitir facturas electronicas")
@RequireTenantModule(ModuleCode.SRI)
public class FacturaSriController {

    private final SriDocumentService sriDocumentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Emitir factura electronica SRI desde una venta")
    public ResponseEntity<FacturaSriResponse> emitirFactura(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody EmitirFacturaRequest request) {
        TaxDocumentSummary summary = sriDocumentService.emitBySaleId(
                tenantCode,
                request != null ? request.ventaId() : null
        );
        String xmlUrl = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}/xml")
                .buildAndExpand(summary.id())
                .toUriString();
        return ResponseEntity.ok(FacturaSriResponse.from(summary, xmlUrl, null));
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Descargar XML generado o autorizado de la factura SRI")
    public ResponseEntity<byte[]> descargarXml(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable Long id) {
        TaxDocumentXmlView xml = sriDocumentService.getXml(tenantCode, id);
        String filename = (xml.accessKey() != null && !xml.accessKey().isBlank() ? xml.accessKey() : "factura-" + id) + ".xml";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(xml.xml().getBytes(StandardCharsets.UTF_8));
    }

    public record EmitirFacturaRequest(Long ventaId) {
    }

    public record FacturaSriResponse(
            Long documentoId,
            String estado,
            String estadoInterno,
            String claveAcceso,
            String numeroAutorizacion,
            String mensajeSri,
            String xmlUrl,
            String pdfUrl) {

        private static FacturaSriResponse from(TaxDocumentSummary summary, String xmlUrl, String pdfUrl) {
            return new FacturaSriResponse(
                    summary.id(),
                    mapStatus(summary.status()),
                    summary.status(),
                    summary.accessKey(),
                    summary.authorizationCode(),
                    summary.providerMessage(),
                    xmlUrl,
                    pdfUrl
            );
        }

        private static String mapStatus(String status) {
            if ("AUTHORIZED".equalsIgnoreCase(status)) {
                return "Autorizado";
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                return "Rechazado";
            }
            if ("SENT".equalsIgnoreCase(status)) {
                return "Recibida";
            }
            if ("READY_TO_SEND".equalsIgnoreCase(status)) {
                return "Listo para envio";
            }
            return status;
        }
    }
}

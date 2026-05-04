package com.ucacue.bar.controller;

import com.ucacue.bar.erp.accounting.application.SriDocumentService;
import com.ucacue.bar.erp.commercial.interfaces.http.RequireTenantModule;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/erp/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Consulta de documentos para el módulo de clientes")
@RequireTenantModule(ModuleCode.CUSTOMERS)
public class CustomerDocumentsController {

    private final SriDocumentService sriDocumentService;

    @GetMapping("/documents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista documentos visibles para clientes")
    public ResponseEntity<List<SriDocumentService.TaxDocumentSummary>> listDocuments(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode) {
        return ResponseEntity.ok(sriDocumentService.list(tenantCode));
    }
}

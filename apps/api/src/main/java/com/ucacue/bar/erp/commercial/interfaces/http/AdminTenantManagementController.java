package com.ucacue.bar.erp.commercial.interfaces.http;

import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.CommercialPlanCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/erp/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "ERP Tenant Admin", description = "Administracion de negocios y modulos")
public class AdminTenantManagementController {

    private final TenantFeatureService tenantFeatureService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar negocios, planes y modulos disponibles")
    public ResponseEntity<TenantFeatureService.AdminTenantCatalogView> listTenants() {
        return ResponseEntity.ok(tenantFeatureService.listTenantsForAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear negocio con plan inicial START")
    public ResponseEntity<TenantFeatureService.AdminTenantView> createTenant(
            @RequestBody @Valid CreateTenantRequest request) {
        var created = tenantFeatureService.createTenantForAdmin(new TenantFeatureService.CreateTenantCommand(
                request.tenantCode(),
                request.legalName(),
                request.tradeName(),
                request.countryCode(),
                request.timeZone(),
                request.currencyCode()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{tenantCode}/plan")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar plan comercial base de un negocio")
    public ResponseEntity<TenantCommercialPlanResponse> updatePlan(
            @PathVariable String tenantCode,
            @RequestBody @Valid UpdateTenantCommercialPlanRequest request) {
        var resolved = tenantFeatureService.updatePlan(tenantCode, CommercialPlanCode.fromRequired(request.commercialPlan()));
        return ResponseEntity.ok(toResponse(resolved));
    }

    @PutMapping("/{tenantCode}/modules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Guardar modulos habilitados para un negocio")
    public ResponseEntity<TenantCommercialPlanResponse> updateModules(
            @PathVariable String tenantCode,
            @RequestBody @Valid UpdateTenantModulesRequest request) {
        var resolved = tenantFeatureService.updateModuleOverrides(tenantCode, request.enabledModules());
        return ResponseEntity.ok(toResponse(resolved));
    }

    private TenantCommercialPlanResponse toResponse(TenantFeatureService.TenantPlanView resolved) {
        return new TenantCommercialPlanResponse(
                resolved.tenantCode(),
                resolved.commercialPlan(),
                resolved.enabledModules(),
                resolved.availablePlans());
    }

    public record CreateTenantRequest(
            @jakarta.validation.constraints.NotBlank(message = "El codigo del negocio es obligatorio")
            @jakarta.validation.constraints.Size(min = 1, max = 64, message = "El codigo del negocio debe tener entre 1 y 64 caracteres")
            String tenantCode,
            @jakarta.validation.constraints.NotBlank(message = "La razon social es obligatoria")
            String legalName,
            String tradeName,
            String countryCode,
            String timeZone,
            String currencyCode) {
    }

    public record UpdateTenantModulesRequest(
            @jakarta.validation.constraints.NotNull(message = "La lista de modulos es obligatoria")
            List<String> enabledModules) {
    }
}

package com.ucacue.bar.erp.commercial.interfaces.http;

import com.ucacue.bar.erp.commercial.application.TenantFeatureService;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.CommercialPlanCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erp/tenants/{tenantCode}/commercial-plan")
@RequiredArgsConstructor
@Tag(name = "ERP Commercial Plans", description = "Planes comerciales por tenant")
public class TenantCommercialPlanController {

    private final TenantFeatureService tenantFeatureService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener plan comercial efectivo por tenant")
    public ResponseEntity<TenantCommercialPlanResponse> getPlan(@PathVariable String tenantCode) {
        var resolved = tenantFeatureService.resolvePlan(tenantCode);
        return ResponseEntity.ok(new TenantCommercialPlanResponse(
                resolved.tenantCode(),
                resolved.commercialPlan(),
                resolved.enabledModules(),
                resolved.availablePlans()
        ));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar plan comercial por tenant")
    public ResponseEntity<TenantCommercialPlanResponse> updatePlan(@PathVariable String tenantCode,
                                                                   @RequestBody @Valid UpdateTenantCommercialPlanRequest request) {
        var resolved = tenantFeatureService.updatePlan(tenantCode, CommercialPlanCode.fromRequired(request.commercialPlan()));
        return ResponseEntity.ok(new TenantCommercialPlanResponse(
                resolved.tenantCode(),
                resolved.commercialPlan(),
                resolved.enabledModules(),
                resolved.availablePlans()
        ));
    }
}

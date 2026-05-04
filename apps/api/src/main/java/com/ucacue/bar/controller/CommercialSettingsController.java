package com.ucacue.bar.controller;

import com.ucacue.bar.dto.settings.CommercialPlanResponse;
import com.ucacue.bar.dto.settings.UpdateCommercialPlanRequest;
import com.ucacue.bar.service.CommercialPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/commercial-plan")
@RequiredArgsConstructor
@Tag(name = "Commercial Settings", description = "Configuración comercial por plan")
public class CommercialSettingsController {

    private final CommercialPlanService commercialPlanService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener plan comercial activo")
    public ResponseEntity<CommercialPlanResponse> getPlan() {
        return ResponseEntity.ok(commercialPlanService.getResponse());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar plan comercial activo")
    public ResponseEntity<CommercialPlanResponse> updatePlan(@RequestBody @Valid UpdateCommercialPlanRequest request) {
        var snapshot = commercialPlanService.updatePlan(request.commercialPlan());
        return ResponseEntity.ok(new CommercialPlanResponse(
                snapshot.commercialPlan(),
                snapshot.enabledModules(),
                snapshot.availablePlans()
        ));
    }
}

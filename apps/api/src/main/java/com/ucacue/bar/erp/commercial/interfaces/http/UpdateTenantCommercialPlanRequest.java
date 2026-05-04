package com.ucacue.bar.erp.commercial.interfaces.http;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantCommercialPlanRequest(
        @NotBlank(message = "El plan comercial es obligatorio")
        String commercialPlan) {
}

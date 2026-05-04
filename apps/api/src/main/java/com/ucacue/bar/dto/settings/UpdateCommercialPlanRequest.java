package com.ucacue.bar.dto.settings;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommercialPlanRequest(
        @NotBlank
        String commercialPlan) {
}

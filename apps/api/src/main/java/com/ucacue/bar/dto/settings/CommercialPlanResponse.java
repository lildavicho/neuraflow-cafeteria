package com.ucacue.bar.dto.settings;

import java.util.List;

public record CommercialPlanResponse(
        String commercialPlan,
        List<String> enabledModules,
        List<String> availablePlans) {
}

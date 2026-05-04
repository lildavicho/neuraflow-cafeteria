package com.ucacue.bar.erp.commercial.interfaces.http;

import java.util.List;

public record TenantCommercialPlanResponse(
        String tenantCode,
        String commercialPlan,
        List<String> enabledModules,
        List<String> availablePlans) {
}

package com.ucacue.bar.erp.shared.domain;

import java.util.Locale;

import com.ucacue.bar.exception.BadRequestException;

public final class CommercialTypes {

    private CommercialTypes() {
    }

    public enum CommercialPlanCode {
        START,
        PRO,
        VISION_AI;

        public static CommercialPlanCode from(String value) {
            if (value == null || value.isBlank()) {
                throw new BadRequestException("El plan comercial es obligatorio");
            }
            String normalized = value.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
            return CommercialPlanCode.valueOf(normalized);
        }

        public static CommercialPlanCode fromRequired(String value) {
            if (value == null || value.isBlank()) {
                throw new BadRequestException("El plan comercial es obligatorio");
            }
            String normalized = value.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);
            try {
                return CommercialPlanCode.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Plan comercial invalido: " + value);
            }
        }
    }

    public enum ModuleCode {
        POS("pos"),
        INVENTORY("inventory"),
        CUSTOMERS("customers"),
        SALES("sales"),
        ORDERS("orders"),
        CASH("cash"),
        BASIC_REPORTS("basic-reports"),
        PURCHASES("purchases"),
        EXECUTIVE_DASHBOARD("executive-dashboard"),
        INSIGHTS("insights"),
        ACCOUNTING("accounting"),
        TAXATION("taxation"),
        SRI("sri"),
        VISION_AI("vision-ai"),
        FOOTFALL("footfall"),
        CONVERSION("conversion"),
        PEAK_HOURS("peak-hours"),
        TRAFFIC_INSIGHTS("traffic-insights"),
        PREDICTIONS("predictions"),
        BRANCHES("branches"),
        WAREHOUSES("warehouses"),
        PARTIES("parties"),
        APPROVALS("approvals"),
        CRM("crm"),
        QUOTATIONS("quotations"),
        SALES_ORDERS("sales-orders"),
        PRICING("pricing"),
        RETURNS("returns"),
        BUDGET("budget"),
        BANK("bank"),
        FIXED_ASSETS("fixed-assets"),
        WITHHOLDINGS("withholdings"),
        RESTAURANT("restaurant"),
        EDUCATION("education"),
        SERVICE_DESK("service-desk"),
        HR("hr");

        private final String moduleKey;

        ModuleCode(String moduleKey) {
            this.moduleKey = moduleKey;
        }

        public String moduleKey() {
            return moduleKey;
        }

        public static ModuleCode fromModuleKey(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("moduleKey is required");
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (ModuleCode moduleCode : values()) {
                if (moduleCode.moduleKey.equals(normalized)
                        || moduleCode.name().equalsIgnoreCase(value.trim().replace('-', '_'))) {
                    return moduleCode;
                }
            }
            throw new IllegalArgumentException("Unknown moduleKey: " + value);
        }
    }
}

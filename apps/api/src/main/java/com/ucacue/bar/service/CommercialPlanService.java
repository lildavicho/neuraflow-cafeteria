package com.ucacue.bar.service;

import com.ucacue.bar.dto.settings.CommercialPlanResponse;
import com.ucacue.bar.entity.SettingEntity;
import com.ucacue.bar.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercialPlanService {

    public static final String SETTING_KEY_COMMERCIAL_PLAN = "commercial_plan";

    private static final String PLAN_START = "START";
    private static final String PLAN_PRO = "PRO";
    private static final String PLAN_VISION_AI = "VISION_AI";

    private static final List<String> START_MODULES = List.of(
            "pos",
            "profile",
            "notifications",
            "settings"
    );

    private static final List<String> PRO_MODULES = List.of(
            "pos",
            "profile",
            "notifications",
            "settings",
            "dashboard",
            "inventory",
            "analytics",
            "reports"
    );

    private static final List<String> VISION_AI_MODULES = List.of(
            "pos",
            "profile",
            "notifications",
            "settings",
            "dashboard",
            "inventory",
            "analytics",
            "reports",
            "vision-ai"
    );

    private static final Map<String, List<String>> MODULES_BY_PLAN = Map.of(
            PLAN_START, START_MODULES,
            PLAN_PRO, PRO_MODULES,
            PLAN_VISION_AI, VISION_AI_MODULES
    );

    private final SettingRepository settingRepository;
    private final RealtimeGateway realtimeGateway;

    @Value("${app.commercial-plan:START}")
    private String defaultCommercialPlan;

    @Transactional(readOnly = true)
    public CommercialPlanSnapshot getSnapshot() {
        String resolvedPlan = settingRepository.findByKey(SETTING_KEY_COMMERCIAL_PLAN)
                .map(SettingEntity::getValue)
                .map(this::normalizePlan)
                .orElseGet(() -> normalizePlan(defaultCommercialPlan));
        return buildSnapshot(resolvedPlan);
    }

    @Transactional
    public CommercialPlanSnapshot updatePlan(String requestedPlan) {
        String resolvedPlan = normalizeRequestedPlan(requestedPlan);

        SettingEntity entity = settingRepository.findByKey(SETTING_KEY_COMMERCIAL_PLAN)
                .orElseGet(SettingEntity::new);
        entity.setKey(SETTING_KEY_COMMERCIAL_PLAN);
        entity.setValue(resolvedPlan);
        settingRepository.save(entity);

        CommercialPlanSnapshot snapshot = buildSnapshot(resolvedPlan);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "commercial.plan.updated");
        payload.put("commercialPlan", snapshot.commercialPlan());
        payload.put("enabledModules", snapshot.enabledModules());
        try {
            realtimeGateway.settings(payload);
        } catch (Exception ex) {
            log.warn("Unable to publish commercial plan update", ex);
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public CommercialPlanResponse getResponse() {
        CommercialPlanSnapshot snapshot = getSnapshot();
        return new CommercialPlanResponse(
                snapshot.commercialPlan(),
                snapshot.enabledModules(),
                snapshot.availablePlans()
        );
    }

    public String normalizePlan(String value) {
        if (value == null) {
            throw new IllegalStateException("Commercial plan setting is missing");
        }
        if (value.isBlank()) {
            throw new IllegalStateException("Commercial plan setting is blank");
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if (!MODULES_BY_PLAN.containsKey(normalized)) {
            throw new IllegalStateException("Unknown commercial plan setting: " + value);
        }
        return normalized;
    }

    private String normalizeRequestedPlan(String value) {
        if (value == null || value.isBlank()) {
            throw new com.ucacue.bar.exception.BadRequestException("El plan comercial es obligatorio");
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if (!MODULES_BY_PLAN.containsKey(normalized)) {
            throw new com.ucacue.bar.exception.BadRequestException("Plan comercial invalido: " + value);
        }
        return normalized;
    }

    private CommercialPlanSnapshot buildSnapshot(String resolvedPlan) {
        List<String> enabledModules = MODULES_BY_PLAN.getOrDefault(resolvedPlan, START_MODULES);
        return new CommercialPlanSnapshot(
                resolvedPlan,
                enabledModules,
                List.of(PLAN_START, PLAN_PRO, PLAN_VISION_AI)
        );
    }

    public record CommercialPlanSnapshot(
            String commercialPlan,
            List<String> enabledModules,
            List<String> availablePlans) {
    }
}

package com.ucacue.bar.erp.commercial.application;

import com.ucacue.bar.entity.SettingEntity;
import com.ucacue.bar.entity.UserEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantFeatureFlagEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantPlanSubscriptionEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantFeatureFlagRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantPlanSubscriptionRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.CommercialPlanCode;
import com.ucacue.bar.erp.shared.domain.CommercialTypes.ModuleCode;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import com.ucacue.bar.exception.UnauthorizedException;
import com.ucacue.bar.repository.SettingRepository;
import com.ucacue.bar.repository.UserRepository;
import com.ucacue.bar.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantFeatureService {

    private static final String GLOBAL_COMMERCIAL_PLAN_KEY = "commercial_plan";
    private static final String DEFAULT_TENANT_CODE = "default";
    private static final String TENANT_CODE_PATTERN = "^[A-Za-z0-9_-]{1,64}$";

    private static final Map<CommercialPlanCode, Set<ModuleCode>> MODULES_BY_PLAN;

    static {
        Map<CommercialPlanCode, Set<ModuleCode>> matrix = new EnumMap<>(CommercialPlanCode.class);
        matrix.put(CommercialPlanCode.START, EnumSet.of(
                ModuleCode.POS,
                ModuleCode.INVENTORY,
                ModuleCode.CUSTOMERS,
                ModuleCode.SALES,
                ModuleCode.ORDERS,
                ModuleCode.CASH,
                ModuleCode.BASIC_REPORTS,
                ModuleCode.BRANCHES,
                ModuleCode.WAREHOUSES,
                ModuleCode.PARTIES
        ));
        matrix.put(CommercialPlanCode.PRO, EnumSet.of(
                ModuleCode.POS,
                ModuleCode.INVENTORY,
                ModuleCode.CUSTOMERS,
                ModuleCode.SALES,
                ModuleCode.ORDERS,
                ModuleCode.CASH,
                ModuleCode.BASIC_REPORTS,
                ModuleCode.BRANCHES,
                ModuleCode.WAREHOUSES,
                ModuleCode.PARTIES,
                ModuleCode.PURCHASES,
                ModuleCode.EXECUTIVE_DASHBOARD,
                ModuleCode.INSIGHTS,
                ModuleCode.ACCOUNTING,
                ModuleCode.TAXATION,
                ModuleCode.SRI,
                ModuleCode.APPROVALS,
                ModuleCode.BUDGET,
                ModuleCode.BANK
        ));
        matrix.put(CommercialPlanCode.VISION_AI, EnumSet.of(
                ModuleCode.POS,
                ModuleCode.INVENTORY,
                ModuleCode.CUSTOMERS,
                ModuleCode.SALES,
                ModuleCode.ORDERS,
                ModuleCode.CASH,
                ModuleCode.BASIC_REPORTS,
                ModuleCode.BRANCHES,
                ModuleCode.WAREHOUSES,
                ModuleCode.PARTIES,
                ModuleCode.PURCHASES,
                ModuleCode.EXECUTIVE_DASHBOARD,
                ModuleCode.INSIGHTS,
                ModuleCode.ACCOUNTING,
                ModuleCode.TAXATION,
                ModuleCode.SRI,
                ModuleCode.APPROVALS,
                ModuleCode.BUDGET,
                ModuleCode.BANK,
                ModuleCode.VISION_AI,
                ModuleCode.FOOTFALL,
                ModuleCode.CONVERSION,
                ModuleCode.PEAK_HOURS,
                ModuleCode.TRAFFIC_INSIGHTS,
                ModuleCode.PREDICTIONS
        ));
        MODULES_BY_PLAN = Map.copyOf(matrix);
    }

    private final TenantRepository tenantRepository;
    private final TenantPlanSubscriptionRepository tenantPlanSubscriptionRepository;
    private final TenantFeatureFlagRepository tenantFeatureFlagRepository;
    private final SettingRepository settingRepository;
    private final TenantContextResolver tenantContextResolver;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    public TenantPlanView resolvePlan(String tenantCode) {
        return cachedPlanForTenant(tenantContextResolver.resolve(tenantCode));
    }

    @Transactional(readOnly = true)
    public TenantPlanView resolvePlanForTenant(TenantEntity tenant) {
        TenantEntity effectiveTenant = tenant != null
                ? tenant
                : tenantRepository.findByTenantCodeIgnoreCase(DEFAULT_TENANT_CODE)
                .orElseThrow(() -> new NotFoundException("No existe el tenant por defecto"));
        return cachedPlanForTenant(effectiveTenant);
    }

    private TenantPlanView cachedPlanForTenant(TenantEntity effectiveTenant) {
        Cache cache = cacheManager.getCache("tenant-plans");
        if (cache == null || effectiveTenant.getId() == null) {
            return buildPlanForTenant(effectiveTenant);
        }
        return cache.get(effectiveTenant.getId(), () -> buildPlanForTenant(effectiveTenant));
    }

    private TenantPlanView buildPlanForTenant(TenantEntity effectiveTenant) {
        CommercialPlanCode basePlan = tenantPlanSubscriptionRepository
                .findFirstByTenantIdAndStatusOrderByStartsAtDesc(
                        effectiveTenant.getId(),
                        TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE)
                .map(TenantPlanSubscriptionEntity::getCommercialPlan)
                .orElseGet(this::resolveGlobalFallbackPlan);

        EnumSet<ModuleCode> enabledModules = EnumSet.copyOf(MODULES_BY_PLAN.getOrDefault(basePlan, EnumSet.noneOf(ModuleCode.class)));
        List<TenantFeatureFlagEntity> overrides = tenantFeatureFlagRepository.findAllByTenantId(effectiveTenant.getId());
        LocalDateTime now = LocalDateTime.now();
        for (TenantFeatureFlagEntity override : overrides) {
            if (override.getExpiresAt() != null && override.getExpiresAt().isBefore(now)) {
                continue;
            }
            if (Boolean.TRUE.equals(override.getEnabled())) {
                enabledModules.add(override.getModuleCode());
            } else {
                enabledModules.remove(override.getModuleCode());
            }
        }

        List<String> availablePlans = List.of(
                CommercialPlanCode.START.name(),
                CommercialPlanCode.PRO.name(),
                CommercialPlanCode.VISION_AI.name()
        );

        List<String> moduleKeys = enabledModules.stream()
                .map(ModuleCode::moduleKey)
                .sorted()
                .toList();

        return new TenantPlanView(effectiveTenant.getTenantCode(), basePlan.name(), moduleKeys, availablePlans);
    }

    @Transactional
    @CacheEvict(cacheNames = "tenant-plans", allEntries = true, beforeInvocation = true)
    public TenantPlanView updatePlan(String tenantCode, CommercialPlanCode planCode) {
        assertPlatformAdmin();
        TenantEntity tenant = findTenant(tenantCode);
        TenantPlanSubscriptionEntity current = tenantPlanSubscriptionRepository
                .findFirstByTenantIdAndStatusOrderByStartsAtDesc(
                        tenant.getId(),
                        TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (current != null) {
            current.setStatus(TenantPlanSubscriptionEntity.SubscriptionStatus.CANCELLED);
            current.setEndsAt(LocalDateTime.now());
            tenantPlanSubscriptionRepository.save(current);
        }

        TenantPlanSubscriptionEntity replacement = new TenantPlanSubscriptionEntity();
        replacement.setTenant(tenant);
        replacement.setCommercialPlan(planCode);
        replacement.setStatus(TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE);
        replacement.setStartsAt(LocalDateTime.now());
        tenantPlanSubscriptionRepository.save(replacement);
        tenantPlanSubscriptionRepository.flush();
        return resolvePlanForTenant(tenant);
    }

    @Transactional(readOnly = true)
    public boolean tenantHasModule(String tenantCode, ModuleCode moduleCode) {
        return resolvePlan(tenantCode).enabledModules().contains(moduleCode.moduleKey());
    }

    @Transactional(readOnly = true)
    public void assertModuleEnabled(String tenantCode, ModuleCode moduleCode) {
        if (!tenantHasModule(tenantCode, moduleCode)) {
            throw new AccessDeniedException("El tenant no tiene habilitado el modulo " + moduleCode.moduleKey());
        }
    }

    @Transactional(readOnly = true)
    public AdminTenantCatalogView listTenantsForAdmin() {
        assertPlatformAdmin();
        List<AdminTenantView> tenants = tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(TenantEntity::getTenantCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::toAdminTenantView)
                .toList();
        return new AdminTenantCatalogView(tenants, moduleCatalog(), availablePlanNames());
    }

    @Transactional
    @CacheEvict(cacheNames = "tenant-plans", allEntries = true, beforeInvocation = true)
    public AdminTenantView createTenantForAdmin(CreateTenantCommand command) {
        assertPlatformAdmin();
        String tenantCode = normalizeTenantCode(command.tenantCode());
        tenantRepository.findByTenantCodeIgnoreCase(tenantCode).ifPresent(existing -> {
            throw new BadRequestException("Ya existe un negocio con ese codigo");
        });

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(tenantCode);
        tenant.setLegalName(blankToDefault(command.legalName(), tenantCode));
        tenant.setTradeName(blankToNull(command.tradeName()));
        tenant.setCountryCode(blankToDefault(command.countryCode(), "EC"));
        tenant.setTimeZone(blankToDefault(command.timeZone(), "America/Guayaquil"));
        tenant.setCurrencyCode(blankToDefault(command.currencyCode(), "USD"));
        tenant.setActive(Boolean.TRUE);
        TenantEntity saved = tenantRepository.save(tenant);
        createSubscription(saved, CommercialPlanCode.START);
        return toAdminTenantView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "tenant-plans", allEntries = true, beforeInvocation = true)
    public TenantPlanView updateModuleOverrides(String tenantCode, List<String> enabledModuleKeys) {
        assertPlatformAdmin();
        TenantEntity tenant = findTenant(tenantCode);
        TenantPlanView current = resolvePlanForTenant(tenant);
        CommercialPlanCode basePlan = CommercialPlanCode.from(current.commercialPlan());
        Set<ModuleCode> selectedModules = parseModuleCodes(enabledModuleKeys);
        Set<ModuleCode> baseModules = MODULES_BY_PLAN.getOrDefault(basePlan, Set.of());

        for (ModuleCode moduleCode : ModuleCode.values()) {
            boolean selected = selectedModules.contains(moduleCode);
            boolean enabledByBasePlan = baseModules.contains(moduleCode);
            var existing = tenantFeatureFlagRepository.findByTenantIdAndModuleCode(tenant.getId(), moduleCode);
            if (selected == enabledByBasePlan) {
                existing.ifPresent(tenantFeatureFlagRepository::delete);
                continue;
            }

            TenantFeatureFlagEntity flag = existing.orElseGet(TenantFeatureFlagEntity::new);
            flag.setTenant(tenant);
            flag.setModuleCode(moduleCode);
            flag.setEnabled(selected);
            flag.setReasonText("Configurado desde administracion");
            flag.setExpiresAt(null);
            tenantFeatureFlagRepository.save(flag);
        }

        tenantFeatureFlagRepository.flush();
        return resolvePlanForTenant(tenant);
    }

    @Transactional
    public TenantEntity resolveTenantForFirebaseLogin(String requestedTenantCode,
                                                      String email,
                                                      String displayName,
                                                      UserEntity existingUser) {
        if (existingUser != null && existingUser.getTenant() != null) {
            return existingUser.getTenant();
        }

        boolean explicitTenant = requestedTenantCode != null && !requestedTenantCode.isBlank();
        String tenantCode = explicitTenant
                ? normalizeTenantCode(requestedTenantCode)
                : nextAvailableFirebaseTenantCode(email, displayName);
        var existingTenant = tenantRepository.findByTenantCodeIgnoreCase(tenantCode);
        if (existingTenant.isPresent()) {
            TenantEntity tenant = existingTenant.get();
            if (explicitTenant && userRepository.countByTenantId(tenant.getId()) > 0) {
                throw new AccessDeniedException("Este negocio ya existe. Pide al administrador que agregue tu cuenta.");
            }
            return tenant;
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(tenantCode);
        tenant.setLegalName(blankToDefault(displayName, email != null ? email : tenantCode));
        tenant.setTradeName(blankToDefault(displayName, tenantCode));
        tenant.setActive(Boolean.TRUE);
        TenantEntity saved = tenantRepository.save(tenant);
        createSubscription(saved, CommercialPlanCode.START);
        return saved;
    }

    private String nextAvailableFirebaseTenantCode(String email, String displayName) {
        String base = firebaseTenantCodeBase(email, displayName);
        for (int index = 1; index <= 100; index++) {
            String suffix = index == 1 ? "" : "-" + index;
            int maxBaseLength = Math.max(1, 64 - suffix.length());
            String candidate = base.length() > maxBaseLength
                    ? base.substring(0, maxBaseLength).replaceAll("(^-|-$)", "")
                    : base;
            candidate = normalizeTenantCode(candidate + suffix);
            var existingTenant = tenantRepository.findByTenantCodeIgnoreCase(candidate);
            if (existingTenant.isEmpty() || userRepository.countByTenantId(existingTenant.get().getId()) == 0) {
                return candidate;
            }
        }
        throw new BadRequestException("No se pudo generar un codigo de negocio para esta cuenta");
    }

    private String firebaseTenantCodeBase(String email, String displayName) {
        String source = email != null && email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : blankToDefault(displayName, "negocio");
        String normalized = source.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.length() < 3) {
            normalized = "negocio-" + normalized;
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("(^-|-$)", "");
        }
        return normalizeTenantCode(normalized);
    }

    private TenantEntity findTenant(String tenantCode) {
        String effectiveTenantCode = normalizeTenantCode(tenantCode);
        return tenantRepository.findByTenantCodeIgnoreCase(effectiveTenantCode)
                .orElseThrow(() -> new NotFoundException("No existe el tenant solicitado"));
    }

    private void assertPlatformAdmin() {
        UserEntity user = currentUser();
        if (user == null || !Boolean.TRUE.equals(user.getPlatformAdmin())) {
            throw new AccessDeniedException("Solo el administrador de plataforma puede cambiar planes o modulos");
        }
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return userRepository.findById(customUserDetails.getId())
                    .filter(user -> Boolean.TRUE.equals(user.getActive()))
                    .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no existe"));
        }
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return null;
        }
        return userRepository.findByEmailIgnoreCaseWithTenant(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario autenticado no existe"));
    }

    private AdminTenantView toAdminTenantView(TenantEntity tenant) {
        TenantPlanView plan = resolvePlanForTenant(tenant);
        long users = userRepository.countByTenantId(tenant.getId());
        return new AdminTenantView(
                tenant.getTenantCode(),
                tenant.getLegalName(),
                tenant.getTradeName(),
                Boolean.TRUE.equals(tenant.getActive()),
                plan.commercialPlan(),
                plan.enabledModules(),
                users);
    }

    private List<ModuleCatalogView> moduleCatalog() {
        return Arrays.stream(ModuleCode.values())
                .map(module -> new ModuleCatalogView(module.moduleKey(), module.name()))
                .sorted(Comparator.comparing(ModuleCatalogView::moduleKey))
                .toList();
    }

    private List<String> availablePlanNames() {
        return List.of(
                CommercialPlanCode.START.name(),
                CommercialPlanCode.PRO.name(),
                CommercialPlanCode.VISION_AI.name()
        );
    }

    private Set<ModuleCode> parseModuleCodes(List<String> moduleKeys) {
        Set<ModuleCode> modules = new HashSet<>();
        if (moduleKeys == null) {
            return modules;
        }
        for (String moduleKey : moduleKeys) {
            try {
                modules.add(ModuleCode.fromModuleKey(moduleKey));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Modulo invalido: " + moduleKey);
            }
        }
        return modules;
    }

    /**
     * Alta pública de un tenant (desde /auth/register-tenant).
     * No requiere rol platform-admin; el caller es responsable de crear el usuario
     * administrador asociado. Normaliza el código, valida unicidad y crea la
     * suscripción al plan START.
     */
    private static final java.util.Set<String> RESERVED_TENANT_CODES = java.util.Set.of(
            "admin", "administrator", "api", "app", "auth", "billing", "console",
            "default", "dev", "docs", "example", "help", "internal", "login", "mail",
            "management", "oauth", "platform", "public", "root", "settings", "signup",
            "static", "status", "super", "superadmin", "support", "system", "test",
            "tenant", "ucacue", "user", "users", "www");

    @Transactional
    @CacheEvict(cacheNames = "tenant-plans", allEntries = true)
    public TenantEntity registerPublicTenant(String tenantCode, String legalName, String tradeName) {
        String normalized = normalizeTenantCode(tenantCode);
        if (RESERVED_TENANT_CODES.contains(normalized)) {
            throw new BadRequestException("El código solicitado está reservado, elige otro");
        }
        tenantRepository.findByTenantCodeIgnoreCase(normalized).ifPresent(existing -> {
            throw new BadRequestException("Ya existe un negocio con ese código");
        });
        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(normalized);
        tenant.setLegalName(blankToDefault(legalName, normalized));
        tenant.setTradeName(blankToNull(tradeName));
        tenant.setCountryCode("EC");
        tenant.setTimeZone("America/Guayaquil");
        tenant.setCurrencyCode("USD");
        tenant.setActive(Boolean.TRUE);
        TenantEntity saved = tenantRepository.save(tenant);
        createSubscription(saved, CommercialPlanCode.START);
        return saved;
    }

    private void createSubscription(TenantEntity tenant, CommercialPlanCode planCode) {
        TenantPlanSubscriptionEntity subscription = new TenantPlanSubscriptionEntity();
        subscription.setTenant(tenant);
        subscription.setCommercialPlan(planCode);
        subscription.setStatus(TenantPlanSubscriptionEntity.SubscriptionStatus.ACTIVE);
        subscription.setStartsAt(LocalDateTime.now());
        tenantPlanSubscriptionRepository.save(subscription);
    }

    private String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BadRequestException("El codigo del negocio es obligatorio");
        }
        String normalized = tenantCode.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank() || !normalized.matches(TENANT_CODE_PATTERN)) {
            throw new BadRequestException("tenantCode invalido");
        }
        return normalized;
    }

    private String blankToDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private CommercialPlanCode resolveGlobalFallbackPlan() {
        return settingRepository.findByKey(GLOBAL_COMMERCIAL_PLAN_KEY)
                .map(SettingEntity::getValue)
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new IllegalStateException("Global commercial plan setting is blank");
                    }
                    try {
                        return CommercialPlanCode.from(value);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalStateException("Global commercial plan setting is invalid: " + value, ex);
                    }
                })
                .orElse(CommercialPlanCode.START);
    }

    public record TenantPlanView(
            String tenantCode,
            String commercialPlan,
            List<String> enabledModules,
            List<String> availablePlans) {
    }

    public record CreateTenantCommand(
            String tenantCode,
            String legalName,
            String tradeName,
            String countryCode,
            String timeZone,
            String currencyCode) {
    }

    public record AdminTenantCatalogView(
            List<AdminTenantView> tenants,
            List<ModuleCatalogView> availableModules,
            List<String> availablePlans) {
    }

    public record AdminTenantView(
            String tenantCode,
            String legalName,
            String tradeName,
            boolean active,
            String commercialPlan,
            List<String> enabledModules,
            long userCount) {
    }

    public record ModuleCatalogView(String moduleKey, String label) {
    }
}

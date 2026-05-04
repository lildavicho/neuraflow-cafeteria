package com.ucacue.bar.erp.onboarding.application;

import com.ucacue.bar.erp.accounting.application.SriDocumentService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingAuditEventEntity;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingCatalogItemEntity;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingReferenceFileEntity;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingSessionEntity;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingAuditEventRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingCatalogItemRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingReferenceFileRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingSessionRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OnboardingWizardService {

    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{13}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern THREE_DIGIT_PATTERN = Pattern.compile("^\\d{3}$");
    private static final Pattern NINE_DIGIT_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Set<String> RIDE_REFERENCE_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg", ".doc", ".docx");
    private static final Set<String> RIDE_REFERENCE_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream");

    private static final List<String> WIZARD_STEPS = List.of(
            "business-discovery",
            "legal-entity",
            "tax-profile",
            "certificate-connectivity",
            "emission-points",
            "accounting-template",
            "sales-pos",
            "purchases",
            "inventory",
            "crm-hr-bi",
            "brand-studio",
            "publish-review");

    private static final Set<String> REQUIRED_PUBLISH_STEPS = Set.of(
            "business-discovery",
            "legal-entity",
            "tax-profile",
            "certificate-connectivity",
            "emission-points",
            "accounting-template",
            "sales-pos",
            "purchases",
            "inventory",
            "brand-studio");

    private static final Set<String> BUSINESS_MODES = Set.of("b2c", "b2b", "mixed");
    private static final Set<String> RIMPE_TYPES = Set.of("entrepreneur", "popular");
    private static final Set<String> COA_TEMPLATES = Set.of("EC_NIIF_BASE", "EC_RETAIL", "EC_RESTAURANT", "EC_EDUCATION");
    private static final Set<String> COSTING_METHODS = Set.of("AVCO", "FIFO");
    private static final Set<String> CREDIT_POLICIES = Set.of("cash-first", "soft-credit", "hard-limit");
    private static final Set<String> DISPATCH_POLICIES = Set.of("stock-first", "reserve-first", "cross-dock");
    private static final Set<String> SERVICE_RETENTION_DEFAULTS = Set.of("30", "70", "100");
    private static final List<String> DOCUMENT_TYPES_ORDER = List.of("01", "03", "04", "05", "06", "07");
    private static final List<String> PAYMENT_METHODS_ORDER = List.of("01", "15", "16", "17", "18", "19", "20", "21");
    private static final List<String> DIMENSIONS_ORDER = List.of("BRANCH", "BUSINESS_LINE", "WAREHOUSE", "COST_CENTER", "SALESPERSON", "PROJECT");
    private static final Set<String> DOCUMENT_TYPES = Set.of("01", "03", "04", "05", "06", "07");
    private static final Set<String> PAYMENT_METHODS = Set.of("01", "15", "16", "17", "18", "19", "20", "21");
    private static final Set<String> DIMENSIONS = Set.copyOf(DIMENSIONS_ORDER);
    private static final Set<String> RIDE_TEMPLATES = Set.of("Classic", "Modern", "Minimal");
    private static final Set<String> REQUIRED_GROWTH_KEYS = Set.of("crmEnabled", "hrEnabled", "biEnabled", "emailRelayEnabled");

    private final TenantContextResolver tenantContextResolver;
    private final TenantRepository tenantRepository;
    private final OnboardingSessionRepository sessionRepository;
    private final OnboardingAuditEventRepository auditEventRepository;
    private final OnboardingCatalogItemRepository catalogItemRepository;
    private final OnboardingReferenceFileRepository referenceFileRepository;
    private final SriDocumentService sriDocumentService;

    @Transactional
    public OnboardingSessionView getOrCreate(String tenantCode) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        OnboardingSessionEntity session = sessionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createInitialSession(tenant));
        return toView(tenant, session);
    }

    @Transactional
    public OnboardingSessionView updateStep(String tenantCode, String step, Map<String, Object> payload) {
        validateStep(step);
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        OnboardingSessionEntity session = sessionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createInitialSession(tenant));

        Map<String, Object> safePayload = sanitizeStepPayload(session, step, mutableMap(payload));
        applyStep(session, step, safePayload);
        syncTenantIdentity(tenant, session);
        session.setCurrentStep(step);
        session.setStatus("PUBLISHED".equals(session.getStatus()) ? "DRAFT" : session.getStatus());
        session.setChecklist(rebuildChecklist(session, step));
        session.setWarnings(rebuildWarnings(session));
        session.setProgressPercent(calculateProgress(session));

        tenantRepository.save(tenant);
        OnboardingSessionEntity saved = sessionRepository.save(session);
        appendAudit(tenant.getId(), saved.getId(), "STEP_SAVED", Map.of(
                "step", step,
                "progressPercent", saved.getProgressPercent()));
        return toView(tenant, saved);
    }

    @Transactional
    public OnboardingSessionView publish(String tenantCode) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        OnboardingSessionEntity session = sessionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createInitialSession(tenant));

        List<String> missing = missingPublishRequirements(session);
        if (!missing.isEmpty()) {
            throw new BadRequestException("Onboarding incompleto: " + String.join(", ", missing));
        }

        syncTenantIdentity(tenant, session);
        session.setStatus("PUBLISHED");
        session.setCurrentStep("publish-review");
        session.setPublishedAt(LocalDateTime.now());
        session.setProgressPercent(100);
        session.setChecklist(rebuildChecklist(session, "publish-review"));
        session.getChecklist().put("publish-review", true);
        session.setWarnings(rebuildWarnings(session));

        tenantRepository.save(tenant);
        OnboardingSessionEntity saved = sessionRepository.save(session);
        appendAudit(tenant.getId(), saved.getId(), "TENANT_PUBLISHED", Map.of(
                "progressPercent", saved.getProgressPercent(),
                "verticalTemplate", Objects.toString(saved.getVerticalTemplate(), "")));
        return toView(tenant, saved);
    }

    @Transactional(readOnly = true)
    public OnboardingCatalogView catalog() {
        return new OnboardingCatalogView(
                items("sri_document_type"),
                items("sri_payment_method"),
                items("vertical_template"),
                items("fiscal_flag"));
    }

    public RucValidationView validateRuc(String ruc) {
        boolean valid = isValidEcuadorRuc(ruc);
        return new RucValidationView(valid, valid
                ? "RUC ecuatoriano valido"
                : "El RUC no cumple la validacion ecuatoriana o le falta establecimiento");
    }

    @Transactional
    public OnboardingSessionView uploadSriCertificate(String tenantCode, MultipartFile file, String password,
            String alias, String environment) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        OnboardingSessionEntity session = sessionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createInitialSession(tenant));
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Selecciona el archivo .p12 de la firma electrónica");
        }
        String originalName = sanitizeFileName(file.getOriginalFilename());
        if (!originalName.toLowerCase().endsWith(".p12")) {
            throw new BadRequestException("La firma electrónica debe ser un archivo .p12");
        }
        if (file.getSize() > 2_500_000) {
            throw new BadRequestException("El archivo .p12 supera el tamano permitido");
        }
        if (!hasText(password)) {
            throw new BadRequestException("Escribe la clave de la firma electrónica");
        }

        Map<String, Object> legal = session.getLegalEntity();
        Map<String, Object> tax = session.getTaxProfile();
        String ruc = asString(legal.get("ruc"));
        String legalName = firstText(asString(legal.get("legalName")), tenant.getLegalName());
        String tradeName = firstText(asString(legal.get("tradeName")), tenant.getTradeName(), legalName);
        String matrixAddress = asString(legal.get("matrixAddress"));
        if (!isValidEcuadorRuc(ruc) || !hasText(legalName) || !hasText(matrixAddress)) {
            throw new BadRequestException("Primero completa Datos del RUC: RUC de 13 dígitos, razón social y dirección matriz");
        }

        try {
            String p12Base64 = Base64.getEncoder().encodeToString(file.getBytes());
            sriDocumentService.upsertConfiguration(tenant.getTenantCode(), new SriDocumentService.UpsertSriConfigurationCommand(
                    normalizeWizardEnvironment(environment),
                    "1",
                    ruc,
                    legalName,
                    tradeName,
                    matrixAddress,
                    firstText(asString(legal.get("establishmentAddress")), matrixAddress),
                    asString(tax.get("specialTaxpayerResolution")),
                    firstText(asString(legal.get("obligatedAccounting")), "NO"),
                    bool(tax.get("rimpe")),
                    bool(tax.get("retention_agent")) || bool(tax.get("retentionAgent")),
                    firstText(asString(tax.get("retentionResolution")), asString(tax.get("retention_agent_resolution"))),
                    "PKCS12",
                    p12Base64,
                    password,
                    trimToNull(alias),
                    false,
                    true));
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el archivo .p12", ex);
        }

        Map<String, Object> certificate = mutableMap(session.getCertificateStatus());
        certificate.put("environment", normalizeWizardEnvironment(environment));
        certificate.put("hasP12", true);
        certificate.put("p12FileName", originalName);
        certificate.put("p12StoredInTenant", true);
        certificate.put("signatureMode", "PKCS12");
        certificate.put("certificateAlias", trimToNull(alias));
        certificate.put("signatureTestPassed", true);
        certificate.put("uploadedAt", LocalDateTime.now().toString());
        session.setCertificateStatus(certificate);
        session.setCurrentStep("certificate-connectivity");
        session.setChecklist(rebuildChecklist(session, "certificate-connectivity"));
        session.setWarnings(rebuildWarnings(session));
        session.setProgressPercent(calculateProgress(session));
        OnboardingSessionEntity saved = sessionRepository.save(session);
        appendAudit(tenant.getId(), saved.getId(), "SRI_CERTIFICATE_UPLOADED", Map.of(
                "fileName", originalName,
                "environment", normalizeWizardEnvironment(environment)));
        return toView(tenant, saved);
    }

    @Transactional
    public OnboardingSessionView uploadRideReference(String tenantCode, MultipartFile file) {
        TenantEntity tenant = tenantContextResolver.resolve(tenantCode);
        OnboardingSessionEntity session = sessionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createInitialSession(tenant));
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Selecciona el modelo de comprobante que quieres usar como referencia");
        }
        if (file.getSize() > 5_000_000) {
            throw new BadRequestException("El modelo de comprobante supera 5MB");
        }
        String originalName = sanitizeFileName(file.getOriginalFilename());
        if (!validRideReferenceFile(originalName, file.getContentType())) {
            throw new BadRequestException("El modelo debe ser PDF, PNG, JPG o Word y pesar máximo 5MB");
        }
        try {
            OnboardingReferenceFileEntity reference = new OnboardingReferenceFileEntity();
            reference.setTenantId(tenant.getId());
            reference.setSessionId(session.getId());
            reference.setFileKind("RIDE_REFERENCE");
            reference.setFileName(originalName);
            reference.setContentType(file.getContentType());
            reference.setFileSize(file.getSize());
            reference.setContent(file.getBytes());
            OnboardingReferenceFileEntity savedReference = referenceFileRepository.save(reference);

            Map<String, Object> brand = mutableMap(session.getBrandSetup());
            brand.put("customRideReferenceUploaded", true);
            brand.put("customRideReferenceFileId", savedReference.getId());
            brand.put("customRideReferenceFileName", savedReference.getFileName());
            brand.put("customRideReferenceUploadedAt", LocalDateTime.now().toString());
            session.setBrandSetup(brand);
            session.setCurrentStep("brand-studio");
            session.setChecklist(rebuildChecklist(session, "brand-studio"));
            session.setWarnings(rebuildWarnings(session));
            session.setProgressPercent(calculateProgress(session));
            OnboardingSessionEntity saved = sessionRepository.save(session);
            appendAudit(tenant.getId(), saved.getId(), "RIDE_REFERENCE_UPLOADED", Map.of(
                    "fileName", savedReference.getFileName(),
                    "fileSize", savedReference.getFileSize()));
            return toView(tenant, saved);
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el modelo de comprobante", ex);
        }
    }

    private OnboardingSessionEntity createInitialSession(TenantEntity tenant) {
        OnboardingSessionEntity session = new OnboardingSessionEntity();
        session.setTenantId(tenant.getId());
        session.setBusinessProfile(new LinkedHashMap<>(Map.of(
                "verticalTemplate", "food_restaurant",
                "businessMode", "b2c",
                "branches", 1,
                "usesPos", true)));
        session.setLegalEntity(new LinkedHashMap<>(Map.of(
                "legalName", tenant.getLegalName(),
                "tradeName", tenant.getTradeName() == null ? tenant.getLegalName() : tenant.getTradeName(),
                "currencyCode", tenant.getCurrencyCode(),
                "timeZone", tenant.getTimeZone())));
        session.setTaxProfile(new LinkedHashMap<>(Map.of(
                "environment", "test",
                "obligatedAccounting", "NO",
                "rimpe", false,
                "retentionAgent", false)));
        session.setEmissionSetup(new LinkedHashMap<>(Map.of(
                "establishmentCode", "001",
                "emissionPointCode", "001",
                "invoiceCurrentNumber", "000000001",
                "enabledDocuments", List.of("01"))));
        session.setAccountingSetup(new LinkedHashMap<>(Map.of(
                "coaTemplate", "EC_NIIF_BASE",
                "costingMethod", "AVCO",
                "dimensions", List.of("BRANCH", "BUSINESS_LINE"))));
        session.setSalesSetup(new LinkedHashMap<>(Map.of(
                "defaultPricelist", "Precio público",
                "creditPolicy", "cash-first",
                "paymentMethods", List.of("01", "16", "19"),
                "creditEnabled", false)));
        session.setPurchasesSetup(new LinkedHashMap<>(Map.of(
                "defaultTaxSupport", "Compras locales",
                "serviceRetentionDefault", "70",
                "supplierAuthorizationRequired", false,
                "purchaseApprovalEnabled", false)));
        session.setInventorySetup(new LinkedHashMap<>(Map.of(
                "warehouseCount", 1,
                "dispatchPolicy", "stock-first",
                "lotsEnabled", false,
                "barcodesEnabled", false,
                "replenishmentEnabled", false)));
        session.setGrowthSetup(new LinkedHashMap<>(Map.of(
                "crmEnabled", false,
                "hrEnabled", false,
                "biEnabled", false,
                "emailRelayEnabled", false)));
        session.setBrandSetup(new LinkedHashMap<>(Map.of(
                "primaryColor", "#4F46E5",
                "accentColor", "#8B5CF6",
                "template", "Classic",
                "legalSafeMode", true)));
        session.setWarnings(rebuildWarnings(session));
        session.setProgressPercent(calculateProgress(session));
        return sessionRepository.save(session);
    }

    private void applyStep(OnboardingSessionEntity session, String step, Map<String, Object> payload) {
        switch (step) {
            case "business-discovery" -> {
                session.setBusinessProfile(payload);
                session.setVerticalTemplate(asString(payload.get("verticalTemplate")));
            }
            case "legal-entity" -> session.setLegalEntity(payload);
            case "tax-profile" -> session.setTaxProfile(payload);
            case "certificate-connectivity" -> session.setCertificateStatus(payload);
            case "emission-points" -> session.setEmissionSetup(payload);
            case "accounting-template" -> session.setAccountingSetup(payload);
            case "sales-pos" -> session.setSalesSetup(payload);
            case "purchases" -> session.setPurchasesSetup(payload);
            case "inventory" -> session.setInventorySetup(payload);
            case "crm-hr-bi" -> session.setGrowthSetup(payload);
            case "brand-studio" -> session.setBrandSetup(payload);
            case "publish-review" -> session.setPermissionsSetup(payload);
            default -> throw new BadRequestException("Paso de onboarding no soportado: " + step);
        }
    }

    private Map<String, Object> rebuildChecklist(OnboardingSessionEntity session, String savedStep) {
        Map<String, Object> checklist = mutableMap(session.getChecklist());
        for (String step : WIZARD_STEPS) {
            boolean touched = step.equals(savedStep) || Boolean.TRUE.equals(checklist.get(step));
            checklist.put(step, touched && isStepComplete(session, step));
        }

        String ruc = asString(session.getLegalEntity().get("ruc"));
        checklist.put("ruc-format", isValidEcuadorRuc(ruc));

        Map<String, Object> tax = session.getTaxProfile();
        boolean rimpe = bool(tax.get("rimpe"));
        boolean retentionAgent = bool(tax.get("retentionAgent")) || bool(tax.get("retention_agent"));
        boolean specialTaxpayer = bool(tax.get("special_taxpayer"));
        checklist.put("rimpe-legend", !rimpe || hasText(asString(tax.get("rimpeType"))));
        checklist.put("retention-resolution", !retentionAgent || hasText(asString(tax.get("retentionResolution"))));
        checklist.put("special-taxpayer-resolution", !specialTaxpayer || hasText(asString(tax.get("specialTaxpayerResolution"))));

        Map<String, Object> emission = session.getEmissionSetup();
        checklist.put("sri-sequences", validThreeDigits(emission.get("establishmentCode"))
                && validThreeDigits(emission.get("emissionPointCode"))
                && validNineDigits(emission.get("invoiceCurrentNumber")));
        checklist.put("invoice-document-enabled", arrayOfStrings(emission.get("enabledDocuments")).contains("01"));

        Map<String, Object> sales = session.getSalesSetup();
        checklist.put("payment-methods", !arrayOfStrings(sales.get("paymentMethods")).isEmpty());

        Map<String, Object> brand = session.getBrandSetup();
        checklist.put("ride-legal-safe-mode", !brand.containsKey("legalSafeMode") || bool(brand.get("legalSafeMode")));
        return checklist;
    }

    private List<Map<String, Object>> rebuildWarnings(OnboardingSessionEntity session) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        Map<String, Object> legal = session.getLegalEntity();
        Map<String, Object> tax = session.getTaxProfile();
        Map<String, Object> certificate = session.getCertificateStatus();
        Map<String, Object> brand = session.getBrandSetup();
        Map<String, Object> checklist = rebuildChecklist(session, session.getCurrentStep());

        String ruc = asString(legal.get("ruc"));
        if (hasText(ruc) && !isValidEcuadorRuc(ruc)) {
            warnings.add(warning("legal-entity", "RUC invalido", "El RUC debe pasar la validacion ecuatoriana antes de emitir comprobantes SRI."));
        }
        if (!hasText(asString(legal.get("legalName"))) || !hasText(asString(legal.get("matrixAddress")))) {
            warnings.add(warning("legal-entity", "Datos legales incompletos", "Razon social y direccion matriz deben quedar completas para cada tenant."));
        }
        if (bool(tax.get("withholdsVat100BySectorOnly"))) {
            warnings.add(warning("tax-profile", "Dato tributario por confirmar", "No configures retencion 100% de IVA solo por sector; confirma naturaleza de operacion y perfil tributario."));
        }
        if ((bool(tax.get("retentionAgent")) || bool(tax.get("retention_agent")))
                && !hasText(asString(tax.get("retentionResolution")))) {
            warnings.add(warning("tax-profile", "Falta resolucion de retencion", "Si el tenant es agente de retencion, debes registrar la resolucion antes de activar el negocio."));
        }
        if (bool(tax.get("special_taxpayer")) && !hasText(asString(tax.get("specialTaxpayerResolution")))) {
            warnings.add(warning("tax-profile", "Falta resolucion de contribuyente especial", "Si el tenant es contribuyente especial, registra la resolucion antes de activar el negocio."));
        }
        if (!bool(certificate.get("hasP12")) && !bool(certificate.get("p12StoredInTenant"))) {
            warnings.add(warning("certificate-connectivity", "Firma electronica pendiente", "Cada tenant debe guardar su archivo .p12 para facturar y firmar en segundo plano."));
        }
        if ("2".equals(normalizeWizardEnvironment(asString(certificate.get("environment"))))
                && !bool(certificate.get("authorizationPollingPassed"))) {
            warnings.add(warning("certificate-connectivity", "Prueba SRI pendiente", "Produccion exige prueba de firma, XSD, recepcion y consulta de autorizacion."));
        }
        if (!Boolean.TRUE.equals(checklist.get("invoice-document-enabled"))) {
            warnings.add(warning("emission-points", "Factura no habilitada", "Debes habilitar el comprobante 01 Factura para el flujo comercial base."));
        }
        if (!Boolean.TRUE.equals(checklist.get("payment-methods"))) {
            warnings.add(warning("sales-pos", "Sin formas de pago", "Configura al menos una forma de pago para que el tenant pueda vender sin bloquearse."));
        }
        if (brand.containsKey("legalSafeMode") && !bool(brand.get("legalSafeMode"))) {
            warnings.add(warning("brand-studio", "RIDE sin zona legal bloqueada", "Clave de acceso, QR, RUC y leyendas fiscales no deben quedar editables."));
        }
        return warnings;
    }

    private int calculateProgress(OnboardingSessionEntity session) {
        Map<String, Object> checklist = session.getChecklist() == null ? Map.of() : session.getChecklist();
        long done = WIZARD_STEPS.stream()
                .filter(step -> Boolean.TRUE.equals(checklist.get(step)))
                .count();
        return (int) Math.min(100, Math.round(done * 100.0 / WIZARD_STEPS.size()));
    }

    private List<String> missingPublishRequirements(OnboardingSessionEntity session) {
        Map<String, Object> checklist = rebuildChecklist(session, "publish-review");
        List<String> missing = REQUIRED_PUBLISH_STEPS.stream()
                .filter(step -> !Boolean.TRUE.equals(checklist.get(step)))
                .map(this::publishLabel)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(ArrayList::new));
        if (!Boolean.TRUE.equals(checklist.get("ruc-format"))) {
            missing.add("RUC valido");
        }
        if (!Boolean.TRUE.equals(checklist.get("sri-sequences"))) {
            missing.add("Numeracion SRI");
        }
        if (!Boolean.TRUE.equals(checklist.get("invoice-document-enabled"))) {
            missing.add("Comprobante 01 Factura");
        }
        if (!Boolean.TRUE.equals(checklist.get("payment-methods"))) {
            missing.add("Formas de pago");
        }
        if (!Boolean.TRUE.equals(checklist.get("ride-legal-safe-mode"))) {
            missing.add("Zona legal del comprobante");
        }
        return missing.stream().distinct().toList();
    }

    private Map<String, Object> sanitizeStepPayload(OnboardingSessionEntity session, String step, Map<String, Object> payload) {
        return switch (step) {
            case "business-discovery" -> sanitizeBusinessDiscovery(payload);
            case "legal-entity" -> sanitizeLegalEntity(payload, session.getLegalEntity());
            case "tax-profile" -> sanitizeTaxProfile(payload);
            case "certificate-connectivity" -> sanitizeCertificateStatus(payload, session.getCertificateStatus());
            case "emission-points" -> sanitizeEmissionSetup(payload);
            case "accounting-template" -> sanitizeAccountingSetup(payload);
            case "sales-pos" -> sanitizeSalesSetup(payload);
            case "purchases" -> sanitizePurchasesSetup(payload);
            case "inventory" -> sanitizeInventorySetup(payload);
            case "crm-hr-bi" -> sanitizeGrowthSetup(payload);
            case "brand-studio" -> sanitizeBrandSetup(payload, session.getBrandSetup());
            case "publish-review" -> sanitizePermissionsSetup(payload, session.getPermissionsSetup());
            default -> throw new BadRequestException("Paso de onboarding no soportado: " + step);
        };
    }

    private Map<String, Object> sanitizeBusinessDiscovery(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("verticalTemplate", firstAllowed(asString(payload.get("verticalTemplate")), items("vertical_template").stream()
                .map(CatalogItemView::itemCode)
                .collect(Collectors.toSet()), "food_restaurant"));
        value.put("customActivity", trimToNull(asString(payload.get("customActivity"))));
        value.put("businessMode", firstAllowed(asString(payload.get("businessMode")), BUSINESS_MODES, "b2c"));
        value.put("branches", boundedInteger(payload.get("branches"), 1, 999, 1, "El numero de locales debe estar entre 1 y 999"));
        value.put("usesPos", bool(payload.get("usesPos")));
        value.put("importsGoods", bool(payload.get("importsGoods")));
        value.put("needsRecurringBilling", bool(payload.get("needsRecurringBilling")));
        return value;
    }

    private Map<String, Object> sanitizeLegalEntity(Map<String, Object> payload, Map<String, Object> current) {
        Map<String, Object> value = mutableMap(current);
        String ruc = trimToNull(asString(payload.get("ruc")));
        if (ruc != null) {
            String numericRuc = ruc.replaceAll("\\D", "");
            if (!numericRuc.isEmpty() && numericRuc.length() != 13) {
                throw new BadRequestException("El RUC debe tener 13 digitos");
            }
            value.put("ruc", numericRuc);
        }
        String email = trimToNull(asString(payload.get("email")));
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("El correo del negocio no tiene un formato valido");
        }
        putTrimmed(value, "legalName", payload.get("legalName"));
        putTrimmed(value, "tradeName", payload.get("tradeName"));
        putTrimmed(value, "matrixAddress", payload.get("matrixAddress"));
        putTrimmed(value, "establishmentAddress", payload.get("establishmentAddress"));
        putTrimmed(value, "phone", payload.get("phone"));
        value.put("email", email == null ? "" : email.toLowerCase(Locale.ROOT));
        value.put("obligatedAccounting", "SI".equalsIgnoreCase(asString(payload.get("obligatedAccounting"))) ? "SI" : "NO");
        value.put("currencyCode", firstText(asString(payload.get("currencyCode")), asString(current.get("currencyCode")), "USD"));
        value.put("timeZone", firstText(asString(payload.get("timeZone")), asString(current.get("timeZone")), "America/Guayaquil"));
        return value;
    }

    private Map<String, Object> sanitizeTaxProfile(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        boolean rimpe = bool(payload.get("rimpe"));
        boolean retentionAgent = bool(payload.get("retentionAgent")) || bool(payload.get("retention_agent"));
        value.put("rimpe", rimpe);
        value.put("retentionAgent", retentionAgent);
        value.put("retention_agent", retentionAgent);
        value.put("special_taxpayer", bool(payload.get("special_taxpayer")));
        value.put("large_taxpayer", bool(payload.get("large_taxpayer")));
        value.put("habitual_exporter", bool(payload.get("habitual_exporter")));
        value.put("construction_materials", bool(payload.get("construction_materials")));
        value.put("commercial_transport", bool(payload.get("commercial_transport")));
        value.put("fuel_sales", bool(payload.get("fuel_sales")));
        value.put("tourism_special_vat", bool(payload.get("tourism_special_vat")));
        value.put("withholdsVat100BySectorOnly", bool(payload.get("withholdsVat100BySectorOnly")));
        value.put("rimpeType", rimpe ? firstAllowed(asString(payload.get("rimpeType")), RIMPE_TYPES, "") : "");
        value.put("retentionResolution", trimToNull(asString(payload.get("retentionResolution"))));
        value.put("specialTaxpayerResolution", trimToNull(asString(payload.get("specialTaxpayerResolution"))));
        return value;
    }

    private Map<String, Object> sanitizeCertificateStatus(Map<String, Object> payload, Map<String, Object> current) {
        Map<String, Object> value = mutableMap(current);
        value.put("environment", normalizeWizardEnvironment(asString(payload.get("environment"))));
        value.put("certificateAlias", trimToNull(asString(payload.get("certificateAlias"))));
        value.put("signatureTestPassed", bool(payload.get("signatureTestPassed")) || bool(current.get("signatureTestPassed")));
        value.put("authorizationPollingPassed", bool(payload.get("authorizationPollingPassed")) || bool(current.get("authorizationPollingPassed")));
        value.put("hasP12", bool(current.get("hasP12")));
        value.put("p12StoredInTenant", bool(current.get("p12StoredInTenant")));
        return value;
    }

    private Map<String, Object> sanitizeEmissionSetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("establishmentCode", normalizeDigits(payload.get("establishmentCode"), 3, "El codigo de establecimiento debe tener 3 digitos"));
        value.put("emissionPointCode", normalizeDigits(payload.get("emissionPointCode"), 3, "El punto de emision debe tener 3 digitos"));
        value.put("invoiceCurrentNumber", normalizeDigits(payload.get("invoiceCurrentNumber"), 9, "El secuencial inicial debe tener 9 digitos"));
        List<String> enabledDocuments = orderedSelections(payload.get("enabledDocuments"), DOCUMENT_TYPES, DOCUMENT_TYPES_ORDER);
        value.put("enabledDocuments", enabledDocuments.isEmpty() ? List.of("01") : enabledDocuments);
        return value;
    }

    private Map<String, Object> sanitizeAccountingSetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("coaTemplate", firstAllowed(asString(payload.get("coaTemplate")), COA_TEMPLATES, "EC_NIIF_BASE"));
        value.put("costingMethod", firstAllowed(asString(payload.get("costingMethod")), COSTING_METHODS, "AVCO"));
        List<String> dimensions = orderedSelections(payload.get("dimensions"), DIMENSIONS, DIMENSIONS_ORDER);
        value.put("dimensions", dimensions.isEmpty() ? List.of("BRANCH", "BUSINESS_LINE") : dimensions);
        return value;
    }

    private Map<String, Object> sanitizeSalesSetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        putTrimmed(value, "defaultPricelist", payload.get("defaultPricelist"));
        value.put("creditPolicy", firstAllowed(asString(payload.get("creditPolicy")), CREDIT_POLICIES, "cash-first"));
        value.put("paymentMethods", orderedSelections(payload.get("paymentMethods"), PAYMENT_METHODS, PAYMENT_METHODS_ORDER));
        value.put("tipsEnabled", bool(payload.get("tipsEnabled")));
        value.put("identifyCustomerRequired", bool(payload.get("identifyCustomerRequired")));
        value.put("creditEnabled", !"cash-first".equals(value.get("creditPolicy")));
        return value;
    }

    private Map<String, Object> sanitizePurchasesSetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        putTrimmed(value, "defaultTaxSupport", payload.get("defaultTaxSupport"));
        value.put("serviceRetentionDefault", firstAllowed(asString(payload.get("serviceRetentionDefault")), SERVICE_RETENTION_DEFAULTS, "70"));
        value.put("supplierAuthorizationRequired", bool(payload.get("supplierAuthorizationRequired")));
        value.put("purchaseApprovalEnabled", bool(payload.get("purchaseApprovalEnabled")));
        return value;
    }

    private Map<String, Object> sanitizeInventorySetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("warehouseCount", boundedInteger(payload.get("warehouseCount"), 1, 999, 1, "La cantidad de bodegas debe estar entre 1 y 999"));
        value.put("dispatchPolicy", firstAllowed(asString(payload.get("dispatchPolicy")), DISPATCH_POLICIES, "stock-first"));
        value.put("lotsEnabled", bool(payload.get("lotsEnabled")));
        value.put("barcodesEnabled", bool(payload.get("barcodesEnabled")));
        value.put("replenishmentEnabled", bool(payload.get("replenishmentEnabled")));
        return value;
    }

    private Map<String, Object> sanitizeGrowthSetup(Map<String, Object> payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("crmEnabled", bool(payload.get("crmEnabled")));
        value.put("hrEnabled", bool(payload.get("hrEnabled")));
        value.put("biEnabled", bool(payload.get("biEnabled")));
        value.put("emailRelayEnabled", bool(payload.get("emailRelayEnabled")));
        return value;
    }

    private Map<String, Object> sanitizeBrandSetup(Map<String, Object> payload, Map<String, Object> current) {
        Map<String, Object> value = mutableMap(current);
        value.put("primaryColor", normalizeColor(payload.get("primaryColor"), firstText(asString(current.get("primaryColor")), "#4F46E5")));
        value.put("accentColor", normalizeColor(payload.get("accentColor"), firstText(asString(current.get("accentColor")), "#8B5CF6")));
        value.put("template", firstAllowed(asString(payload.get("template")), RIDE_TEMPLATES, "Classic"));
        value.put("footerNote", trimToNull(asString(payload.get("footerNote"))));
        value.put("legalSafeMode", !payload.containsKey("legalSafeMode") || bool(payload.get("legalSafeMode")));
        if (payload.containsKey("logoUrl")) {
            putTrimmed(value, "logoUrl", payload.get("logoUrl"));
        }
        if (payload.containsKey("logoStoragePath")) {
            putTrimmed(value, "logoStoragePath", payload.get("logoStoragePath"));
        }
        if (payload.containsKey("logoDataUrl")) {
            value.put("logoDataUrl", trimToNull(asString(payload.get("logoDataUrl"))));
        }
        return value;
    }

    private Map<String, Object> sanitizePermissionsSetup(Map<String, Object> payload, Map<String, Object> current) {
        return mutableMap(current == null || current.isEmpty() ? payload : current);
    }

    private boolean isStepComplete(OnboardingSessionEntity session, String step) {
        return switch (step) {
            case "business-discovery" -> {
                Map<String, Object> business = session.getBusinessProfile();
                String vertical = asString(business.get("verticalTemplate"));
                yield hasText(vertical)
                        && BUSINESS_MODES.contains(firstAllowed(asString(business.get("businessMode")), BUSINESS_MODES, ""))
                        && boundedInteger(business.get("branches"), 1, 999, 0, null) > 0
                        && (!"other_custom".equals(vertical) || hasText(asString(business.get("customActivity"))));
            }
            case "legal-entity" -> {
                Map<String, Object> legal = session.getLegalEntity();
                yield isValidEcuadorRuc(asString(legal.get("ruc")))
                        && hasText(asString(legal.get("legalName")))
                        && hasText(asString(legal.get("matrixAddress")))
                        && ("SI".equals(asString(legal.get("obligatedAccounting"))) || "NO".equals(asString(legal.get("obligatedAccounting"))));
            }
            case "tax-profile" -> {
                Map<String, Object> tax = session.getTaxProfile();
                boolean rimpe = bool(tax.get("rimpe"));
                boolean retentionAgent = bool(tax.get("retentionAgent")) || bool(tax.get("retention_agent"));
                boolean specialTaxpayer = bool(tax.get("special_taxpayer"));
                yield (!rimpe || hasText(asString(tax.get("rimpeType"))))
                        && (!retentionAgent || hasText(asString(tax.get("retentionResolution"))))
                        && (!specialTaxpayer || hasText(asString(tax.get("specialTaxpayerResolution"))));
            }
            case "certificate-connectivity" -> {
                Map<String, Object> certificate = session.getCertificateStatus();
                yield (bool(certificate.get("hasP12")) || bool(certificate.get("p12StoredInTenant")))
                        && Set.of("1", "2").contains(normalizeWizardEnvironment(asString(certificate.get("environment"))));
            }
            case "emission-points" -> {
                Map<String, Object> emission = session.getEmissionSetup();
                yield validThreeDigits(emission.get("establishmentCode"))
                        && validThreeDigits(emission.get("emissionPointCode"))
                        && validNineDigits(emission.get("invoiceCurrentNumber"))
                        && !arrayOfStrings(emission.get("enabledDocuments")).isEmpty();
            }
            case "accounting-template" -> {
                Map<String, Object> accounting = session.getAccountingSetup();
                yield COA_TEMPLATES.contains(asString(accounting.get("coaTemplate")))
                        && COSTING_METHODS.contains(asString(accounting.get("costingMethod")))
                        && !arrayOfStrings(accounting.get("dimensions")).isEmpty();
            }
            case "sales-pos" -> {
                Map<String, Object> sales = session.getSalesSetup();
                yield hasText(asString(sales.get("defaultPricelist")))
                        && CREDIT_POLICIES.contains(asString(sales.get("creditPolicy")))
                        && !arrayOfStrings(sales.get("paymentMethods")).isEmpty();
            }
            case "purchases" -> {
                Map<String, Object> purchases = session.getPurchasesSetup();
                yield hasText(asString(purchases.get("defaultTaxSupport")))
                        && SERVICE_RETENTION_DEFAULTS.contains(asString(purchases.get("serviceRetentionDefault")));
            }
            case "inventory" -> {
                Map<String, Object> inventory = session.getInventorySetup();
                yield boundedInteger(inventory.get("warehouseCount"), 1, 999, 0, null) > 0
                        && DISPATCH_POLICIES.contains(asString(inventory.get("dispatchPolicy")));
            }
            case "crm-hr-bi" -> REQUIRED_GROWTH_KEYS.stream().allMatch(key -> session.getGrowthSetup().containsKey(key));
            case "brand-studio" -> {
                Map<String, Object> brand = session.getBrandSetup();
                yield isHexColor(asString(brand.get("primaryColor")))
                        && isHexColor(asString(brand.get("accentColor")))
                        && RIDE_TEMPLATES.contains(asString(brand.get("template")))
                        && bool(brand.get("legalSafeMode"));
            }
            case "publish-review" -> "PUBLISHED".equalsIgnoreCase(session.getStatus());
            default -> false;
        };
    }

    private void syncTenantIdentity(TenantEntity tenant, OnboardingSessionEntity session) {
        Map<String, Object> legal = session.getLegalEntity();
        String legalName = trimToNull(asString(legal.get("legalName")));
        String tradeName = trimToNull(asString(legal.get("tradeName")));
        String currencyCode = trimToNull(asString(legal.get("currencyCode")));
        String timeZone = trimToNull(asString(legal.get("timeZone")));
        if (legalName != null) {
            tenant.setLegalName(legalName);
        }
        tenant.setTradeName(firstText(tradeName, legalName, tenant.getLegalName()));
        if (currencyCode != null) {
            tenant.setCurrencyCode(currencyCode.toUpperCase(Locale.ROOT));
        }
        if (timeZone != null) {
            tenant.setTimeZone(timeZone);
        }
    }

    private String publishLabel(String step) {
        return switch (step) {
            case "business-discovery" -> "Actividad del negocio";
            case "legal-entity" -> "Datos del RUC";
            case "tax-profile" -> "Perfil tributario";
            case "certificate-connectivity" -> "Firma electronica";
            case "emission-points" -> "Puntos de emision";
            case "accounting-template" -> "Configuracion contable";
            case "sales-pos" -> "Ventas y cobros";
            case "purchases" -> "Compras";
            case "inventory" -> "Inventario";
            case "brand-studio" -> "Diseno del comprobante";
            default -> step;
        };
    }

    private String firstAllowed(String value, Set<String> allowed, String fallback) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return fallback;
        }
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private int boundedInteger(Object value, int min, int max, int fallback, String errorMessage) {
        String text = trimToNull(asString(value));
        if (text == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(text);
            if (parsed < min || parsed > max) {
                if (errorMessage != null) {
                    throw new BadRequestException(errorMessage);
                }
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            if (errorMessage != null) {
                throw new BadRequestException(errorMessage);
            }
            return fallback;
        }
    }

    private String normalizeDigits(Object value, int digits, String errorMessage) {
        String text = trimToNull(asString(value));
        if (text == null) {
            return "";
        }
        String numeric = text.replaceAll("\\D", "");
        if (numeric.length() > digits) {
            throw new BadRequestException(errorMessage);
        }
        return "0".repeat(digits - numeric.length()) + numeric;
    }

    private List<String> orderedSelections(Object value, Set<String> allowed, List<String> order) {
        Set<String> selected = arrayOfStrings(value).stream()
                .filter(allowed::contains)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return order.stream().filter(selected::contains).toList();
    }

    private List<String> arrayOfStrings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> Objects.toString(item, "").trim())
                    .filter(this::hasText)
                    .toList();
        }
        if (value instanceof String stringValue) {
            return List.of(stringValue.split(",")).stream()
                    .map(String::trim)
                    .filter(this::hasText)
                    .toList();
        }
        return List.of();
    }

    private String normalizeColor(Object value, String fallback) {
        String color = trimToNull(asString(value));
        if (color == null) {
            return fallback;
        }
        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new BadRequestException("El color debe tener formato hexadecimal #RRGGBB");
        }
        return color.toUpperCase(Locale.ROOT);
    }

    private void putTrimmed(Map<String, Object> target, String key, Object rawValue) {
        target.put(key, trimToNull(asString(rawValue)));
    }

    private boolean validThreeDigits(Object value) {
        return THREE_DIGIT_PATTERN.matcher(Objects.toString(value, "")).matches();
    }

    private boolean validNineDigits(Object value) {
        return NINE_DIGIT_PATTERN.matcher(Objects.toString(value, "")).matches();
    }

    private boolean isHexColor(String value) {
        return hasText(value) && HEX_COLOR_PATTERN.matcher(value).matches();
    }

    private boolean isValidEcuadorRuc(String ruc) {
        String value = trimToNull(ruc);
        if (value == null || !RUC_PATTERN.matcher(value).matches()) {
            return false;
        }
        int province = Integer.parseInt(value.substring(0, 2));
        if ((province < 1 || province > 24) && province != 30) {
            return false;
        }
        int thirdDigit = value.charAt(2) - '0';
        if (thirdDigit < 6) {
            return isValidNaturalPersonRuc(value);
        }
        if (thirdDigit == 6) {
            return isValidPublicEntityRuc(value);
        }
        if (thirdDigit == 9) {
            return isValidPrivateEntityRuc(value);
        }
        return false;
    }

    private boolean isValidNaturalPersonRuc(String ruc) {
        int[] factors = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;
        for (int index = 0; index < factors.length; index++) {
            int product = (ruc.charAt(index) - '0') * factors[index];
            sum += product >= 10 ? product - 9 : product;
        }
        int verifier = (10 - (sum % 10)) % 10;
        return verifier == (ruc.charAt(9) - '0') && !ruc.substring(10).equals("000");
    }

    private boolean isValidPublicEntityRuc(String ruc) {
        int[] factors = {3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < factors.length; index++) {
            sum += (ruc.charAt(index) - '0') * factors[index];
        }
        int verifier = 11 - (sum % 11);
        if (verifier == 11) {
            verifier = 0;
        } else if (verifier == 10) {
            verifier = 1;
        }
        return verifier == (ruc.charAt(8) - '0') && !ruc.substring(9).equals("0000");
    }

    private boolean isValidPrivateEntityRuc(String ruc) {
        int[] factors = {4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < factors.length; index++) {
            sum += (ruc.charAt(index) - '0') * factors[index];
        }
        int verifier = 11 - (sum % 11);
        if (verifier == 11) {
            verifier = 0;
        } else if (verifier == 10) {
            verifier = 1;
        }
        return verifier == (ruc.charAt(9) - '0') && !ruc.substring(10).equals("000");
    }

    private void validateStep(String step) {
        if (!WIZARD_STEPS.contains(step)) {
            throw new BadRequestException("Paso de onboarding no soportado: " + step);
        }
    }

    private List<CatalogItemView> items(String catalogType) {
        return catalogItemRepository.findByCatalogTypeAndActiveTrueOrderByItemCodeAsc(catalogType).stream()
                .map(this::toCatalogItem)
                .toList();
    }

    private CatalogItemView toCatalogItem(OnboardingCatalogItemEntity item) {
        return new CatalogItemView(
                item.getCatalogType(),
                item.getItemCode(),
                item.getLabel(),
                item.getPayload() == null ? Map.of() : item.getPayload());
    }

    private OnboardingSessionView toView(TenantEntity tenant, OnboardingSessionEntity session) {
        return new OnboardingSessionView(
                session.getId(),
                tenant.getId(),
                tenant.getTenantCode(),
                tenant.getLegalName(),
                session.getStatus(),
                session.getCurrentStep(),
                session.getProgressPercent(),
                session.getVerticalTemplate(),
                copy(session.getBusinessProfile()),
                copy(session.getLegalEntity()),
                copy(session.getTaxProfile()),
                copy(session.getCertificateStatus()),
                copy(session.getEmissionSetup()),
                copy(session.getAccountingSetup()),
                copy(session.getSalesSetup()),
                copy(session.getPurchasesSetup()),
                copy(session.getInventorySetup()),
                copy(session.getGrowthSetup()),
                copy(session.getBrandSetup()),
                copy(session.getPermissionsSetup()),
                copy(session.getChecklist()),
                session.getWarnings() == null ? List.of() : session.getWarnings(),
                session.getPublishedAt(),
                session.getUpdatedAt());
    }

    private void appendAudit(Long tenantId, Long sessionId, String eventType, Map<String, Object> payload) {
        OnboardingAuditEventEntity audit = new OnboardingAuditEventEntity();
        audit.setTenantId(tenantId);
        audit.setSessionId(sessionId);
        audit.setEventType(eventType);
        audit.setActor(currentActor());
        audit.setPayload(new LinkedHashMap<>(payload));
        auditEventRepository.save(audit);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "system";
        }
        return authentication.getName();
    }

    private Map<String, Object> warning(String step, String title, String detail) {
        return new LinkedHashMap<>(Map.of(
                "step", step,
                "title", title,
                "detail", detail));
    }

    private Map<String, Object> mutableMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeWizardEnvironment(String value) {
        if ("2".equals(value) || "production".equalsIgnoreCase(value) || "prod".equalsIgnoreCase(value)) {
            return "2";
        }
        return "1";
    }

    private String sanitizeFileName(String originalName) {
        String value = hasText(originalName) ? originalName.trim() : "archivo";
        return value.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private boolean validRideReferenceFile(String fileName, String contentType) {
        String lowerName = Objects.toString(fileName, "").toLowerCase();
        boolean extensionOk = RIDE_REFERENCE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!extensionOk) {
            return false;
        }
        String normalizedContentType = Objects.toString(contentType, "").toLowerCase();
        return !hasText(normalizedContentType) || RIDE_REFERENCE_CONTENT_TYPES.contains(normalizedContentType);
    }

    public record OnboardingSessionView(
            Long id,
            Long tenantId,
            String tenantCode,
            String tenantName,
            String status,
            String currentStep,
            Integer progressPercent,
            String verticalTemplate,
            Map<String, Object> businessProfile,
            Map<String, Object> legalEntity,
            Map<String, Object> taxProfile,
            Map<String, Object> certificateStatus,
            Map<String, Object> emissionSetup,
            Map<String, Object> accountingSetup,
            Map<String, Object> salesSetup,
            Map<String, Object> purchasesSetup,
            Map<String, Object> inventorySetup,
            Map<String, Object> growthSetup,
            Map<String, Object> brandSetup,
            Map<String, Object> permissionsSetup,
            Map<String, Object> checklist,
            List<Map<String, Object>> warnings,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }

    public record CatalogItemView(
            String catalogType,
            String itemCode,
            String label,
            Map<String, Object> payload) {
    }

    public record OnboardingCatalogView(
            List<CatalogItemView> documentTypes,
            List<CatalogItemView> paymentMethods,
            List<CatalogItemView> verticalTemplates,
            List<CatalogItemView> fiscalFlags) {
    }

    public record RucValidationView(boolean valid, String message) {
    }
}

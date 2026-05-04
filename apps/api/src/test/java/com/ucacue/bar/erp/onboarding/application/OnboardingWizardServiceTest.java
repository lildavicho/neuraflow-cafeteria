package com.ucacue.bar.erp.onboarding.application;

import com.ucacue.bar.erp.accounting.application.SriDocumentService;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.TenantRepository;
import com.ucacue.bar.erp.onboarding.application.OnboardingWizardService.OnboardingSessionView;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.entity.OnboardingSessionEntity;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingAuditEventRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingCatalogItemRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingReferenceFileRepository;
import com.ucacue.bar.erp.onboarding.infrastructure.persistence.repository.OnboardingSessionRepository;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingWizardServiceTest {

    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private TenantRepository tenantRepository;
    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private OnboardingAuditEventRepository auditEventRepository;
    @Mock private OnboardingCatalogItemRepository catalogItemRepository;
    @Mock private OnboardingReferenceFileRepository referenceFileRepository;
    @Mock private SriDocumentService sriDocumentService;

    private OnboardingWizardService onboardingWizardService;
    private TenantEntity tenant;
    private OnboardingSessionEntity session;

    @BeforeEach
    void setUp() {
        onboardingWizardService = new OnboardingWizardService(
                tenantContextResolver,
                tenantRepository,
                sessionRepository,
                auditEventRepository,
                catalogItemRepository,
                referenceFileRepository,
                sriDocumentService
        );

        tenant = new TenantEntity();
        tenant.setId(1L);
        tenant.setTenantCode("default");
        tenant.setLegalName("Tenant Default");
        tenant.setTradeName("Tenant Default");
        tenant.setCurrencyCode("USD");
        tenant.setTimeZone("America/Guayaquil");

        session = new OnboardingSessionEntity();
        session.setId(10L);
        session.setTenantId(1L);
        session.setBusinessProfile(new LinkedHashMap<>());
        session.setLegalEntity(new LinkedHashMap<>());
        session.setTaxProfile(new LinkedHashMap<>());
        session.setCertificateStatus(new LinkedHashMap<>());
        session.setEmissionSetup(new LinkedHashMap<>());
        session.setAccountingSetup(new LinkedHashMap<>());
        session.setSalesSetup(new LinkedHashMap<>());
        session.setPurchasesSetup(new LinkedHashMap<>());
        session.setInventorySetup(new LinkedHashMap<>());
        session.setGrowthSetup(new LinkedHashMap<>());
        session.setBrandSetup(new LinkedHashMap<>());
        session.setPermissionsSetup(new LinkedHashMap<>());
        session.setChecklist(new LinkedHashMap<>());
        session.setWarnings(List.of());
    }

    @Test
    void validateRuc_usesEcuadorChecksumRules() {
        assertThat(onboardingWizardService.validateRuc("0102030400001").valid()).isTrue();
        assertThat(onboardingWizardService.validateRuc("0102030401001").valid()).isFalse();
        assertThat(onboardingWizardService.validateRuc("0102030400000").valid()).isFalse();
    }

    @Test
    void updateStep_syncsTenantIdentityButKeepsChecklistFalseWhenLegalDataIsIncomplete() {
        stubTenantSession();

        OnboardingSessionView view = onboardingWizardService.updateStep("default", "legal-entity", Map.of(
                "ruc", "0102030400001",
                "legalName", "Comercial InsightVision",
                "tradeName", "InsightVision Cuenca",
                "obligatedAccounting", "NO",
                "email", "facturas@insightvision.ec"
        ));

        assertThat(tenant.getLegalName()).isEqualTo("Comercial InsightVision");
        assertThat(tenant.getTradeName()).isEqualTo("InsightVision Cuenca");
        assertThat(view.checklist()).containsEntry("ruc-format", true);
        assertThat(view.checklist()).containsEntry("legal-entity", false);
    }

    @Test
    void updateStep_keepsTaxProfileIncompleteWhenSpecialTaxpayerResolutionIsMissing() {
        stubTenantSession();

        OnboardingSessionView view = onboardingWizardService.updateStep("default", "tax-profile", Map.of(
                "rimpe", false,
                "retentionAgent", false,
                "special_taxpayer", true
        ));

        assertThat(view.checklist()).containsEntry("tax-profile", false);
        assertThat(view.warnings()).anySatisfy(warning -> {
            assertThat(warning.get("step")).isEqualTo("tax-profile");
            assertThat(warning.get("title")).isEqualTo("Falta resolucion de contribuyente especial");
        });
    }

    @Test
    void publish_rejectsMissingInvoiceDocumentAndPaymentMethodsWithFriendlyLabels() {
        stubTenantSession();
        when(catalogItemRepository.findByCatalogTypeAndActiveTrueOrderByItemCodeAsc(any())).thenReturn(List.of());
        when(sriDocumentService.upsertConfiguration(any(), any())).thenReturn(
                new SriDocumentService.SriConfigurationView(
                        1L, "1", "1", "0102030400001", "Negocio Demo", "Negocio Demo",
                        "Cuenca", "Cuenca", null, "NO", false, false, null, true,
                        "PKCS12", "mi-firma", true, null, null
                )
        );

        onboardingWizardService.updateStep("default", "business-discovery", Map.of(
                "verticalTemplate", "food_restaurant",
                "businessMode", "b2c",
                "branches", 1,
                "usesPos", true
        ));
        onboardingWizardService.updateStep("default", "legal-entity", Map.of(
                "ruc", "0102030400001",
                "legalName", "Negocio Demo",
                "tradeName", "Negocio Demo",
                "matrixAddress", "Av. Loja y Remigio Crespo",
                "obligatedAccounting", "NO",
                "email", "demo@negocio.ec"
        ));
        onboardingWizardService.updateStep("default", "tax-profile", Map.of(
                "rimpe", false,
                "retentionAgent", false
        ));
        onboardingWizardService.uploadSriCertificate(
                "default",
                new MockMultipartFile("file", "firma.p12", "application/x-pkcs12", new byte[]{1, 2, 3}),
                "clave-segura",
                "mi-firma",
                "1"
        );
        onboardingWizardService.updateStep("default", "emission-points", Map.of(
                "establishmentCode", "001",
                "emissionPointCode", "001",
                "invoiceCurrentNumber", "000000001",
                "enabledDocuments", List.of("03")
        ));
        onboardingWizardService.updateStep("default", "accounting-template", Map.of(
                "coaTemplate", "EC_NIIF_BASE",
                "costingMethod", "AVCO",
                "dimensions", List.of("BRANCH")
        ));
        onboardingWizardService.updateStep("default", "sales-pos", Map.of(
                "defaultPricelist", "Precio publico",
                "creditPolicy", "cash-first",
                "paymentMethods", List.of()
        ));
        onboardingWizardService.updateStep("default", "purchases", Map.of(
                "defaultTaxSupport", "Compras locales",
                "serviceRetentionDefault", "70"
        ));
        onboardingWizardService.updateStep("default", "inventory", Map.of(
                "warehouseCount", 1,
                "dispatchPolicy", "stock-first"
        ));
        onboardingWizardService.updateStep("default", "brand-studio", Map.of(
                "primaryColor", "#4F46E5",
                "accentColor", "#8B5CF6",
                "template", "Classic",
                "legalSafeMode", true
        ));

        assertThatThrownBy(() -> onboardingWizardService.publish("default"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Comprobante 01 Factura")
                .hasMessageContaining("Formas de pago");
    }

    private void stubTenantSession() {
        when(tenantContextResolver.resolve("default")).thenReturn(tenant);
        when(sessionRepository.findByTenantId(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(OnboardingSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantRepository.save(any(TenantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}

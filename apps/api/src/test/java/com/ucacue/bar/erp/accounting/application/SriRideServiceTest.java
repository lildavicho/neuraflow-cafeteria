package com.ucacue.bar.erp.accounting.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.erp.accounting.application.SriRideService.RideTemplateView;
import com.ucacue.bar.erp.accounting.application.SriRideService.UpsertRideTemplateCommand;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.RideTemplateEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.RideTemplateRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.TenantEntity;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SriRideService — template save / load round-trip")
class SriRideServiceTest {

    private static final Long TENANT = 11L;
    private static final String TENANT_CODE = "default";

    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private TaxDocumentRepository taxDocumentRepository;
    @Mock private SriConfigurationRepository sriConfigurationRepository;
    @Mock private RideTemplateRepository rideTemplateRepository;
    @Mock private AuditService auditService;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private SriRideService service;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(TENANT);
        tenant.setTenantCode(TENANT_CODE);
        lenient().when(tenantContextResolver.resolve(TENANT_CODE)).thenReturn(tenant);

        lenient().when(rideTemplateRepository.save(any(RideTemplateEntity.class)))
                .thenAnswer(inv -> {
                    RideTemplateEntity e = inv.getArgument(0);
                    if (e.getId() == null) e.setId(77L);
                    return e;
                });
    }

    @Test
    @DisplayName("getCurrentTemplate returns InsightVision defaults when tenant has no stored template")
    void getCurrentTemplate_returnsDefaultsWhenMissing() {
        when(rideTemplateRepository.findFirstByTenantIdAndActiveTrueOrderByCustomDescUpdatedAtDesc(TENANT))
                .thenReturn(Optional.empty());

        RideTemplateView view = service.getCurrentTemplate(TENANT_CODE);

        assertThat(view.templateCode()).isEqualTo("DEFAULT");
        assertThat(view.templateName()).isEqualTo("InsightVision RIDE");
        assertThat(view.custom()).isFalse();
        assertThat(view.active()).isTrue();
        assertThat(view.layout())
                .containsEntry("headerColor", "#123a5a")
                .containsEntry("accentColor", "#0f766e")
                .containsEntry("borderColor", "#d9e2ec")
                .containsEntry("fontFamily", "Inter, sans-serif")
                .containsEntry("footerNote", "Documento generado por InsightVision");
        assertThat(view.htmlContent()).contains("{{cliente.nombre}}");
    }

    @Test
    @DisplayName("saveTemplate persists entity, merges layout, marks custom and clears cache for next read")
    void saveTemplate_persistsAndClearsCache() {
        when(rideTemplateRepository.findByTenantIdAndTemplateCodeIgnoreCase(TENANT, "DEFAULT"))
                .thenReturn(Optional.empty());

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("headerColor", "#236641");
        layout.put("fontFamily", "Cormorant Garamond, serif");
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "default", "Aurora Brand", layout,
                "<section class=\"ride-section\"><h2>{{cliente.nombre}}</h2></section>",
                Boolean.TRUE);

        RideTemplateView saved = service.saveTemplate(TENANT_CODE, cmd);

        ArgumentCaptor<RideTemplateEntity> captor = ArgumentCaptor.forClass(RideTemplateEntity.class);
        verify(rideTemplateRepository).save(captor.capture());
        RideTemplateEntity persisted = captor.getValue();
        assertThat(persisted.getTenantId()).isEqualTo(TENANT);
        assertThat(persisted.getTemplateCode()).isEqualTo("DEFAULT");
        assertThat(persisted.getTemplateName()).isEqualTo("Aurora Brand");
        assertThat(persisted.getCustom()).isTrue();
        assertThat(persisted.getActive()).isTrue();
        assertThat(persisted.getLayoutJson())
                .contains("\"headerColor\":\"#236641\"")
                .contains("\"fontFamily\":\"Cormorant Garamond, serif\"")
                .contains("\"accentColor\":\"#0f766e\"");
        assertThat(persisted.getHtmlContent()).contains("{{cliente.nombre}}");

        assertThat(saved.templateCode()).isEqualTo("DEFAULT");
        assertThat(saved.layout())
                .containsEntry("headerColor", "#236641")
                .containsEntry("fontFamily", "Cormorant Garamond, serif")
                .containsEntry("borderColor", "#d9e2ec");

        lenient().when(rideTemplateRepository.findFirstByTenantIdAndActiveTrueOrderByCustomDescUpdatedAtDesc(TENANT))
                .thenReturn(Optional.of(persisted));
        RideTemplateView reloaded = service.getCurrentTemplate(TENANT_CODE);
        assertThat(reloaded.layout()).containsEntry("headerColor", "#236641");
        verify(rideTemplateRepository).findFirstByTenantIdAndActiveTrueOrderByCustomDescUpdatedAtDesc(TENANT);
    }

    @Test
    @DisplayName("saveTemplate updates the existing row when one already exists for the tenant/code")
    void saveTemplate_updatesExistingRow() {
        RideTemplateEntity existing = new RideTemplateEntity();
        existing.setId(42L);
        existing.setTenantId(TENANT);
        existing.setTemplateCode("DEFAULT");
        existing.setTemplateName("Old name");
        existing.setLayoutJson("{\"headerColor\":\"#000000\"}");
        existing.setHtmlContent("<p>old</p>");
        existing.setCustom(true);
        existing.setActive(true);
        when(rideTemplateRepository.findByTenantIdAndTemplateCodeIgnoreCase(TENANT, "DEFAULT"))
                .thenReturn(Optional.of(existing));

        Map<String, Object> layout = new HashMap<>();
        layout.put("accentColor", "#0f766e");
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "default", "  ", layout, "  ", Boolean.FALSE);

        RideTemplateView view = service.saveTemplate(TENANT_CODE, cmd);

        ArgumentCaptor<RideTemplateEntity> captor = ArgumentCaptor.forClass(RideTemplateEntity.class);
        verify(rideTemplateRepository).save(captor.capture());
        RideTemplateEntity persisted = captor.getValue();
        assertThat(persisted.getId()).isEqualTo(42L);
        assertThat(persisted.getTemplateName()).isEqualTo("InsightVision RIDE");
        assertThat(persisted.getActive()).isFalse();
        assertThat(persisted.getHtmlContent()).isNull();
        assertThat(view.id()).isEqualTo(42L);
        assertThat(view.active()).isFalse();
    }

    @Test
    @DisplayName("saveTemplate rejects HTML with <script> tags")
    void saveTemplate_rejectsScriptTags() {
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "default", "Brand", Map.of(),
                "<div><script>alert('xss')</script></div>", true);

        assertThatThrownBy(() -> service.saveTemplate(TENANT_CODE, cmd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scripts");
    }

    @Test
    @DisplayName("saveTemplate rejects inline JavaScript handlers like onclick=")
    void saveTemplate_rejectsInlineHandlers() {
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "default", "Brand", Map.of(),
                "<button onclick=\"steal()\">x</button>", true);

        assertThatThrownBy(() -> service.saveTemplate(TENANT_CODE, cmd))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("saveTemplate normalizes the template code to upper-case ASCII")
    void saveTemplate_normalizesCode() {
        when(rideTemplateRepository.findByTenantIdAndTemplateCodeIgnoreCase(TENANT, "BRAND_AURORA"))
                .thenReturn(Optional.empty());

        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "  brand_aurora ", "Aurora", Map.of("borderColor", "#236641"), null, true);

        service.saveTemplate(TENANT_CODE, cmd);

        ArgumentCaptor<RideTemplateEntity> captor = ArgumentCaptor.forClass(RideTemplateEntity.class);
        verify(rideTemplateRepository).save(captor.capture());
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("BRAND_AURORA");
    }

    @Test
    @DisplayName("saveTemplate rejects template codes with illegal characters")
    void saveTemplate_rejectsIllegalCode() {
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "bad code!", "Brand", Map.of(), null, true);

        assertThatThrownBy(() -> service.saveTemplate(TENANT_CODE, cmd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("plantilla");
    }

    @Test
    @DisplayName("saveTemplate ignores unknown layout keys but keeps known overrides")
    void saveTemplate_ignoresUnknownLayoutKeys() {
        when(rideTemplateRepository.findByTenantIdAndTemplateCodeIgnoreCase(TENANT, "DEFAULT"))
                .thenReturn(Optional.empty());

        Map<String, Object> layout = new HashMap<>();
        layout.put("headerColor", "#236641");
        layout.put("rogueKey", "<script>alert(1)</script>");
        UpsertRideTemplateCommand cmd = new UpsertRideTemplateCommand(
                "default", "Aurora", layout, null, true);

        RideTemplateView view = service.saveTemplate(TENANT_CODE, cmd);

        assertThat(view.layout())
                .containsEntry("headerColor", "#236641")
                .doesNotContainKey("rogueKey");
    }
}

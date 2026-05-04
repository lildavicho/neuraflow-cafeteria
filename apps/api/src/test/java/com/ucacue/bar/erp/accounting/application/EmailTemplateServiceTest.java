package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.EmailTemplateEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.EmailTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailTemplateServiceTest {

    private final EmailTemplateRepository repository = mock(EmailTemplateRepository.class);
    private final EmailTemplateService service = new EmailTemplateService(repository);

    @Test
    void usesTenantTemplateBeforeGlobalTemplate() {
        EmailTemplateEntity tenantTemplate = template(7L, "SRI_INVOICE_ISSUED", "Factura {{documentNumber}}", "<p>{{customerName}}</p>");
        when(repository.findFirstByTenantIdAndTemplateCodeAndActiveTrueOrderByVersionDesc(7L, "SRI_INVOICE_ISSUED"))
                .thenReturn(Optional.of(tenantTemplate));

        EmailTemplateService.RenderedEmailTemplate rendered = service.render(
                7L,
                "SRI_INVOICE_ISSUED",
                Map.of("documentNumber", "001-001-000000123", "customerName", "Ana"));

        assertThat(rendered.subject()).isEqualTo("Factura 001-001-000000123");
        assertThat(rendered.htmlBody()).contains("Ana");
    }

    @Test
    void fallsBackToGlobalTemplateAndEscapesHtmlVariables() {
        EmailTemplateEntity globalTemplate = template(null, "SRI_INVOICE_RESEND", "Reenvio {{documentNumber}}", "<p>{{customerName}}</p>");
        when(repository.findFirstByTenantIdAndTemplateCodeAndActiveTrueOrderByVersionDesc(7L, "SRI_INVOICE_RESEND"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByTenantIdIsNullAndTemplateCodeAndActiveTrueOrderByVersionDesc("SRI_INVOICE_RESEND"))
                .thenReturn(Optional.of(globalTemplate));

        EmailTemplateService.RenderedEmailTemplate rendered = service.render(
                7L,
                "SRI_INVOICE_RESEND",
                Map.of("documentNumber", "123", "customerName", "<script>"));

        assertThat(rendered.subject()).isEqualTo("Reenvio 123");
        assertThat(rendered.htmlBody()).contains("&lt;script&gt;");
    }

    private EmailTemplateEntity template(Long tenantId, String code, String subject, String html) {
        EmailTemplateEntity entity = new EmailTemplateEntity();
        entity.setTenantId(tenantId);
        entity.setTemplateCode(code);
        entity.setVersion(1);
        entity.setSubject(subject);
        entity.setHtmlBody(html);
        entity.setActive(true);
        return entity;
    }
}

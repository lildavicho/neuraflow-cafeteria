package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.EmailTemplateEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.EmailTemplateRepository;
import com.ucacue.bar.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    private final EmailTemplateRepository emailTemplateRepository;

    public RenderedEmailTemplate render(Long tenantId, String templateCode, Map<String, ?> variables) {
        EmailTemplateEntity template = emailTemplateRepository
                .findFirstByTenantIdAndTemplateCodeAndActiveTrueOrderByVersionDesc(tenantId, templateCode)
                .or(() -> emailTemplateRepository
                        .findFirstByTenantIdIsNullAndTemplateCodeAndActiveTrueOrderByVersionDesc(templateCode))
                .orElseThrow(() -> new BadRequestException("Plantilla de correo no configurada: " + templateCode));

        String subject = renderText(template.getSubject(), variables, false);
        String html = renderText(template.getHtmlBody(), variables, true);
        if (subject.isBlank() || html.isBlank()) {
            throw new BadRequestException("La plantilla de correo produce asunto o cuerpo vacio");
        }
        return new RenderedEmailTemplate(template.getTemplateCode(), template.getVersion(), subject, html);
    }

    private String renderText(String template, Map<String, ?> variables, boolean html) {
        if (template == null) {
            return "";
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables == null ? null : variables.get(key);
            String replacement = value == null ? "" : String.valueOf(value);
            if (html) {
                replacement = escapeHtml(replacement);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record RenderedEmailTemplate(
            String templateCode,
            int version,
            String subject,
            String htmlBody) {
    }
}

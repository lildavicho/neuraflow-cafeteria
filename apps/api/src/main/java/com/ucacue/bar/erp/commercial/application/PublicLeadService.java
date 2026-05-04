package com.ucacue.bar.erp.commercial.application;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.email.ResendEmailClient;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.entity.PublicLeadEntity;
import com.ucacue.bar.erp.commercial.infrastructure.persistence.repository.PublicLeadRepository;
import com.ucacue.bar.erp.commercial.interfaces.http.PublicLeadRequest;
import com.ucacue.bar.erp.commercial.interfaces.http.PublicLeadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicLeadService {

    private final PublicLeadRepository publicLeadRepository;
    private final ResendEmailClient resendEmailClient;
    private final SriProperties sriProperties;

    @Value("${app.public-leads.notify-to:${PUBLIC_LEADS_NOTIFY_TO:soporte@insightvisionia.cloud}}")
    private String notifyTo;

    @Transactional
    public PublicLeadResponse create(PublicLeadRequest request) {
        PublicLeadEntity lead = new PublicLeadEntity();
        lead.setFullName(clean(request.fullName()));
        lead.setCompanyName(cleanNullable(request.companyName()));
        lead.setEmail(clean(request.email()).toLowerCase(Locale.ROOT));
        lead.setPhone(cleanNullable(request.phone()));
        lead.setCity(cleanNullable(request.city()));
        lead.setBusinessType(cleanNullable(request.businessType()));
        lead.setBranchCount(request.branchCount());
        lead.setInterest(cleanNullable(request.interest()));
        lead.setMessage(cleanNullable(request.message()));
        PublicLeadEntity saved = publicLeadRepository.save(lead);
        notifyCommercialTeam(saved);
        return new PublicLeadResponse(saved.getId(), saved.getStatus(), "Solicitud recibida");
    }

    private void notifyCommercialTeam(PublicLeadEntity lead) {
        if (!resendEmailClient.isReady() || notifyTo == null || notifyTo.isBlank()) {
            return;
        }
        try {
            resendEmailClient.send(new ResendEmailClient.SendRequest(
                    notifyTo.trim(),
                    "Nuevo lead comercial - Insight Vision IA",
                    leadHtml(lead),
                    sriProperties.getRide().getResend().getFromEmail(),
                    sriProperties.getRide().getResend().getReplyTo(),
                    sriProperties.getRide().getResend().getFromName(),
                    List.of()));
        } catch (Exception ex) {
            log.warn("No se pudo notificar lead publico id={} por correo: {}", lead.getId(), ex.getMessage());
        }
    }

    private String leadHtml(PublicLeadEntity lead) {
        return """
                <h2>Nuevo lead comercial</h2>
                <p><strong>Nombre:</strong> %s</p>
                <p><strong>Empresa:</strong> %s</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Telefono:</strong> %s</p>
                <p><strong>Ciudad:</strong> %s</p>
                <p><strong>Tipo de negocio:</strong> %s</p>
                <p><strong>Sucursales:</strong> %s</p>
                <p><strong>Interes:</strong> %s</p>
                <p><strong>Mensaje:</strong><br>%s</p>
                """.formatted(
                html(lead.getFullName()),
                html(lead.getCompanyName()),
                html(lead.getEmail()),
                html(lead.getPhone()),
                html(lead.getCity()),
                html(lead.getBusinessType()),
                lead.getBranchCount() != null ? lead.getBranchCount() : "",
                html(lead.getInterest()),
                html(lead.getMessage()).replace("\n", "<br>"));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String html(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

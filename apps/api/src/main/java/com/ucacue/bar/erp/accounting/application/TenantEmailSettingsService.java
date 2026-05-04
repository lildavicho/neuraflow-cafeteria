package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TenantEmailSettingsEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TenantEmailSettingsRepository;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriCredentialCryptoService;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.service.EmailService;
import com.ucacue.bar.service.EmailService.EmailSendResult;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantEmailSettingsService {

    private final TenantEmailSettingsRepository tenantEmailSettingsRepository;
    private final SriCredentialCryptoService sriCredentialCryptoService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public EmailSettingsResponse get(Long tenantId) {
        return tenantEmailSettingsRepository.findByTenantIdAndActiveTrue(tenantId)
                .map(EmailSettingsResponse::from)
                .orElseGet(() -> new EmailSettingsResponse(null, null, null, null, null, null, null, true));
    }

    @Transactional
    public EmailSettingsResponse update(Long tenantId, UpdateEmailSettingsRequest request) {
        TenantEmailSettingsEntity settings = tenantEmailSettingsRepository.findByTenantIdAndActiveTrue(tenantId)
                .orElseGet(TenantEmailSettingsEntity::new);
        settings.setTenantId(tenantId);
        settings.setFromName(blankToNull(request.fromName()));
        settings.setFromEmail(validEmailOrNull(request.fromEmail(), "fromEmail"));
        settings.setReplyToEmail(validEmailOrNull(request.replyToEmail(), "replyToEmail"));
        settings.setSmtpHost(blankToNull(request.smtpHost()));
        settings.setSmtpPort(request.smtpPort() != null ? request.smtpPort() : 587);
        settings.setSmtpUsername(blankToNull(request.smtpUsername()));
        settings.setSmtpAuth(request.smtpAuth() == null || request.smtpAuth());
        settings.setSmtpStartTls(request.smtpStartTls() == null || request.smtpStartTls());
        settings.setActive(request.active() == null || request.active());
        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) {
            String salt = sriCredentialCryptoService.generateSalt();
            settings.setSmtpEncryptionSalt(salt);
            settings.setSmtpPasswordEncrypted(sriCredentialCryptoService.encrypt(request.smtpPassword(), salt));
        }
        return EmailSettingsResponse.from(tenantEmailSettingsRepository.save(settings));
    }

    public EmailTestResponse test(Long tenantId, TestEmailRequest request, String authenticatedEmail) {
        String toEmail = validEmailOrNull(firstNonBlank(request.toEmail(), authenticatedEmail), "toEmail");
        EmailSendResult result = emailService.sendSriDocument(
                tenantId,
                toEmail,
                "Prueba de correo Insight Vision IA",
                "<p>Correo de prueba de facturacion electronica.</p>",
                java.util.List.of());
        return new EmailTestResponse(result.success(), result.provider(), result.status(), result.errorCode(), result.errorMessage());
    }

    private String validEmailOrNull(String email, String field) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            InternetAddress address = new InternetAddress(email.trim(), true);
            address.validate();
            return email.trim();
        } catch (Exception ex) {
            throw new BadRequestException("Correo invalido en " + field);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record EmailSettingsResponse(
            Long id,
            String fromName,
            String fromEmail,
            String replyToEmail,
            String smtpHost,
            Integer smtpPort,
            String smtpUsername,
            Boolean active) {

        private static EmailSettingsResponse from(TenantEmailSettingsEntity settings) {
            return new EmailSettingsResponse(
                    settings.getId(),
                    settings.getFromName(),
                    settings.getFromEmail(),
                    settings.getReplyToEmail(),
                    settings.getSmtpHost(),
                    settings.getSmtpPort(),
                    settings.getSmtpUsername(),
                    settings.getActive());
        }
    }

    public record UpdateEmailSettingsRequest(
            String fromName,
            String fromEmail,
            String replyToEmail,
            String smtpHost,
            Integer smtpPort,
            String smtpUsername,
            String smtpPassword,
            Boolean smtpAuth,
            Boolean smtpStartTls,
            Boolean active) {
    }

    public record TestEmailRequest(String toEmail) {
    }

    public record EmailTestResponse(
            boolean success,
            String provider,
            String status,
            String errorCode,
            String errorMessage) {
    }
}

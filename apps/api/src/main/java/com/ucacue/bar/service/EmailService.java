package com.ucacue.bar.service;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.email.ResendEmailClient;
import com.ucacue.bar.erp.accounting.infrastructure.email.ResendSendResult;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TenantEmailSettingsEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TenantEmailSettingsRepository;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriCredentialCryptoService;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> defaultMailSender;
    private final TenantEmailSettingsRepository tenantEmailSettingsRepository;
    private final SriCredentialCryptoService sriCredentialCryptoService;
    private final SriProperties sriProperties;
    private final ResendEmailClient resendEmailClient;
    private final ConcurrentMap<Long, CachedSender> tenantSenders = new ConcurrentHashMap<>();

    public EmailService(ObjectProvider<JavaMailSender> defaultMailSender,
                        TenantEmailSettingsRepository tenantEmailSettingsRepository,
                        SriCredentialCryptoService sriCredentialCryptoService,
                        SriProperties sriProperties,
                        ResendEmailClient resendEmailClient) {
        this.defaultMailSender = defaultMailSender;
        this.tenantEmailSettingsRepository = tenantEmailSettingsRepository;
        this.sriCredentialCryptoService = sriCredentialCryptoService;
        this.sriProperties = sriProperties;
        this.resendEmailClient = resendEmailClient;
    }

    public void send(String to, String subject, String body) {
        int size = body != null ? body.length() : 0;
        log.debug("Email send requested to={} subject={} bodySize={}", to, subject, size);
    }

    public EmailSendResult sendSriDocument(Long tenantId,
                                           String to,
                                           String subject,
                                           String htmlBody,
                                           List<EmailAttachment> attachments) {
        AttachmentSummary attachmentSummary = summarizeAttachments(attachments);
        if (to == null || to.isBlank()) {
            return failed("NONE", null, null, null, to, subject, "NO_CUSTOMER_EMAIL",
                    "El cliente no tiene correo registrado", false, attachmentSummary);
        }
        if (!isValidEmail(to)) {
            return failed("NONE", null, null, null, to, subject, "INVALID_RECIPIENT",
                    "El correo del cliente no es valido", false, attachmentSummary);
        }
        if (!sriProperties.getRide().isEmailEnabled()) {
            return failed("NONE", null, null, null, to, subject, "DISABLED",
                    "Envio de RIDE por correo deshabilitado", false, attachmentSummary);
        }
        if (!attachmentSummary.allowed()) {
            return failed("NONE", null, null, null, to, subject, "ATTACHMENT_LIMIT_EXCEEDED",
                    "Los adjuntos exceden el limite configurado", false, attachmentSummary);
        }

        TenantEmailSettingsEntity tenantSettings = tenantEmailSettingsRepository
                .findByTenantIdAndActiveTrue(tenantId)
                .orElse(null);
        EmailIdentity from = resolveFrom(tenantSettings);
        EmailIdentity replyTo = resolveReplyTo(tenantSettings);

        if (resendEmailClient.isReady()) {
            ResendSendResult result = resendEmailClient.send(new ResendEmailClient.SendRequest(
                    to.trim(),
                    subject,
                    htmlBody,
                    from != null ? from.address() : sriProperties.getRide().getResend().getFromEmail(),
                    replyTo != null ? replyTo.address() : sriProperties.getRide().getResend().getReplyTo(),
                    from != null ? from.name() : sriProperties.getRide().getResend().getFromName(),
                    attachments));
            if (result.success() || !result.retryable()) {
                return new EmailSendResult(
                        result.success(),
                        result.provider(),
                        result.messageId(),
                        from != null ? from.address() : sriProperties.getRide().getResend().getFromEmail(),
                        from != null ? from.name() : sriProperties.getRide().getResend().getFromName(),
                        replyTo != null ? replyTo.address() : sriProperties.getRide().getResend().getReplyTo(),
                        to.trim(),
                        subject,
                        result.success() ? "SENT" : "FAILED",
                        result.errorCode(),
                        result.errorMessage(),
                        result.retryable(),
                        result.sentAt(),
                        attachmentSummary.totalBytes(),
                        attachmentSummary.count());
            }
            log.warn("Resend retryable failure tenant={} to={}; attempting SMTP fallback if configured", tenantId, to);
        }
        JavaMailSender sender;
        try {
            sender = !sriProperties.getRide().isCentralRelayEnabled() && tenantSettings != null && hasTenantSmtp(tenantSettings)
                    ? resolveTenantSender(tenantId, tenantSettings)
                    : defaultMailSender.getIfAvailable();
        } catch (Exception ex) {
            tenantSenders.remove(tenantId);
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_CONFIG_FAILED", friendlyMailError(ex), false,
                    attachmentSummary);
        }
        if (sender == null) {
            log.info("SRI RIDE email skipped: no JavaMailSender configured for tenant={}", tenantId);
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_NOT_CONFIGURED", "No hay SMTP configurado",
                    false, attachmentSummary);
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (from != null && !isBlank(from.address())) {
                helper.setFrom(new InternetAddress(
                        from.address(),
                        firstNonBlank(from.name(), sriProperties.getRide().getDefaultSenderName(), "InsightVision Facturas"),
                        StandardCharsets.UTF_8.name()));
            }
            if (replyTo != null && !isBlank(replyTo.address())) {
                message.setReplyTo(new Address[] {
                        new InternetAddress(
                                replyTo.address(),
                                firstNonBlank(replyTo.name(), "Facturacion"),
                                StandardCharsets.UTF_8.name())
                });
            }
            if (attachments != null) {
                for (EmailAttachment attachment : attachments) {
                    if (attachment != null && attachment.content() != null && attachment.content().length > 0) {
                        helper.addAttachment(
                                attachment.fileName(),
                                new ByteArrayResource(attachment.content()),
                                attachment.contentType());
                    }
                }
            }
            sender.send(message);
            return new EmailSendResult(true, "SMTP", null,
                    from != null ? from.address() : null,
                    from != null ? from.name() : null,
                    replyTo != null ? replyTo.address() : null,
                    to.trim(), subject, "SENT", null, null, false, Instant.now(),
                    attachmentSummary.totalBytes(), attachmentSummary.count());
        } catch (MailAuthenticationException ex) {
            tenantSenders.remove(tenantId);
            log.warn("SRI RIDE email authentication failed tenant={} to={}", tenantId, to);
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_AUTH_FAILED",
                    "No se pudo autenticar el correo del negocio. Revisa usuario y contrasena SMTP.", false,
                    attachmentSummary);
        } catch (MailSendException ex) {
            tenantSenders.remove(tenantId);
            log.warn("SRI RIDE email send failed tenant={} to={} reason={}", tenantId, to, ex.getMessage());
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_SEND_FAILED", friendlyMailError(ex), true,
                    attachmentSummary);
        } catch (MailException ex) {
            tenantSenders.remove(tenantId);
            log.warn("SRI RIDE email failed tenant={} to={} reason={}", tenantId, to, ex.getMessage());
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_FAILED", friendlyMailError(ex),
                    isTemporary(ex), attachmentSummary);
        } catch (Exception ex) {
            tenantSenders.remove(tenantId);
            log.warn("SRI RIDE email failed tenant={} to={} reason={}", tenantId, to, ex.getMessage());
            return failed("SMTP", from, replyTo, null, to, subject, "SMTP_FAILED", friendlyMailError(ex),
                    isTemporary(ex), attachmentSummary);
        }
    }

    private boolean hasTenantSmtp(TenantEmailSettingsEntity settings) {
        return settings.getSmtpHost() != null && !settings.getSmtpHost().isBlank();
    }

    private JavaMailSender resolveTenantSender(Long tenantId, TenantEmailSettingsEntity settings) {
        CachedSender cached = tenantSenders.get(tenantId);
        String cacheKey = cacheKey(settings);
        if (cached != null && cached.cacheKey().equals(cacheKey)) {
            return cached.sender();
        }
        JavaMailSender sender = buildTenantSender(settings);
        tenantSenders.put(tenantId, new CachedSender(cacheKey, sender));
        return sender;
    }

    private JavaMailSender buildTenantSender(TenantEmailSettingsEntity settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getSmtpHost().trim());
        sender.setPort(settings.getSmtpPort() != null ? settings.getSmtpPort() : 587);
        sender.setUsername(settings.getSmtpUsername());
        String password = sriCredentialCryptoService.decrypt(
                settings.getSmtpPasswordEncrypted(),
                settings.getSmtpEncryptionSalt());
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(settings.getSmtpAuth())));
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(settings.getSmtpStartTls())));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        return sender;
    }

    private String cacheKey(TenantEmailSettingsEntity settings) {
        return settings.getId() + "|"
                + settings.getUpdatedAt() + "|"
                + settings.getSmtpHost() + "|"
                + settings.getSmtpPort() + "|"
                + settings.getSmtpUsername();
    }

    private String friendlyMailError(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("timeout") || message.contains("timed out") || message.contains("connection")) {
            return "El servidor SMTP no respondio a tiempo. Se reintentara automaticamente.";
        }
        if (message.contains("auth") || message.contains("credential") || message.contains("password")) {
            return "No se pudo autenticar el SMTP del negocio. Revisa usuario y contrasena.";
        }
        if (message.contains("recipient") || message.contains("invalid address")) {
            return "El correo del cliente no es valido.";
        }
        return "No se pudo enviar el correo del RIDE. Revisa la configuracion SMTP.";
    }

    private boolean isTemporary(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return ex instanceof MailSendException
                || message.contains("timeout")
                || message.contains("timed out")
                || message.contains("connection")
                || message.contains("4.")
                || message.contains("5.");
    }

    private EmailIdentity resolveFrom(TenantEmailSettingsEntity settings) {
        String defaultSender = sriProperties.getRide().getDefaultSender();
        String defaultSenderName = firstNonBlank(sriProperties.getRide().getDefaultSenderName(), "Insight Vision IA");
        String tenantName = settings != null ? settings.getFromName() : null;
        if (sriProperties.getRide().isCentralRelayEnabled()) {
            String name = !isBlank(tenantName) ? "Facturas " + tenantName.trim() : defaultSenderName;
            return new EmailIdentity(defaultSender, name);
        }
        if (settings != null && settings.getFromEmail() != null && !settings.getFromEmail().isBlank()) {
            return new EmailIdentity(settings.getFromEmail().trim(), firstNonBlank(tenantName, defaultSenderName));
        }
        return new EmailIdentity(defaultSender, defaultSenderName);
    }

    private EmailIdentity resolveReplyTo(TenantEmailSettingsEntity settings) {
        String globalReplyTo = sriProperties.getRide().getResend().getReplyTo();
        if (settings == null) {
            return !isBlank(globalReplyTo) ? new EmailIdentity(globalReplyTo.trim(), "Soporte") : null;
        }
        if (settings.getReplyToEmail() != null && !settings.getReplyToEmail().isBlank()) {
            return new EmailIdentity(settings.getReplyToEmail().trim(), firstNonBlank(settings.getFromName(), "Facturacion"));
        }
        if (!isBlank(globalReplyTo)) {
            return new EmailIdentity(globalReplyTo.trim(), firstNonBlank(settings.getFromName(), "Facturacion"));
        }
        if (sriProperties.getRide().isCentralRelayEnabled()
                && settings.getFromEmail() != null
                && !settings.getFromEmail().isBlank()) {
            return new EmailIdentity(settings.getFromEmail().trim(), firstNonBlank(settings.getFromName(), "Facturacion"));
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email.trim(), true);
            address.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private AttachmentSummary summarizeAttachments(List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return new AttachmentSummary(0, 0L, true);
        }
        int count = 0;
        long totalBytes = 0L;
        long maxSingle = sriProperties.getRide().getResend().getMaxAttachmentBytes();
        long maxTotal = sriProperties.getRide().getResend().getMaxTotalAttachmentBytes();
        boolean allowed = true;
        for (EmailAttachment attachment : attachments) {
            if (attachment == null || attachment.content() == null) {
                continue;
            }
            count++;
            int size = attachment.content().length;
            totalBytes += size;
            if (size > maxSingle) {
                allowed = false;
            }
        }
        if (totalBytes > maxTotal || count > 2) {
            allowed = false;
        }
        return new AttachmentSummary(count, totalBytes, allowed);
    }

    private EmailSendResult failed(String provider,
                                   EmailIdentity from,
                                   EmailIdentity replyTo,
                                   String providerMessageId,
                                   String to,
                                   String subject,
                                   String errorCode,
                                   String errorMessage,
                                   boolean retryable,
                                   AttachmentSummary attachmentSummary) {
        return new EmailSendResult(false, provider, providerMessageId,
                from != null ? from.address() : null,
                from != null ? from.name() : null,
                replyTo != null ? replyTo.address() : null,
                to, subject, "FAILED", errorCode, errorMessage, retryable, null,
                attachmentSummary.totalBytes(), attachmentSummary.count());
    }

    public record EmailAttachment(
            String fileName,
            byte[] content,
            String contentType) {
    }

    public record EmailSendResult(
            boolean success,
            String provider,
            String providerMessageId,
            String fromEmail,
            String fromName,
            String replyToEmail,
            String toEmail,
            String subject,
            String status,
            String errorCode,
            String errorMessage,
            boolean retryable,
            Instant sentAt,
            long attachmentTotalBytes,
            int attachmentCount) {
    }

    private record CachedSender(
            String cacheKey,
            JavaMailSender sender) {
    }

    private record EmailIdentity(
            String address,
            String name) {
    }

    private record AttachmentSummary(
            int count,
            long totalBytes,
            boolean allowed) {
    }
}

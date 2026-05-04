package com.ucacue.bar.erp.accounting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_email_settings", indexes = {
        @Index(name = "uk_tenant_email_settings_tenant", columnList = "tenant_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class TenantEmailSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "from_name", length = 120)
    private String fromName;

    @Column(name = "from_email", length = 180)
    private String fromEmail;

    @Column(name = "reply_to_email", length = 180)
    private String replyToEmail;

    @Column(name = "smtp_host", length = 180)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort = 587;

    @Column(name = "smtp_username", length = 180)
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted", columnDefinition = "TEXT")
    private String smtpPasswordEncrypted;

    @Column(name = "smtp_encryption_salt", length = 80)
    private String smtpEncryptionSalt;

    @Column(name = "smtp_auth", nullable = false)
    private Boolean smtpAuth = Boolean.TRUE;

    @Column(name = "smtp_starttls", nullable = false)
    private Boolean smtpStartTls = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

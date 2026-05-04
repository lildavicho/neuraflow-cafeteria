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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_sri_settings", indexes = {
        @Index(name = "uk_tax_sri_settings_tenant", columnList = "tenant_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class SriConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "environment_code", nullable = false, length = 10)
    private String environmentCode = "1";

    @Column(name = "emission_code", nullable = false, length = 10)
    private String emissionCode = "1";

    @Column(name = "issuer_ruc", nullable = false, length = 13)
    private String issuerRuc;

    @Column(name = "issuer_legal_name", nullable = false, length = 300)
    private String issuerLegalName;

    @Column(name = "issuer_trade_name", length = 300)
    private String issuerTradeName;

    @Column(name = "matrix_address", nullable = false, length = 300)
    private String matrixAddress;

    @Column(name = "establishment_address", nullable = false, length = 300)
    private String establishmentAddress;

    @Column(name = "special_taxpayer", length = 20)
    private String specialTaxpayer;

    @Column(name = "obligated_accounting", nullable = false, length = 2)
    private String obligatedAccounting = "NO";

    @Column(name = "rimpe_contributor", nullable = false)
    private Boolean rimpeContributor = Boolean.FALSE;

    @Column(name = "retention_agent", nullable = false)
    private Boolean retentionAgent = Boolean.FALSE;

    @Column(name = "retention_agent_resolution", length = 80)
    private String retentionAgentResolution;

    @Column(name = "signature_mode", nullable = false, length = 20)
    private String signatureMode = "NONE";

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "p12_content", length = 1_048_576)
    private byte[] p12Content;

    @Column(name = "p12_password_encrypted", columnDefinition = "TEXT")
    private String p12PasswordEncrypted;

    @Column(name = "sri_encryption_salt", length = 80)
    private String sriEncryptionSalt;

    @Column(name = "p12_key_alias", length = 120)
    private String p12KeyAlias;

    @Column(name = "signature_updated_at")
    private LocalDateTime signatureUpdatedAt;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

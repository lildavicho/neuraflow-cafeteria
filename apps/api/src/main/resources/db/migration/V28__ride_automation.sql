ALTER TABLE tax_documents
    ADD COLUMN ride_pdf_path VARCHAR(500) NULL,
    ADD COLUMN ride_pdf_url VARCHAR(500) NULL,
    ADD COLUMN ride_pdf_generated_at DATETIME NULL,
    ADD COLUMN ride_email_status VARCHAR(30) NULL,
    ADD COLUMN ride_email_sent_at DATETIME NULL,
    ADD COLUMN ride_email_error TEXT NULL;

ALTER TABLE tax_sri_settings
    ADD COLUMN retention_agent TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN retention_agent_resolution VARCHAR(80) NULL;

CREATE TABLE IF NOT EXISTS tenant_email_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    from_name VARCHAR(120) NULL,
    from_email VARCHAR(180) NULL,
    reply_to_email VARCHAR(180) NULL,
    smtp_host VARCHAR(180) NULL,
    smtp_port INT NULL DEFAULT 587,
    smtp_username VARCHAR(180) NULL,
    smtp_password_encrypted TEXT NULL,
    smtp_encryption_salt VARCHAR(80) NULL,
    smtp_auth TINYINT(1) NOT NULL DEFAULT 1,
    smtp_starttls TINYINT(1) NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_email_settings_tenant
        FOREIGN KEY (tenant_id) REFERENCES erp_tenants(id),
    UNIQUE KEY uk_tenant_email_settings_tenant (tenant_id)
) ENGINE=InnoDB;

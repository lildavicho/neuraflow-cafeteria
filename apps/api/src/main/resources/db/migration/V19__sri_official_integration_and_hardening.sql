ALTER TABLE tax_documents
    ADD COLUMN sent_at DATETIME NULL AFTER issue_date,
    ADD COLUMN last_status_at DATETIME NULL AFTER sent_at,
    ADD COLUMN reception_status VARCHAR(40) NULL AFTER status,
    ADD COLUMN authorization_status VARCHAR(40) NULL AFTER reception_status,
    ADD COLUMN signed_xml_payload LONGTEXT NULL AFTER xml_payload,
    ADD COLUMN last_provider_message TEXT NULL AFTER validation_errors;

UPDATE tax_documents
SET last_status_at = COALESCE(authorized_at, created_at)
WHERE last_status_at IS NULL;

CREATE TABLE tax_sri_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    environment_code VARCHAR(10) NOT NULL DEFAULT '1',
    emission_code VARCHAR(10) NOT NULL DEFAULT '1',
    issuer_ruc VARCHAR(13) NOT NULL,
    issuer_legal_name VARCHAR(300) NOT NULL,
    issuer_trade_name VARCHAR(300) NULL,
    matrix_address VARCHAR(300) NOT NULL,
    establishment_address VARCHAR(300) NOT NULL,
    special_taxpayer VARCHAR(20) NULL,
    obligated_accounting VARCHAR(2) NOT NULL DEFAULT 'NO',
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_sri_settings_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants(id),
    UNIQUE KEY uk_tax_sri_settings_tenant (tenant_id)
) ENGINE=InnoDB;

CREATE TABLE tax_document_transmissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tax_document_id BIGINT NOT NULL,
    phase VARCHAR(20) NOT NULL,
    endpoint_url VARCHAR(255) NOT NULL,
    request_payload LONGTEXT NULL,
    response_payload LONGTEXT NULL,
    provider_status VARCHAR(60) NULL,
    provider_message TEXT NULL,
    http_status INT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    CONSTRAINT fk_tax_document_transmissions_document
        FOREIGN KEY (tax_document_id) REFERENCES tax_documents(id) ON DELETE CASCADE,
    INDEX idx_tax_document_transmissions_document_phase (tax_document_id, phase, attempted_at),
    INDEX idx_tax_document_transmissions_status (provider_status, success)
) ENGINE=InnoDB;

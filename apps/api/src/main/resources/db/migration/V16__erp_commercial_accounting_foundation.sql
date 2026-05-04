CREATE TABLE IF NOT EXISTS erp_tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    legal_name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150) NULL,
    country_code VARCHAR(5) NOT NULL DEFAULT 'EC',
    time_zone VARCHAR(50) NOT NULL DEFAULT 'America/Guayaquil',
    currency_code VARCHAR(10) NOT NULL DEFAULT 'USD',
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_tenant_plan_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    commercial_plan VARCHAR(30) NOT NULL,
    subscription_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_erp_tenant_plan_subscriptions_tenant
        FOREIGN KEY (tenant_id) REFERENCES erp_tenants(id),
    INDEX idx_erp_tenant_plan_subscriptions_tenant (tenant_id),
    INDEX idx_erp_tenant_plan_subscriptions_status (subscription_status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS erp_tenant_feature_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    module_code VARCHAR(40) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    reason_text VARCHAR(255) NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_erp_tenant_feature_flags_tenant
        FOREIGN KEY (tenant_id) REFERENCES erp_tenants(id),
    UNIQUE KEY uk_erp_tenant_feature_flags_tenant_module (tenant_id, module_code),
    INDEX idx_erp_tenant_feature_flags_module (module_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_account_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_code VARCHAR(30) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    parent_account_id BIGINT NULL,
    account_type VARCHAR(20) NOT NULL,
    account_nature VARCHAR(20) NOT NULL,
    tree_level INT NOT NULL DEFAULT 1,
    allow_posting TINYINT(1) NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_account_catalog_tenant_code (tenant_id, account_code),
    INDEX idx_acc_account_catalog_parent (parent_account_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_tax_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tax_code VARCHAR(30) NOT NULL,
    tax_name VARCHAR(120) NOT NULL,
    tax_scope VARCHAR(20) NOT NULL,
    computation_type VARCHAR(20) NOT NULL,
    rate DECIMAL(8,4) NOT NULL DEFAULT 0,
    tax_authority_code VARCHAR(30) NULL,
    taxable_object_code VARCHAR(30) NULL,
    included_in_price TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    valid_from DATE NULL,
    valid_until DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_tax_rules_tenant_code (tenant_id, tax_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_document_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    document_code VARCHAR(30) NOT NULL,
    document_name VARCHAR(120) NOT NULL,
    fiscal_domain VARCHAR(20) NOT NULL,
    document_direction VARCHAR(20) NOT NULL,
    requires_sequence TINYINT(1) NOT NULL DEFAULT 1,
    requires_counterparty TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_document_types_tenant_code (tenant_id, document_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_document_sequences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    document_type_id BIGINT NOT NULL,
    establishment_code VARCHAR(10) NOT NULL,
    emission_point_code VARCHAR(10) NOT NULL,
    current_number BIGINT NOT NULL DEFAULT 0,
    increment_step INT NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_acc_document_sequences_document_type
        FOREIGN KEY (document_type_id) REFERENCES acc_document_types(id),
    UNIQUE KEY uk_acc_document_sequences_codes (tenant_id, document_type_id, establishment_code, emission_point_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_journal_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    entry_number VARCHAR(40) NOT NULL,
    entry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_module VARCHAR(20) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    external_reference VARCHAR(80) NULL,
    posted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_journal_entries_tenant_entry (tenant_id, entry_number),
    INDEX idx_acc_journal_entries_source (source_type, source_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_journal_entry_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    account_id BIGINT NOT NULL,
    tax_rule_id BIGINT NULL,
    debit DECIMAL(14,2) NOT NULL DEFAULT 0,
    credit DECIMAL(14,2) NOT NULL DEFAULT 0,
    description VARCHAR(255) NULL,
    reference_code VARCHAR(80) NULL,
    CONSTRAINT fk_acc_journal_entry_lines_entry
        FOREIGN KEY (entry_id) REFERENCES acc_journal_entries(id),
    INDEX idx_acc_journal_entry_lines_entry (entry_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_receivables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    source_document_type VARCHAR(40) NOT NULL,
    source_document_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NULL,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    balance DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    accounting_entry_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_acc_receivables_tenant_status (tenant_id, status),
    INDEX idx_acc_receivables_source (source_document_type, source_document_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_payables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    supplier_id BIGINT NULL,
    source_document_type VARCHAR(40) NOT NULL,
    source_document_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NULL,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    balance DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    accounting_entry_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_acc_payables_tenant_status (tenant_id, status),
    INDEX idx_acc_payables_source (source_document_type, source_document_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tax_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    source_module VARCHAR(20) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    document_type_id BIGINT NOT NULL,
    sequence_id BIGINT NULL,
    establishment_code VARCHAR(10) NULL,
    emission_point_code VARCHAR(10) NULL,
    sequential_number VARCHAR(20) NULL,
    environment_code VARCHAR(10) NULL,
    access_key VARCHAR(80) NULL,
    authorization_code VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    issue_date DATE NOT NULL,
    authorized_at DATETIME NULL,
    buyer_identification VARCHAR(30) NULL,
    buyer_name VARCHAR(150) NULL,
    subtotal_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    xml_payload LONGTEXT NULL,
    validation_errors TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_documents_document_type
        FOREIGN KEY (document_type_id) REFERENCES acc_document_types(id),
    CONSTRAINT fk_tax_documents_sequence
        FOREIGN KEY (sequence_id) REFERENCES acc_document_sequences(id),
    UNIQUE KEY uk_tax_documents_access_key (access_key),
    INDEX idx_tax_documents_tenant_status (tenant_id, status),
    INDEX idx_tax_documents_source (source_type, source_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tax_document_tax_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tax_document_id BIGINT NOT NULL,
    tax_rule_id BIGINT NULL,
    taxable_base DECIMAL(14,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    percentage DECIMAL(8,4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_tax_document_tax_lines_document
        FOREIGN KEY (tax_document_id) REFERENCES tax_documents(id),
    INDEX idx_tax_document_tax_lines_document (tax_document_id)
) ENGINE=InnoDB;

INSERT INTO erp_tenants (tenant_code, legal_name, trade_name, country_code, time_zone, currency_code, active)
VALUES ('default', 'Default ERP Tenant', 'Default ERP Tenant', 'EC', 'America/Guayaquil', 'USD', 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

INSERT INTO erp_tenant_plan_subscriptions (tenant_id, commercial_plan, subscription_status, starts_at)
SELECT t.id, 'START', 'ACTIVE', CURRENT_TIMESTAMP
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1
    FROM erp_tenant_plan_subscriptions s
    WHERE s.tenant_id = t.id
      AND s.subscription_status = 'ACTIVE'
  );

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SALES_INVOICE', 'Factura de venta', 'ACCOUNTING', 'OUTBOUND', 0, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SALES_INVOICE'
  );

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'PURCHASE_INVOICE', 'Factura de compra', 'ACCOUNTING', 'INBOUND', 0, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'PURCHASE_INVOICE'
  );

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_INVOICE', 'Comprobante electronico factura', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_INVOICE'
  );

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_CREDIT_NOTE', 'Comprobante electronico nota de credito', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_CREDIT_NOTE'
  );

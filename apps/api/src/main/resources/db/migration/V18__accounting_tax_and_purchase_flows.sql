CREATE TABLE acc_product_tax_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    tax_rule_id BIGINT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_acc_product_tax_rules_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_acc_product_tax_rules_tax_rule FOREIGN KEY (tax_rule_id) REFERENCES acc_tax_rules(id),
    UNIQUE KEY uk_acc_product_tax_rules_tenant_product (tenant_id, product_id),
    INDEX idx_acc_product_tax_rules_tax_rule (tax_rule_id)
) ENGINE=InnoDB;

CREATE TABLE erp_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    supplier_name VARCHAR(150) NOT NULL,
    supplier_identification VARCHAR(30) NULL,
    supplier_email VARCHAR(120) NULL,
    external_document_number VARCHAR(60) NULL,
    subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
    tax DECIMAL(14,2) NOT NULL DEFAULT 0,
    total DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    due_date DATE NULL,
    received_at DATETIME NULL,
    paid_at DATETIME NULL,
    accounting_entry_id BIGINT NULL,
    payment_accounting_entry_id BIGINT NULL,
    payable_id BIGINT NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_erp_purchases_tenant_status (tenant_id, status)
) ENGINE=InnoDB;

CREATE TABLE erp_purchase_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2) NOT NULL,
    line_subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
    tax_rule_id BIGINT NULL,
    tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(14,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_erp_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES erp_purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_erp_purchase_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_erp_purchase_items_purchase (purchase_id),
    INDEX idx_erp_purchase_items_product (product_id)
) ENGINE=InnoDB;

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '1.1.01.01', 'Caja general', NULL, 'ASSET', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '1.1.01.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '1.1.01.02', 'Bancos', NULL, 'ASSET', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '1.1.01.02'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '1.1.02.01', 'Cuentas por cobrar clientes', NULL, 'ASSET', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '1.1.02.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '1.1.03.01', 'Inventario mercaderia', NULL, 'ASSET', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '1.1.03.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '1.1.04.01', 'IVA credito tributario', NULL, 'ASSET', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '1.1.04.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '2.1.01.01', 'Cuentas por pagar proveedores', NULL, 'LIABILITY', 'CREDIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '2.1.01.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '2.1.02.01', 'IVA debito fiscal', NULL, 'LIABILITY', 'CREDIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '2.1.02.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '4.1.01.01', 'Ventas mostrador', NULL, 'REVENUE', 'CREDIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '4.1.01.01'
  );

INSERT INTO acc_account_catalog (tenant_id, account_code, account_name, parent_account_id, account_type, account_nature, tree_level, allow_posting, active)
SELECT t.id, '5.1.01.01', 'Costo de ventas', NULL, 'EXPENSE', 'DEBIT', 4, 1, 1
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_account_catalog a WHERE a.tenant_id = t.id AND a.account_code = '5.1.01.01'
  );

INSERT INTO acc_tax_rules (tenant_id, tax_code, tax_name, tax_scope, computation_type, rate, tax_authority_code, taxable_object_code, included_in_price, active, valid_from)
SELECT t.id, 'IVA_GENERAL', 'IVA general configurable', 'DOCUMENT', 'PERCENTAGE', 15.0000, '2', '2', 0, 1, CURRENT_DATE
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_tax_rules r WHERE r.tenant_id = t.id AND r.tax_code = 'IVA_GENERAL'
  );

INSERT INTO acc_tax_rules (tenant_id, tax_code, tax_name, tax_scope, computation_type, rate, tax_authority_code, taxable_object_code, included_in_price, active, valid_from)
SELECT t.id, 'IVA_0', 'IVA tarifa 0', 'DOCUMENT', 'PERCENTAGE', 0.0000, '0', '0', 0, 1, CURRENT_DATE
FROM erp_tenants t
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_tax_rules r WHERE r.tenant_id = t.id AND r.tax_code = 'IVA_0'
  );

INSERT INTO acc_document_sequences (tenant_id, document_type_id, establishment_code, emission_point_code, current_number, increment_step, active)
SELECT t.id, d.id, '001', '001', 0, 1, 1
FROM erp_tenants t
JOIN acc_document_types d ON d.tenant_id = t.id AND d.document_code = 'SRI_INVOICE'
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_sequences s
    WHERE s.tenant_id = t.id
      AND s.document_type_id = d.id
      AND s.establishment_code = '001'
      AND s.emission_point_code = '001'
  );

INSERT INTO acc_document_sequences (tenant_id, document_type_id, establishment_code, emission_point_code, current_number, increment_step, active)
SELECT t.id, d.id, '001', '001', 0, 1, 1
FROM erp_tenants t
JOIN acc_document_types d ON d.tenant_id = t.id AND d.document_code = 'SRI_CREDIT_NOTE'
WHERE t.tenant_code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM acc_document_sequences s
    WHERE s.tenant_id = t.id
      AND s.document_type_id = d.id
      AND s.establishment_code = '001'
      AND s.emission_point_code = '001'
  );

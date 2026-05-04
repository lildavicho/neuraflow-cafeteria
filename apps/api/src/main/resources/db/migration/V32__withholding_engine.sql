-- Sprint A.2: Withholding (Retenciones SRI) engine — matrix catalog + issued documents

CREATE TABLE IF NOT EXISTS acc_withholding_matrix (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    concept_code VARCHAR(10) NOT NULL,
    concept_name VARCHAR(200) NOT NULL,
    tax_type VARCHAR(20) NOT NULL,
    supplier_regime VARCHAR(40) NOT NULL,
    rate_percentage DECIMAL(7,4) NOT NULL,
    min_base_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    sri_form_code VARCHAR(10),
    active TINYINT(1) NOT NULL DEFAULT 1,
    valid_from DATE,
    valid_until DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_withholding_matrix_tenant_code_regime (tenant_id, concept_code, supplier_regime, tax_type),
    INDEX idx_acc_withholding_matrix_tenant_active (tenant_id, active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_withholding_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    branch_id BIGINT,
    supplier_identification VARCHAR(30),
    supplier_name VARCHAR(200),
    establishment_code VARCHAR(10),
    emission_point_code VARCHAR(10),
    sequential_number VARCHAR(20),
    access_key VARCHAR(80),
    issue_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    iva_base DECIMAL(14,2) NOT NULL DEFAULT 0,
    iva_withheld DECIMAL(14,2) NOT NULL DEFAULT 0,
    income_base DECIMAL(14,2) NOT NULL DEFAULT 0,
    income_withheld DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_withheld DECIMAL(14,2) NOT NULL DEFAULT 0,
    xml_payload LONGTEXT,
    signed_xml_payload LONGTEXT,
    authorization_code VARCHAR(80),
    authorized_at DATETIME,
    cancelled_at DATETIME,
    cancellation_reason VARCHAR(300),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_acc_withholding_docs_tenant_purchase (tenant_id, purchase_id),
    INDEX idx_acc_withholding_docs_tenant_status (tenant_id, status),
    UNIQUE KEY uk_acc_withholding_docs_access_key (access_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS acc_withholding_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    matrix_id BIGINT,
    concept_code VARCHAR(10) NOT NULL,
    concept_name VARCHAR(200) NOT NULL,
    tax_type VARCHAR(20) NOT NULL,
    base_amount DECIMAL(14,2) NOT NULL,
    rate_percentage DECIMAL(7,4) NOT NULL,
    withheld_amount DECIMAL(14,2) NOT NULL,
    related_document_code VARCHAR(20),
    related_document_serial VARCHAR(40),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_acc_withholding_lines_document (document_id),
    CONSTRAINT fk_acc_withholding_lines_document FOREIGN KEY (document_id)
        REFERENCES acc_withholding_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Seed Ecuador SRI default matrix (codes per resolución NAC-DGERCGC vigente)
-- Tax type INCOME = retención renta, VAT = retención IVA
INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '303', 'Honorarios profesionales', 'INCOME', 'NATURAL_PERSON', 10.0000, 0, '303', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '303' AND w.supplier_regime = 'NATURAL_PERSON' AND w.tax_type = 'INCOME');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '304', 'Servicios predomina el intelecto', 'INCOME', 'NATURAL_PERSON', 8.0000, 0, '304', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '304' AND w.supplier_regime = 'NATURAL_PERSON' AND w.tax_type = 'INCOME');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '307', 'Servicios predomina la mano de obra', 'INCOME', 'COMPANY', 2.0000, 0, '307', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '307' AND w.supplier_regime = 'COMPANY' AND w.tax_type = 'INCOME');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '312', 'Transferencia bienes muebles naturaleza corporal', 'INCOME', 'COMPANY', 1.7500, 0, '312', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '312' AND w.supplier_regime = 'COMPANY' AND w.tax_type = 'INCOME');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '332', 'Pagos arrendamiento bienes inmuebles', 'INCOME', 'NATURAL_PERSON', 8.0000, 0, '332', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '332' AND w.supplier_regime = 'NATURAL_PERSON' AND w.tax_type = 'INCOME');

-- Retenciones IVA (porcentaje sobre IVA causado)
INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '721', 'Retencion IVA bienes 30%', 'VAT', 'COMPANY', 30.0000, 0, '721', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '721' AND w.supplier_regime = 'COMPANY' AND w.tax_type = 'VAT');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '725', 'Retencion IVA servicios 70%', 'VAT', 'COMPANY', 70.0000, 0, '725', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '725' AND w.supplier_regime = 'COMPANY' AND w.tax_type = 'VAT');

INSERT INTO acc_withholding_matrix (tenant_id, concept_code, concept_name, tax_type, supplier_regime, rate_percentage, min_base_amount, sri_form_code, active)
SELECT t.id, '727', 'Retencion IVA servicios profesionales 100%', 'VAT', 'NATURAL_PERSON', 100.0000, 0, '727', 1
FROM erp_tenants t
WHERE NOT EXISTS (SELECT 1 FROM acc_withholding_matrix w WHERE w.tenant_id = t.id AND w.concept_code = '727' AND w.supplier_regime = 'NATURAL_PERSON' AND w.tax_type = 'VAT');

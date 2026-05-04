-- Sprint A.1: extend SRI document type catalog (codDoc 03/05/06/07) and IVA validity windows
-- (Factura 01 + Nota de Crédito 04 already seeded in V16)

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_DEBIT_NOTE', 'Comprobante electronico nota de debito', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_DEBIT_NOTE'
);

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_WITHHOLDING', 'Comprobante electronico de retencion', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_WITHHOLDING'
);

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_WAYBILL', 'Comprobante electronico guia de remision', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_WAYBILL'
);

INSERT INTO acc_document_types (tenant_id, document_code, document_name, fiscal_domain, document_direction, requires_sequence, requires_counterparty, active)
SELECT t.id, 'SRI_PURCHASE_LIQUIDATION', 'Comprobante electronico liquidacion de compra', 'SRI', 'OUTBOUND', 1, 1, 1
FROM erp_tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM acc_document_types d
    WHERE d.tenant_id = t.id AND d.document_code = 'SRI_PURCHASE_LIQUIDATION'
);

-- Tighten existing IVA_GENERAL rule to its current validity window (from 2024-04-01 onward)
UPDATE acc_tax_rules
SET valid_from = '2024-04-01'
WHERE tax_code = 'IVA_GENERAL'
  AND (valid_from IS NULL OR valid_from > '2024-04-01');

-- Add legacy IVA 12% rule for historical document reprocessing (valid until 2024-03-31)
INSERT INTO acc_tax_rules (tenant_id, tax_code, tax_name, tax_scope, computation_type, rate, tax_authority_code, taxable_object_code, included_in_price, active, valid_from, valid_until)
SELECT t.id, 'IVA_LEGACY_12', 'IVA 12% (vigencia hasta 2024-03-31)', 'DOCUMENT', 'PERCENTAGE', 12.0000, '2', '2', 0, 1, '2020-01-01', '2024-03-31'
FROM erp_tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM acc_tax_rules r WHERE r.tenant_id = t.id AND r.tax_code = 'IVA_LEGACY_12'
);

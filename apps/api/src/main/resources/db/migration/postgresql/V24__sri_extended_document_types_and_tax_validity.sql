-- Sprint A.1: extend SRI document type catalog (codDoc 03/05/06/07) and IVA validity windows
-- (Factura 01 + Nota de Crédito 04 already seeded in baseline)

insert into "acc_document_types" (
    "tenant_id", "document_code", "document_name", "fiscal_domain", "document_direction",
    "requires_sequence", "requires_counterparty", "active"
)
select t."id", 'SRI_DEBIT_NOTE', 'Comprobante electronico nota de debito', 'SRI', 'OUTBOUND', true, true, true
from "erp_tenants" t
where not exists (
    select 1 from "acc_document_types" d
    where d."tenant_id" = t."id" and d."document_code" = 'SRI_DEBIT_NOTE'
);

insert into "acc_document_types" (
    "tenant_id", "document_code", "document_name", "fiscal_domain", "document_direction",
    "requires_sequence", "requires_counterparty", "active"
)
select t."id", 'SRI_WITHHOLDING', 'Comprobante electronico de retencion', 'SRI', 'OUTBOUND', true, true, true
from "erp_tenants" t
where not exists (
    select 1 from "acc_document_types" d
    where d."tenant_id" = t."id" and d."document_code" = 'SRI_WITHHOLDING'
);

insert into "acc_document_types" (
    "tenant_id", "document_code", "document_name", "fiscal_domain", "document_direction",
    "requires_sequence", "requires_counterparty", "active"
)
select t."id", 'SRI_WAYBILL', 'Comprobante electronico guia de remision', 'SRI', 'OUTBOUND', true, true, true
from "erp_tenants" t
where not exists (
    select 1 from "acc_document_types" d
    where d."tenant_id" = t."id" and d."document_code" = 'SRI_WAYBILL'
);

insert into "acc_document_types" (
    "tenant_id", "document_code", "document_name", "fiscal_domain", "document_direction",
    "requires_sequence", "requires_counterparty", "active"
)
select t."id", 'SRI_PURCHASE_LIQUIDATION', 'Comprobante electronico liquidacion de compra', 'SRI', 'OUTBOUND', true, true, true
from "erp_tenants" t
where not exists (
    select 1 from "acc_document_types" d
    where d."tenant_id" = t."id" and d."document_code" = 'SRI_PURCHASE_LIQUIDATION'
);

-- Tighten existing IVA_GENERAL rule to its current validity window
update "acc_tax_rules"
set "valid_from" = DATE '2024-04-01'
where "tax_code" = 'IVA_GENERAL'
  and ("valid_from" is null or "valid_from" > DATE '2024-04-01');

-- Add legacy IVA 12% rule for historical document reprocessing
insert into "acc_tax_rules" (
    "tenant_id", "tax_code", "tax_name", "tax_scope", "computation_type", "rate",
    "tax_authority_code", "taxable_object_code", "included_in_price", "active",
    "valid_from", "valid_until"
)
select t."id", 'IVA_LEGACY_12', 'IVA 12% (vigencia hasta 2024-03-31)', 'DOCUMENT', 'PERCENTAGE', 12.0000,
       '2', '2', false, true, DATE '2020-01-01', DATE '2024-03-31'
from "erp_tenants" t
where not exists (
    select 1 from "acc_tax_rules" r
    where r."tenant_id" = t."id" and r."tax_code" = 'IVA_LEGACY_12'
);

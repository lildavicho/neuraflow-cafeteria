set search_path to "bar_app", public;

insert into "settings" ("setting_key", "setting_value")
values ('commercial_plan', 'START')
on conflict ("setting_key") do nothing;

insert into "erp_tenants" (
    "tenant_code",
    "legal_name",
    "trade_name",
    "country_code",
    "time_zone",
    "currency_code",
    "active"
)
values ('default', 'UCACUE Demo Tenant', 'UCACUE Demo', 'EC', 'America/Guayaquil', 'USD', true)
on conflict ("tenant_code") do update
set "legal_name" = excluded."legal_name",
    "trade_name" = excluded."trade_name",
    "country_code" = excluded."country_code",
    "time_zone" = excluded."time_zone",
    "currency_code" = excluded."currency_code",
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "erp_tenant_plan_subscriptions" ("tenant_id", "commercial_plan", "subscription_status", "starts_at")
select t."id", 'VISION_AI', 'ACTIVE', CURRENT_TIMESTAMP
from "erp_tenants" t
where t."tenant_code" = 'default'
  and not exists (
      select 1
      from "erp_tenant_plan_subscriptions" s
      where s."tenant_id" = t."id"
        and s."subscription_status" = 'ACTIVE'
  );

insert into "categories" ("name", "description", "active")
values
    ('Desayunos', 'Menu de desayunos', true),
    ('Almuerzos', 'Menu de almuerzos', true),
    ('Snacks', 'Productos rapidos', true),
    ('Bebidas', 'Bebidas frias y calientes', true),
    ('Postres', 'Postres y dulces', true),
    ('Ingredientes', 'Ingredientes de cocina', true)
on conflict ("name") do update
set "description" = excluded."description",
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "acc_account_catalog" (
    "tenant_id",
    "account_code",
    "account_name",
    "account_type",
    "account_nature",
    "tree_level",
    "allow_posting",
    "active"
)
select t."id", v."account_code", v."account_name", v."account_type", v."account_nature", 4, true, true
from "erp_tenants" t
cross join (
    values
        ('1.1.01.01', 'Caja general', 'ASSET', 'DEBIT'),
        ('1.1.01.02', 'Bancos', 'ASSET', 'DEBIT'),
        ('1.1.02.01', 'Cuentas por cobrar clientes', 'ASSET', 'DEBIT'),
        ('1.1.03.01', 'Inventario mercaderia', 'ASSET', 'DEBIT'),
        ('1.1.04.01', 'IVA credito tributario', 'ASSET', 'DEBIT'),
        ('2.1.01.01', 'Cuentas por pagar proveedores', 'LIABILITY', 'CREDIT'),
        ('2.1.02.01', 'IVA debito fiscal', 'LIABILITY', 'CREDIT'),
        ('4.1.01.01', 'Ventas mostrador', 'REVENUE', 'CREDIT'),
        ('5.1.01.01', 'Costo de ventas', 'EXPENSE', 'DEBIT')
) as v("account_code", "account_name", "account_type", "account_nature")
where t."tenant_code" = 'default'
on conflict ("tenant_id", "account_code") do update
set "account_name" = excluded."account_name",
    "account_type" = excluded."account_type",
    "account_nature" = excluded."account_nature",
    "tree_level" = excluded."tree_level",
    "allow_posting" = true,
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "acc_document_types" (
    "tenant_id",
    "document_code",
    "document_name",
    "fiscal_domain",
    "document_direction",
    "requires_sequence",
    "requires_counterparty",
    "active"
)
select t."id", v."document_code", v."document_name", v."fiscal_domain", v."document_direction", v."requires_sequence", v."requires_counterparty", true
from "erp_tenants" t
cross join (
    values
        ('SALES_INVOICE', 'Factura de venta', 'ACCOUNTING', 'OUTBOUND', false, true),
        ('PURCHASE_INVOICE', 'Factura de compra', 'ACCOUNTING', 'INBOUND', false, true),
        ('SRI_INVOICE', 'Comprobante electronico factura', 'SRI', 'OUTBOUND', true, true),
        ('SRI_CREDIT_NOTE', 'Comprobante electronico nota de credito', 'SRI', 'OUTBOUND', true, true)
) as v("document_code", "document_name", "fiscal_domain", "document_direction", "requires_sequence", "requires_counterparty")
where t."tenant_code" = 'default'
on conflict ("tenant_id", "document_code") do update
set "document_name" = excluded."document_name",
    "fiscal_domain" = excluded."fiscal_domain",
    "document_direction" = excluded."document_direction",
    "requires_sequence" = excluded."requires_sequence",
    "requires_counterparty" = excluded."requires_counterparty",
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "acc_document_sequences" (
    "tenant_id",
    "document_type_id",
    "establishment_code",
    "emission_point_code",
    "current_number",
    "increment_step",
    "active"
)
select t."id", dt."id", '001', '001', 0, 1, true
from "erp_tenants" t
join "acc_document_types" dt on dt."tenant_id" = t."id"
where t."tenant_code" = 'default'
  and dt."document_code" in ('SRI_INVOICE', 'SRI_CREDIT_NOTE')
on conflict ("tenant_id", "document_type_id", "establishment_code", "emission_point_code") do update
set "increment_step" = excluded."increment_step",
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "acc_tax_rules" (
    "tenant_id",
    "tax_code",
    "tax_name",
    "tax_scope",
    "computation_type",
    "rate",
    "tax_authority_code",
    "taxable_object_code",
    "included_in_price",
    "active",
    "valid_from"
)
select t."id", v."tax_code", v."tax_name", 'DOCUMENT', 'PERCENTAGE', v."rate", v."tax_authority_code", v."taxable_object_code", false, true, CURRENT_DATE
from "erp_tenants" t
cross join (
    values
        ('IVA_GENERAL', 'IVA general configurable', 15.0000, '2', '2'),
        ('IVA_0', 'IVA tarifa 0', 0.0000, '0', '0')
) as v("tax_code", "tax_name", "rate", "tax_authority_code", "taxable_object_code")
where t."tenant_code" = 'default'
on conflict ("tenant_id", "tax_code") do update
set "tax_name" = excluded."tax_name",
    "tax_scope" = excluded."tax_scope",
    "computation_type" = excluded."computation_type",
    "rate" = excluded."rate",
    "tax_authority_code" = excluded."tax_authority_code",
    "taxable_object_code" = excluded."taxable_object_code",
    "included_in_price" = false,
    "active" = true,
    "updated_at" = CURRENT_TIMESTAMP;

insert into "products" (
    "category_id",
    "code",
    "name",
    "description",
    "unit",
    "price",
    "purchase_price",
    "stock",
    "min_stock",
    "status",
    "prepared"
)
select c."id", v."code", v."name", v."description", 'UNIDAD', v."price", v."purchase_price", v."stock", v."min_stock", 'AVAILABLE', true
from (
    values
        ('Bebidas', 'CAF-001', 'Cafe americano', 'Cafe negro recien preparado', 1.50, 0.83, 120, 10),
        ('Bebidas', 'JUG-001', 'Jugo natural', 'Jugo de frutas del dia', 2.25, 1.24, 60, 8),
        ('Desayunos', 'DES-001', 'Tostada mixta', 'Pan tostado con huevo y queso', 3.75, 2.06, 40, 6),
        ('Almuerzos', 'ALM-001', 'Almuerzo ejecutivo', 'Menu del dia con sopa y plato fuerte', 5.50, 3.03, 35, 5),
        ('Snacks', 'SNA-001', 'Empanada de queso', 'Empanada horneada lista para servir', 1.25, 0.69, 50, 8),
        ('Postres', 'POS-001', 'Cheesecake', 'Porcion individual de cheesecake', 3.00, 1.65, 20, 4)
) as v("category_name", "code", "name", "description", "price", "purchase_price", "stock", "min_stock")
join "categories" c on c."name" = v."category_name"
on conflict ("code") do update
set "category_id" = excluded."category_id",
    "name" = excluded."name",
    "description" = excluded."description",
    "unit" = excluded."unit",
    "price" = excluded."price",
    "purchase_price" = excluded."purchase_price",
    "min_stock" = excluded."min_stock",
    "status" = excluded."status",
    "prepared" = excluded."prepared",
    "updated_at" = CURRENT_TIMESTAMP;

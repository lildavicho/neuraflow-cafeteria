-- Sprint A.2: Withholding (Retenciones SRI) engine — matrix catalog + issued documents

create table if not exists "acc_withholding_matrix" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "concept_code" varchar(10) not null,
    "concept_name" varchar(200) not null,
    "tax_type" varchar(20) not null,
    "supplier_regime" varchar(40) not null,
    "rate_percentage" decimal(7,4) not null,
    "min_base_amount" decimal(14,2) not null default 0,
    "sri_form_code" varchar(10),
    "active" boolean not null default true,
    "valid_from" date,
    "valid_until" date,
    "created_at" timestamp not null default CURRENT_TIMESTAMP,
    "updated_at" timestamp not null default CURRENT_TIMESTAMP,
    primary key ("id"),
    constraint "uk_acc_withholding_matrix_tenant_code_regime" unique ("tenant_id", "concept_code", "supplier_regime", "tax_type")
);
create index if not exists "idx_acc_withholding_matrix_tenant_active" on "acc_withholding_matrix" ("tenant_id", "active");

create table if not exists "acc_withholding_documents" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "purchase_id" bigint not null,
    "branch_id" bigint,
    "supplier_identification" varchar(30),
    "supplier_name" varchar(200),
    "establishment_code" varchar(10),
    "emission_point_code" varchar(10),
    "sequential_number" varchar(20),
    "access_key" varchar(80),
    "issue_date" date not null,
    "status" varchar(20) not null,
    "iva_base" decimal(14,2) not null default 0,
    "iva_withheld" decimal(14,2) not null default 0,
    "income_base" decimal(14,2) not null default 0,
    "income_withheld" decimal(14,2) not null default 0,
    "total_withheld" decimal(14,2) not null default 0,
    "xml_payload" text,
    "signed_xml_payload" text,
    "authorization_code" varchar(80),
    "authorized_at" timestamp,
    "cancelled_at" timestamp,
    "cancellation_reason" varchar(300),
    "created_at" timestamp not null default CURRENT_TIMESTAMP,
    "updated_at" timestamp not null default CURRENT_TIMESTAMP,
    primary key ("id"),
    constraint "uk_acc_withholding_docs_access_key" unique ("access_key")
);
create index if not exists "idx_acc_withholding_docs_tenant_purchase" on "acc_withholding_documents" ("tenant_id", "purchase_id");
create index if not exists "idx_acc_withholding_docs_tenant_status" on "acc_withholding_documents" ("tenant_id", "status");

create table if not exists "acc_withholding_lines" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "document_id" bigint not null,
    "matrix_id" bigint,
    "concept_code" varchar(10) not null,
    "concept_name" varchar(200) not null,
    "tax_type" varchar(20) not null,
    "base_amount" decimal(14,2) not null,
    "rate_percentage" decimal(7,4) not null,
    "withheld_amount" decimal(14,2) not null,
    "related_document_code" varchar(20),
    "related_document_serial" varchar(40),
    "created_at" timestamp not null default CURRENT_TIMESTAMP,
    primary key ("id"),
    constraint "fk_acc_withholding_lines_document" foreign key ("document_id")
        references "acc_withholding_documents" ("id") on delete cascade
);
create index if not exists "idx_acc_withholding_lines_document" on "acc_withholding_lines" ("document_id");

-- Seeds (Ecuador SRI matrix)
insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '303', 'Honorarios profesionales', 'INCOME', 'NATURAL_PERSON', 10.0000, 0, '303', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '303' and w."supplier_regime" = 'NATURAL_PERSON' and w."tax_type" = 'INCOME');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '304', 'Servicios predomina el intelecto', 'INCOME', 'NATURAL_PERSON', 8.0000, 0, '304', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '304' and w."supplier_regime" = 'NATURAL_PERSON' and w."tax_type" = 'INCOME');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '307', 'Servicios predomina la mano de obra', 'INCOME', 'COMPANY', 2.0000, 0, '307', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '307' and w."supplier_regime" = 'COMPANY' and w."tax_type" = 'INCOME');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '312', 'Transferencia bienes muebles naturaleza corporal', 'INCOME', 'COMPANY', 1.7500, 0, '312', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '312' and w."supplier_regime" = 'COMPANY' and w."tax_type" = 'INCOME');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '332', 'Pagos arrendamiento bienes inmuebles', 'INCOME', 'NATURAL_PERSON', 8.0000, 0, '332', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '332' and w."supplier_regime" = 'NATURAL_PERSON' and w."tax_type" = 'INCOME');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '721', 'Retencion IVA bienes 30%', 'VAT', 'COMPANY', 30.0000, 0, '721', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '721' and w."supplier_regime" = 'COMPANY' and w."tax_type" = 'VAT');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '725', 'Retencion IVA servicios 70%', 'VAT', 'COMPANY', 70.0000, 0, '725', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '725' and w."supplier_regime" = 'COMPANY' and w."tax_type" = 'VAT');

insert into "acc_withholding_matrix" ("tenant_id", "concept_code", "concept_name", "tax_type", "supplier_regime", "rate_percentage", "min_base_amount", "sri_form_code", "active")
select t."id", '727', 'Retencion IVA servicios profesionales 100%', 'VAT', 'NATURAL_PERSON', 100.0000, 0, '727', true
from "erp_tenants" t
where not exists (select 1 from "acc_withholding_matrix" w where w."tenant_id" = t."id" and w."concept_code" = '727' and w."supplier_regime" = 'NATURAL_PERSON' and w."tax_type" = 'VAT');

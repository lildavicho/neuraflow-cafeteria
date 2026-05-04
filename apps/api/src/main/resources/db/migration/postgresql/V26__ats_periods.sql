-- Sprint A.3: Anexo Transaccional Simplificado (ATS) period tracker (PostgreSQL mirror of V33 MySQL)

create table if not exists "acc_ats_periods" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "fiscal_year" int not null,
    "fiscal_month" int not null,
    "status" varchar(20) not null,
    "sales_count" int not null default 0,
    "purchases_count" int not null default 0,
    "withholdings_count" int not null default 0,
    "total_sales" decimal(14,2) not null default 0,
    "total_purchases" decimal(14,2) not null default 0,
    "total_withheld" decimal(14,2) not null default 0,
    "xml_payload" text,
    "generated_at" timestamp,
    "submitted_at" timestamp,
    "created_at" timestamp not null default CURRENT_TIMESTAMP,
    "updated_at" timestamp not null default CURRENT_TIMESTAMP,
    primary key ("id"),
    constraint "uk_acc_ats_periods_tenant_year_month" unique ("tenant_id", "fiscal_year", "fiscal_month")
);
create index if not exists "idx_acc_ats_periods_tenant_status" on "acc_ats_periods" ("tenant_id", "status");

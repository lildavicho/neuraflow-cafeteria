-- V12: Branches (sucursales) and Warehouses (bodegas)
-- Part of Phase 1.1 — Platform Core
set search_path to "bar_app", public;

-- ============================================================
-- BRANCHES (Sucursales)
-- ============================================================
create table if not exists "erp_branches" (
    "id"            bigserial       primary key,
    "tenant_id"     bigint          not null,
    "code"          varchar(20)     not null,
    "name"          varchar(150)    not null,
    "address"       varchar(300),
    "city"          varchar(100),
    "province"      varchar(100),
    "phone"         varchar(30),
    "email"         varchar(150),
    "sri_establishment_code" varchar(10),
    "is_main"       boolean         default false not null,
    "active"        boolean         default true not null,
    "created_at"    timestamp       default CURRENT_TIMESTAMP not null,
    "updated_at"    timestamp       default CURRENT_TIMESTAMP not null
);

create unique index if not exists "uq_erp_branches_tenant_code"
    on "erp_branches" ("tenant_id", lower("code"));

create index if not exists "idx_erp_branches_tenant"
    on "erp_branches" ("tenant_id");

create index if not exists "idx_erp_branches_active"
    on "erp_branches" ("tenant_id", "active");

-- ============================================================
-- WAREHOUSES (Bodegas)
-- ============================================================
create table if not exists "erp_warehouses" (
    "id"            bigserial       primary key,
    "tenant_id"     bigint          not null,
    "branch_id"     bigint          not null,
    "code"          varchar(20)     not null,
    "name"          varchar(150)    not null,
    "type"          varchar(20)     default 'STORE' not null,
    "address"       varchar(300),
    "active"        boolean         default true not null,
    "created_at"    timestamp       default CURRENT_TIMESTAMP not null,
    "updated_at"    timestamp       default CURRENT_TIMESTAMP not null,
    constraint "fk_erp_warehouses_branch"
        foreign key ("branch_id") references "erp_branches" ("id")
);

create unique index if not exists "uq_erp_warehouses_tenant_code"
    on "erp_warehouses" ("tenant_id", lower("code"));

create index if not exists "idx_erp_warehouses_tenant"
    on "erp_warehouses" ("tenant_id");

create index if not exists "idx_erp_warehouses_branch"
    on "erp_warehouses" ("branch_id");

create index if not exists "idx_erp_warehouses_active"
    on "erp_warehouses" ("tenant_id", "active");

-- ============================================================
-- SEED: Default branch and warehouse for existing tenants
-- ============================================================
insert into "erp_branches" ("tenant_id", "code", "name", "is_main", "active")
select t."id", 'MAIN', 'Sucursal Principal', true, true
from "erp_tenants" t
where not exists (
    select 1 from "erp_branches" b where b."tenant_id" = t."id"
);

insert into "erp_warehouses" ("tenant_id", "branch_id", "code", "name", "type", "active")
select b."tenant_id", b."id", 'PRINCIPAL', 'Bodega Principal', 'STORE', true
from "erp_branches" b
where b."is_main" = true
and not exists (
    select 1 from "erp_warehouses" w where w."branch_id" = b."id"
);

-- V13: Party Model — unified persons and organizations
-- Part of Phase 1.3 — Masters Core
set search_path to "bar_app", public;

-- ============================================================
-- PARTIES (Personas y Organizaciones)
-- ============================================================
create table if not exists "erp_parties" (
    "id"                bigserial       primary key,
    "tenant_id"         bigint          not null,
    "party_type"        varchar(20)     default 'PERSON' not null,
    "display_name"      varchar(200)    not null,
    "legal_name"        varchar(200),
    "first_name"        varchar(100),
    "last_name"         varchar(100),
    "email"             varchar(150),
    "phone"             varchar(30),
    "mobile"            varchar(30),
    "website"           varchar(200),
    "notes"             text,
    "active"            boolean         default true not null,
    "created_at"        timestamp       default CURRENT_TIMESTAMP not null,
    "updated_at"        timestamp       default CURRENT_TIMESTAMP not null
);

create index if not exists "idx_erp_parties_tenant"
    on "erp_parties" ("tenant_id");

create index if not exists "idx_erp_parties_display"
    on "erp_parties" ("tenant_id", lower("display_name"));

create index if not exists "idx_erp_parties_type"
    on "erp_parties" ("tenant_id", "party_type");

-- ============================================================
-- PARTY IDENTIFIERS (cedula, RUC, pasaporte, etc.)
-- ============================================================
create table if not exists "erp_party_identifiers" (
    "id"                bigserial       primary key,
    "tenant_id"         bigint          not null,
    "party_id"          bigint          not null,
    "identifier_type"   varchar(30)     not null,
    "identifier_value"  varchar(50)     not null,
    "is_primary"        boolean         default false not null,
    "created_at"        timestamp       default CURRENT_TIMESTAMP not null,
    constraint "fk_party_identifiers_party"
        foreign key ("party_id") references "erp_parties" ("id")
);

create index if not exists "idx_erp_party_identifiers_party"
    on "erp_party_identifiers" ("party_id");

create unique index if not exists "uq_erp_party_identifiers_tenant_type_value"
    on "erp_party_identifiers" ("tenant_id", "identifier_type", lower("identifier_value"));

-- ============================================================
-- PARTY ADDRESSES
-- ============================================================
create table if not exists "erp_party_addresses" (
    "id"                bigserial       primary key,
    "tenant_id"         bigint          not null,
    "party_id"          bigint          not null,
    "address_type"      varchar(20)     default 'MAIN' not null,
    "street"            varchar(300),
    "city"              varchar(100),
    "province"          varchar(100),
    "postal_code"       varchar(20),
    "country"           varchar(5)      default 'EC' not null,
    "is_primary"        boolean         default false not null,
    "created_at"        timestamp       default CURRENT_TIMESTAMP not null,
    constraint "fk_party_addresses_party"
        foreign key ("party_id") references "erp_parties" ("id")
);

create index if not exists "idx_erp_party_addresses_party"
    on "erp_party_addresses" ("party_id");

-- ============================================================
-- PARTY CONTACTS (additional contact points)
-- ============================================================
create table if not exists "erp_party_contacts" (
    "id"                bigserial       primary key,
    "tenant_id"         bigint          not null,
    "party_id"          bigint          not null,
    "contact_type"      varchar(20)     not null,
    "contact_value"     varchar(200)    not null,
    "label"             varchar(100),
    "created_at"        timestamp       default CURRENT_TIMESTAMP not null,
    constraint "fk_party_contacts_party"
        foreign key ("party_id") references "erp_parties" ("id")
);

create index if not exists "idx_erp_party_contacts_party"
    on "erp_party_contacts" ("party_id");

-- ============================================================
-- PARTY ROLES (CUSTOMER, SUPPLIER, EMPLOYEE, STUDENT, etc.)
-- ============================================================
create table if not exists "erp_party_roles" (
    "id"                bigserial       primary key,
    "tenant_id"         bigint          not null,
    "party_id"          bigint          not null,
    "role_type"         varchar(30)     not null,
    "active"            boolean         default true not null,
    "metadata_json"     text,
    "created_at"        timestamp       default CURRENT_TIMESTAMP not null,
    "updated_at"        timestamp       default CURRENT_TIMESTAMP not null,
    constraint "fk_party_roles_party"
        foreign key ("party_id") references "erp_parties" ("id")
);

create unique index if not exists "uq_erp_party_roles_tenant_party_role"
    on "erp_party_roles" ("tenant_id", "party_id", "role_type");

create index if not exists "idx_erp_party_roles_tenant_role"
    on "erp_party_roles" ("tenant_id", "role_type");

create index if not exists "idx_erp_party_roles_party"
    on "erp_party_roles" ("party_id");

set search_path to "bar_app", public;

insert into "erp_tenants" (
    "tenant_code",
    "legal_name",
    "trade_name",
    "country_code",
    "time_zone",
    "currency_code",
    "active"
)
values (
    'default',
    'Negocio principal',
    'Negocio principal',
    'EC',
    'America/Guayaquil',
    'USD',
    true
)
on conflict ("tenant_code") do nothing;

alter table "users" add column if not exists "tenant_id" bigint;
alter table "users" add column if not exists "platform_admin" boolean not null default false;

update "users"
set "platform_admin" = true
where "role" = 'ADMIN'
  and coalesce("provider", '') <> 'firebase';

update "users"
set "tenant_id" = (
    select "id"
    from "erp_tenants"
    where lower("tenant_code") = 'default'
    limit 1
)
where "tenant_id" is null;

do $$
begin
    alter table "users"
        add constraint "fk_users_tenant"
        foreign key ("tenant_id")
        references "erp_tenants" ("id");
exception
    when duplicate_object then null;
end $$;

create index if not exists "idx_users_tenant" on "users" ("tenant_id");
create index if not exists "idx_users_platform_admin" on "users" ("platform_admin");

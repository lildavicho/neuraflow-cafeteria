set search_path to "bar_app", public;

alter table "tax_sri_settings"
    add column if not exists "rimpe_contributor" boolean not null default false;

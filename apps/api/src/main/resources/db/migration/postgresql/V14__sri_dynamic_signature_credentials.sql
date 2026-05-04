set search_path to "bar_app", public;

alter table "tax_sri_settings"
    add column if not exists "signature_mode" varchar(20) not null default 'NONE';

alter table "tax_sri_settings"
    add column if not exists "p12_content" bytea;

alter table "tax_sri_settings"
    add column if not exists "p12_password_encrypted" text;

alter table "tax_sri_settings"
    add column if not exists "sri_encryption_salt" varchar(80);

alter table "tax_sri_settings"
    add column if not exists "p12_key_alias" varchar(120);

alter table "tax_sri_settings"
    add column if not exists "signature_updated_at" timestamp;

update "acc_tax_rules"
set "taxable_object_code" = '4',
    "updated_at" = current_timestamp
where "tax_authority_code" = '2'
  and round("rate", 2) = 15.00
  and coalesce("taxable_object_code", '') <> '4';

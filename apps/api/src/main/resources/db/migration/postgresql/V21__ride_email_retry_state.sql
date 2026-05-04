set search_path to "bar_app", public;

alter table "tax_documents"
    add column if not exists "ride_email_attempts" integer not null default 0;

alter table "tax_documents"
    add column if not exists "ride_email_next_retry_at" timestamp;

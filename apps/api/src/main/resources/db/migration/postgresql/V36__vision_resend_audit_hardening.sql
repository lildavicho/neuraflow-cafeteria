set search_path to "bar_app", public;

alter table "vision_audit_logs"
    alter column "tenant_id" drop not null;

alter table "invoice_email_logs"
    alter column "to_email" drop not null;

alter table "invoice_email_logs"
    alter column "subject" drop not null;

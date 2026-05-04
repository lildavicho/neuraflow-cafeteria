-- UCACUE Bar runtime schema for Supabase/PostgreSQL.
-- Generated from the working backend schema; do not include local secrets here.
create schema if not exists "bar_app";
set search_path to "bar_app", public;

create table if not exists "acc_account_catalog" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "account_code" varchar(30) not null,
    "account_name" varchar(150) not null,
    "parent_account_id" bigint,
    "account_type" varchar(20) not null,
    "account_nature" varchar(20) not null,
    "tree_level" integer default 1 not null,
    "allow_posting" boolean default true not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_document_sequences" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "document_type_id" bigint not null,
    "establishment_code" varchar(10) not null,
    "emission_point_code" varchar(10) not null,
    "current_number" bigint default 0 not null,
    "increment_step" integer default 1 not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_document_types" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "document_code" varchar(30) not null,
    "document_name" varchar(120) not null,
    "fiscal_domain" varchar(20) not null,
    "document_direction" varchar(20) not null,
    "requires_sequence" boolean default true not null,
    "requires_counterparty" boolean default false not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_journal_entries" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "entry_number" varchar(40) not null,
    "entry_date" date not null,
    "status" varchar(20) not null,
    "source_module" varchar(20) not null,
    "source_type" varchar(40) not null,
    "source_id" bigint not null,
    "description" varchar(255) not null,
    "external_reference" varchar(80),
    "posted_at" timestamp,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_journal_entry_lines" (
    "id" bigserial,
    "entry_id" bigint not null,
    "line_number" integer not null,
    "account_id" bigint not null,
    "tax_rule_id" bigint,
    "debit" numeric(14,2) default 0.00 not null,
    "credit" numeric(14,2) default 0.00 not null,
    "description" varchar(255),
    "reference_code" varchar(80),
    primary key ("id")
);

create table if not exists "acc_payables" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "supplier_id" bigint,
    "source_document_type" varchar(40) not null,
    "source_document_id" bigint not null,
    "issue_date" date not null,
    "due_date" date,
    "total_amount" numeric(14,2) default 0.00 not null,
    "paid_amount" numeric(14,2) default 0.00 not null,
    "balance" numeric(14,2) default 0.00 not null,
    "status" varchar(20) default 'OPEN' not null,
    "accounting_entry_id" bigint,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_product_tax_rules" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "product_id" bigint not null,
    "tax_rule_id" bigint not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_receivables" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "customer_id" bigint,
    "source_document_type" varchar(40) not null,
    "source_document_id" bigint not null,
    "issue_date" date not null,
    "due_date" date,
    "total_amount" numeric(14,2) default 0.00 not null,
    "paid_amount" numeric(14,2) default 0.00 not null,
    "balance" numeric(14,2) default 0.00 not null,
    "status" varchar(20) default 'OPEN' not null,
    "accounting_entry_id" bigint,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "acc_tax_rules" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "tax_code" varchar(30) not null,
    "tax_name" varchar(120) not null,
    "tax_scope" varchar(20) not null,
    "computation_type" varchar(20) not null,
    "rate" numeric(8,4) default 0.0000 not null,
    "tax_authority_code" varchar(30),
    "taxable_object_code" varchar(30),
    "included_in_price" boolean default false not null,
    "active" boolean default true not null,
    "valid_from" date,
    "valid_until" date,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "cameras" (
    "id" bigserial,
    "name" varchar(50) not null,
    "rtsp_url" varchar(255) not null,
    "description" varchar(200),
    "location" varchar(50),
    "enabled" boolean default true not null,
    "stream_path" varchar(50),
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "cash_registers" (
    "id" bigserial,
    "actual_cash" numeric(12,2),
    "card_sales_total" numeric(12,2) not null,
    "cash_sales_total" numeric(12,2) not null,
    "closed_at" timestamp,
    "closing_amount" numeric(12,2),
    "difference" numeric(12,2),
    "expected_cash" numeric(12,2),
    "notes" varchar(500),
    "opened_at" timestamp not null,
    "opening_amount" numeric(12,2) not null,
    "status" varchar(20) not null,
    "total_orders" integer not null,
    "total_sales_amount" numeric(12,2) not null,
    "transfer_sales_total" numeric(12,2) not null,
    "user_id" bigint not null,
    primary key ("id")
);

create table if not exists "categories" (
    "id" bigserial,
    "name" varchar(50) not null,
    "description" varchar(200),
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "erp_purchases" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "supplier_name" varchar(150) not null,
    "supplier_identification" varchar(30),
    "supplier_email" varchar(120),
    "external_document_number" varchar(60),
    "subtotal" numeric(14,2) default 0.00 not null,
    "tax" numeric(14,2) default 0.00 not null,
    "total" numeric(14,2) default 0.00 not null,
    "status" varchar(20) default 'DRAFT' not null,
    "due_date" date,
    "received_at" timestamp,
    "paid_at" timestamp,
    "accounting_entry_id" bigint,
    "payment_accounting_entry_id" bigint,
    "payable_id" bigint,
    "notes" varchar(500),
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "erp_purchase_items" (
    "id" bigserial,
    "purchase_id" bigint not null,
    "product_id" bigint not null,
    "quantity" integer not null,
    "unit_cost" numeric(12,2) not null,
    "line_subtotal" numeric(14,2) default 0.00 not null,
    "tax_rule_id" bigint,
    "tax_amount" numeric(14,2) default 0.00 not null,
    "line_total" numeric(14,2) default 0.00 not null,
    primary key ("id")
);

create table if not exists "erp_tenants" (
    "id" bigserial,
    "tenant_code" varchar(50) not null,
    "legal_name" varchar(150) not null,
    "trade_name" varchar(150),
    "country_code" varchar(5) default 'EC' not null,
    "time_zone" varchar(50) default 'America/Guayaquil' not null,
    "currency_code" varchar(10) default 'USD' not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "erp_tenant_feature_flags" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "module_code" varchar(40) not null,
    "enabled" boolean default true not null,
    "reason_text" varchar(255),
    "expires_at" timestamp,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "erp_tenant_plan_subscriptions" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "commercial_plan" varchar(30) not null,
    "subscription_status" varchar(30) default 'ACTIVE' not null,
    "starts_at" timestamp not null,
    "ends_at" timestamp,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "loyalty_ledger" (
    "id" bigserial,
    "user_id" bigint not null,
    "delta" integer not null,
    "reason" varchar(200),
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "loyalty_points" (
    "id" bigserial,
    "user_id" bigint not null,
    "points" integer default 0 not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "ml_predictions" (
    "id" bigserial,
    "order_id" bigint,
    "prediction_type" varchar(40) not null,
    "predicted_value" varchar(255) not null,
    "actual_value" varchar(255),
    "confidence_score" numeric(5,4),
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "model_metrics" (
    "id" bigserial,
    "model_name" varchar(255) not null,
    "metric_type" varchar(20) not null,
    "metric_value" numeric(10,4) not null,
    "training_date" timestamp default CURRENT_TIMESTAMP not null,
    "data_points_used" integer,
    primary key ("id")
);

create table if not exists "notification_logs" (
    "id" bigserial,
    "order_id" bigint,
    "user_id" bigint,
    "notification_type" varchar(40) not null,
    "delivery_method" varchar(20) not null,
    "status" varchar(20) not null,
    "sent_at" timestamp default CURRENT_TIMESTAMP not null,
    "delivered_at" timestamp,
    "error_message" text,
    primary key ("id")
);

create table if not exists "orders" (
    "id" bigserial,
    "user_id" bigint,
    "subtotal" numeric(12,2) not null,
    "tax" numeric(12,2) not null,
    "total" numeric(12,2) not null,
    "status" varchar(20) not null,
    "transaction_status" varchar(30) not null,
    "payment_method" varchar(20),
    "payment_reference" varchar(100),
    "payment_status" varchar(30),
    "paid_at" timestamp,
    "cancelled_at" timestamp,
    "refunded_at" timestamp,
    "inventory_status" varchar(30) not null,
    "notes" varchar(500),
    "loyalty_points_awarded" integer,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    "preparation_start_at" timestamp,
    "estimated_ready_at" timestamp,
    "actual_ready_at" timestamp,
    "payment_breakdown_json" varchar(2000),
    primary key ("id")
);

create table if not exists "order_items" (
    "id" bigserial,
    "order_id" bigint not null,
    "product_id" bigint not null,
    "quantity" integer not null,
    "unit_price" numeric(12,2) not null,
    "line_total" numeric(12,2) not null,
    primary key ("id")
);

create table if not exists "people_counts" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "ts" timestamp not null,
    "count" integer not null,
    primary key ("id")
);

create table if not exists "people_counts_hourly" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "hour_start" timestamp not null,
    "avg_count" numeric(10,2) not null,
    primary key ("id")
);

create table if not exists "products" (
    "id" bigserial,
    "category_id" bigint not null,
    "code" varchar(20) not null,
    "name" varchar(100) not null,
    "description" varchar(500),
    "image_url" varchar(255),
    "unit" varchar(20) not null,
    "price" numeric(10,2) not null,
    "purchase_price" numeric(10,2),
    "stock" integer default 0 not null,
    "min_stock" integer default 5 not null,
    "status" varchar(20) not null,
    "prepared" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "push_tokens" (
    "id" bigserial,
    "user_id" bigint,
    "fcm_token" varchar(255) not null,
    "platform" varchar(20),
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    "device_name" varchar(255),
    "last_used" timestamp,
    "is_active" boolean default true not null,
    primary key ("id")
);

create table if not exists "sales" (
    "id" bigserial,
    "order_id" bigint not null,
    "total" numeric(12,2) not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "paid_at" timestamp,
    "sale_status" varchar(20) not null,
    "refunded_at" timestamp,
    primary key ("id")
);

create table if not exists "sale_items" (
    "id" bigserial,
    "quantity" integer not null,
    "subtotal" numeric(10,2) not null,
    "subtotal_discount" numeric(10,2),
    "unit_price" numeric(10,2) not null,
    "product_id" bigint not null,
    "sale_id" bigint not null,
    primary key ("id")
);

create table if not exists "settings" (
    "id" bigserial,
    "setting_key" varchar(100) not null,
    "setting_value" text,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "stock_movements" (
    "id" bigserial,
    "product_id" bigint not null,
    "type" varchar(20) not null,
    "quantity" integer not null,
    "stock_before" integer not null,
    "stock_after" integer not null,
    "reason" varchar(200),
    "user_id" bigint not null,
    "reference_type" varchar(50),
    "reference_id" bigint,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "tax_documents" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "source_module" varchar(20) not null,
    "source_type" varchar(40) not null,
    "source_id" bigint not null,
    "document_type_id" bigint not null,
    "sequence_id" bigint,
    "establishment_code" varchar(10),
    "emission_point_code" varchar(10),
    "sequential_number" varchar(20),
    "environment_code" varchar(10),
    "access_key" varchar(80),
    "authorization_code" varchar(80),
    "status" varchar(30) default 'DRAFT' not null,
    "reception_status" varchar(40),
    "authorization_status" varchar(40),
    "issue_date" date not null,
    "sent_at" timestamp,
    "last_status_at" timestamp,
    "authorized_at" timestamp,
    "buyer_identification" varchar(30),
    "buyer_name" varchar(150),
    "subtotal_amount" numeric(14,2) default 0.00 not null,
    "tax_amount" numeric(14,2) default 0.00 not null,
    "total_amount" numeric(14,2) default 0.00 not null,
    "xml_payload" text,
    "signed_xml_payload" text,
    "validation_errors" text,
    "last_provider_message" text,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "tax_document_tax_lines" (
    "id" bigserial,
    "tax_document_id" bigint not null,
    "tax_rule_id" bigint,
    "taxable_base" numeric(14,2) default 0.00 not null,
    "tax_amount" numeric(14,2) default 0.00 not null,
    "percentage" numeric(8,4) default 0.0000 not null,
    primary key ("id")
);

create table if not exists "tax_document_transmissions" (
    "id" bigserial,
    "tax_document_id" bigint not null,
    "phase" varchar(20) not null,
    "endpoint_url" varchar(255) not null,
    "request_payload" text,
    "response_payload" text,
    "provider_status" varchar(60),
    "provider_message" text,
    "http_status" integer,
    "attempt_number" integer default 1 not null,
    "trace_id" varchar(100),
    "transport_error" boolean default false not null,
    "success" boolean default false not null,
    "attempted_at" timestamp default CURRENT_TIMESTAMP not null,
    "completed_at" timestamp,
    primary key ("id")
);

create table if not exists "tax_sri_settings" (
    "id" bigserial,
    "tenant_id" bigint not null,
    "environment_code" varchar(10) default '1' not null,
    "emission_code" varchar(10) default '1' not null,
    "issuer_ruc" varchar(13) not null,
    "issuer_legal_name" varchar(300) not null,
    "issuer_trade_name" varchar(300),
    "matrix_address" varchar(300) not null,
    "establishment_address" varchar(300) not null,
    "special_taxpayer" varchar(20),
    "obligated_accounting" varchar(2) default 'NO' not null,
    "active" boolean default true not null,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "users" (
    "id" bigserial,
    "name" varchar(100) not null,
    "email" varchar(120) not null,
    "password_hash" varchar(255),
    "phone" varchar(15),
    "identification" varchar(20),
    "role" varchar(20) not null,
    "active" boolean default true not null,
    "firebase_uid" varchar(100),
    "provider" varchar(50),
    "avatar_url" varchar(255),
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "user_preferences" (
    "id" bigserial,
    "user_id" bigint not null,
    "preferred_categories" text,
    "dietary_restrictions" text,
    "average_order_value" numeric(12,2),
    "favorite_dishes" text,
    "last_updated" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "vision_detection_events" (
    "id" bigserial,
    "camera_id" bigint not null,
    "event_key" varchar(120) not null,
    "ts" timestamp not null,
    "people_count" integer default 0 not null,
    "unique_people" integer default 0 not null,
    "event_type" varchar(30) default 'ENTRY' not null,
    "source" varchar(40) default 'VISION_AI' not null,
    "track_ids" text,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create table if not exists "vision_hourly_metrics" (
    "id" bigserial,
    "camera_id" bigint not null,
    "hour_start" timestamp not null,
    "unique_people" integer default 0 not null,
    "event_count" integer default 0 not null,
    "last_event_at" timestamp,
    "created_at" timestamp default CURRENT_TIMESTAMP not null,
    "updated_at" timestamp default CURRENT_TIMESTAMP not null,
    primary key ("id")
);

create index if not exists "idx_acc_account_catalog_parent" on "acc_account_catalog" ("parent_account_id");
create unique index if not exists "idx_acc_account_catalog_tenant_code" on "acc_account_catalog" ("tenant_id", "account_code");
create unique index if not exists "idx_acc_document_sequences_tenant_codes" on "acc_document_sequences" ("tenant_id", "document_type_id", "establishment_code", "emission_point_code");
create index if not exists "idx_acc_document_sequences_type" on "acc_document_sequences" ("document_type_id");
create unique index if not exists "idx_acc_document_types_tenant_code" on "acc_document_types" ("tenant_id", "document_code");
create index if not exists "idx_acc_journal_entries_source" on "acc_journal_entries" ("source_type", "source_id");
create unique index if not exists "idx_acc_journal_entries_tenant_entry" on "acc_journal_entries" ("tenant_id", "entry_number");
create index if not exists "idx_acc_journal_entry_lines_entry" on "acc_journal_entry_lines" ("entry_id");
create index if not exists "idx_acc_payables_source" on "acc_payables" ("source_document_type", "source_document_id");
create index if not exists "idx_acc_payables_tenant_status" on "acc_payables" ("tenant_id", "status");
create index if not exists "fk_acc_product_tax_rules_product" on "acc_product_tax_rules" ("product_id");
create index if not exists "idx_acc_product_tax_rules_tax_rule" on "acc_product_tax_rules" ("tax_rule_id");
create index if not exists "idx_acc_product_tax_rules_tenant_product" on "acc_product_tax_rules" ("tenant_id", "product_id");
create unique index if not exists "uk_acc_product_tax_rules_tenant_product" on "acc_product_tax_rules" ("tenant_id", "product_id");
create index if not exists "idx_acc_receivables_source" on "acc_receivables" ("source_document_type", "source_document_id");
create index if not exists "idx_acc_receivables_tenant_status" on "acc_receivables" ("tenant_id", "status");
create unique index if not exists "idx_acc_tax_rules_tenant_code" on "acc_tax_rules" ("tenant_id", "tax_code");
create unique index if not exists "idx_cameras_name" on "cameras" ("name");
create index if not exists "idx_cash_reg_status" on "cash_registers" ("status");
create index if not exists "idx_cash_reg_user" on "cash_registers" ("user_id");
create unique index if not exists "idx_categories_name" on "categories" ("name");
create index if not exists "idx_erp_purchases_tenant_status" on "erp_purchases" ("tenant_id", "status");
create index if not exists "idx_erp_purchase_items_product" on "erp_purchase_items" ("product_id");
create index if not exists "idx_erp_purchase_items_purchase" on "erp_purchase_items" ("purchase_id");
create unique index if not exists "idx_erp_tenants_code" on "erp_tenants" ("tenant_code");
create index if not exists "idx_erp_tenant_feature_flags_module" on "erp_tenant_feature_flags" ("module_code");
create index if not exists "idx_erp_tenant_feature_flags_tenant" on "erp_tenant_feature_flags" ("tenant_id");
create unique index if not exists "uk_erp_tenant_feature_flags_tenant_module" on "erp_tenant_feature_flags" ("tenant_id", "module_code");
create index if not exists "idx_erp_tenant_plan_subscriptions_status" on "erp_tenant_plan_subscriptions" ("subscription_status");
create index if not exists "idx_erp_tenant_plan_subscriptions_tenant" on "erp_tenant_plan_subscriptions" ("tenant_id");
create index if not exists "idx_loyalty_ledger_created_at" on "loyalty_ledger" ("created_at");
create index if not exists "idx_loyalty_ledger_user" on "loyalty_ledger" ("user_id");
create index if not exists "idx_loyalty_points_user" on "loyalty_points" ("user_id");
create unique index if not exists "idx_loyalty_points_user_id" on "loyalty_points" ("user_id");
create index if not exists "idx_ml_predictions_created" on "ml_predictions" ("created_at");
create index if not exists "idx_ml_predictions_order" on "ml_predictions" ("order_id");
create index if not exists "idx_model_metrics_name" on "model_metrics" ("model_name");
create index if not exists "idx_model_metrics_training" on "model_metrics" ("training_date");
create index if not exists "idx_notification_logs_order" on "notification_logs" ("order_id");
create index if not exists "idx_notification_logs_sent_at" on "notification_logs" ("sent_at");
create index if not exists "idx_notification_logs_user" on "notification_logs" ("user_id");
create index if not exists "idx_orders_created_at" on "orders" ("created_at");
create index if not exists "idx_orders_inventory_status" on "orders" ("inventory_status");
create index if not exists "idx_orders_paid_at" on "orders" ("paid_at");
create index if not exists "idx_orders_payment_status" on "orders" ("payment_status");
create index if not exists "idx_orders_status" on "orders" ("status");
create index if not exists "idx_orders_transaction_status" on "orders" ("transaction_status");
create index if not exists "idx_orders_user" on "orders" ("user_id");
create index if not exists "idx_order_items_order" on "order_items" ("order_id");
create index if not exists "idx_order_items_product" on "order_items" ("product_id");
create index if not exists "idx_people_counts_ts" on "people_counts" ("ts");
create index if not exists "idx_people_counts_tenant" on "people_counts" ("tenant_id");
create index if not exists "idx_people_counts_hourly_hour_start" on "people_counts_hourly" ("hour_start");
create index if not exists "idx_people_counts_hourly_tenant" on "people_counts_hourly" ("tenant_id");
create unique index if not exists "uk_people_counts_hourly_tenant_hour" on "people_counts_hourly" ("tenant_id", "hour_start");
create unique index if not exists "idx_products_code" on "products" ("code");
create index if not exists "idx_products_category" on "products" ("category_id");
create index if not exists "idx_products_name" on "products" ("name");
create index if not exists "idx_product_code" on "products" ("code");
create unique index if not exists "idx_push_tokens_fcm_token" on "push_tokens" ("fcm_token");
create index if not exists "idx_push_tokens_token" on "push_tokens" ("fcm_token");
create index if not exists "idx_push_tokens_user" on "push_tokens" ("user_id");
create index if not exists "idx_sales_order" on "sales" ("order_id");
create index if not exists "idx_sales_paid_at" on "sales" ("paid_at");
create index if not exists "idx_sales_sale_status" on "sales" ("sale_status");
create unique index if not exists "idx_sales_order_id" on "sales" ("order_id");
create index if not exists "idx_sale_item_product" on "sale_items" ("product_id");
create index if not exists "idx_sale_item_sale" on "sale_items" ("sale_id");
create unique index if not exists "idx_settings_key" on "settings" ("setting_key");
create index if not exists "fk_stock_movements_user" on "stock_movements" ("user_id");
create index if not exists "idx_stock_movements_created" on "stock_movements" ("created_at");
create index if not exists "idx_stock_movements_product" on "stock_movements" ("product_id");
create index if not exists "fk_tax_documents_document_type" on "tax_documents" ("document_type_id");
create index if not exists "fk_tax_documents_sequence" on "tax_documents" ("sequence_id");
create unique index if not exists "idx_tax_documents_access_key" on "tax_documents" ("access_key");
create index if not exists "idx_tax_documents_source" on "tax_documents" ("source_type", "source_id");
create index if not exists "idx_tax_documents_tenant_status" on "tax_documents" ("tenant_id", "status");
create index if not exists "idx_tax_document_tax_lines_document" on "tax_document_tax_lines" ("tax_document_id");
create index if not exists "idx_tax_document_transmissions_document_phase" on "tax_document_transmissions" ("tax_document_id", "phase", "attempted_at");
create index if not exists "idx_tax_document_transmissions_status" on "tax_document_transmissions" ("provider_status", "success");
create index if not exists "idx_tax_document_transmissions_trace" on "tax_document_transmissions" ("trace_id");
create unique index if not exists "uk_tax_sri_settings_tenant" on "tax_sri_settings" ("tenant_id");
create unique index if not exists "idx_users_email" on "users" ("email");
create unique index if not exists "idx_users_firebase_uid" on "users" ("firebase_uid");
create unique index if not exists "idx_users_identification" on "users" ("identification");
create index if not exists "idx_users_created_at" on "users" ("created_at");
create index if not exists "idx_users_email_users" on "users" ("email");
create index if not exists "idx_users_firebase_uid_users" on "users" ("firebase_uid");
create unique index if not exists "idx_user_preferences_unique_user_preferences" on "user_preferences" ("user_id");
create index if not exists "idx_vision_detection_events_camera_ts" on "vision_detection_events" ("camera_id", "ts");
create index if not exists "idx_vision_detection_events_ts" on "vision_detection_events" ("ts");
create unique index if not exists "uk_vision_detection_events_camera_event" on "vision_detection_events" ("camera_id", "event_key");
create index if not exists "idx_vision_hourly_metrics_camera_hour_start" on "vision_hourly_metrics" ("camera_id", "hour_start");
create index if not exists "idx_vision_hourly_metrics_hour_start" on "vision_hourly_metrics" ("hour_start");
create unique index if not exists "uk_vision_hourly_metrics_camera_hour" on "vision_hourly_metrics" ("camera_id", "hour_start");

alter table "acc_document_sequences" add constraint "fk_acc_document_sequences_document_type" foreign key ("document_type_id") references "acc_document_types" ("id");
alter table "acc_journal_entry_lines" add constraint "fk_acc_journal_entry_lines_entry" foreign key ("entry_id") references "acc_journal_entries" ("id");
alter table "acc_product_tax_rules" add constraint "fk_acc_product_tax_rules_product" foreign key ("product_id") references "products" ("id");
alter table "acc_product_tax_rules" add constraint "fk_acc_product_tax_rules_tax_rule" foreign key ("tax_rule_id") references "acc_tax_rules" ("id");
alter table "cash_registers" add constraint "fkjsgmchr83413apfdvs19tbohv" foreign key ("user_id") references "users" ("id");
alter table "erp_purchase_items" add constraint "fk_erp_purchase_items_product" foreign key ("product_id") references "products" ("id");
alter table "erp_purchase_items" add constraint "fk_erp_purchase_items_purchase" foreign key ("purchase_id") references "erp_purchases" ("id") on delete cascade;
alter table "erp_tenant_feature_flags" add constraint "fk_erp_tenant_feature_flags_tenant" foreign key ("tenant_id") references "erp_tenants" ("id");
alter table "erp_tenant_plan_subscriptions" add constraint "fk_erp_tenant_plan_subscriptions_tenant" foreign key ("tenant_id") references "erp_tenants" ("id");
alter table "loyalty_ledger" add constraint "fk_loyalty_ledger_user" foreign key ("user_id") references "users" ("id");
alter table "loyalty_points" add constraint "fk_loyalty_points_user" foreign key ("user_id") references "users" ("id");
alter table "ml_predictions" add constraint "fk_ml_predictions_order" foreign key ("order_id") references "orders" ("id");
alter table "notification_logs" add constraint "fk_notification_logs_order" foreign key ("order_id") references "orders" ("id");
alter table "notification_logs" add constraint "fk_notification_logs_user" foreign key ("user_id") references "users" ("id");
alter table "orders" add constraint "fk_orders_user" foreign key ("user_id") references "users" ("id");
alter table "order_items" add constraint "fk_order_items_order" foreign key ("order_id") references "orders" ("id") on delete cascade;
alter table "order_items" add constraint "fk_order_items_product" foreign key ("product_id") references "products" ("id");
alter table "products" add constraint "fk_products_category" foreign key ("category_id") references "categories" ("id");
alter table "push_tokens" add constraint "fk_push_tokens_user" foreign key ("user_id") references "users" ("id");
alter table "sales" add constraint "fk_sales_order" foreign key ("order_id") references "orders" ("id") on delete cascade;
alter table "sale_items" add constraint "fk7tcpbc5c5mpnm8fl2phl8ep7l" foreign key ("sale_id") references "sales" ("id");
alter table "sale_items" add constraint "fk8g0sjiqs7tg055o06p6wawu39" foreign key ("product_id") references "products" ("id");
alter table "stock_movements" add constraint "fk_stock_movements_product" foreign key ("product_id") references "products" ("id");
alter table "stock_movements" add constraint "fk_stock_movements_user" foreign key ("user_id") references "users" ("id");
alter table "tax_documents" add constraint "fk_tax_documents_document_type" foreign key ("document_type_id") references "acc_document_types" ("id");
alter table "tax_documents" add constraint "fk_tax_documents_sequence" foreign key ("sequence_id") references "acc_document_sequences" ("id");
alter table "tax_document_tax_lines" add constraint "fk_tax_document_tax_lines_document" foreign key ("tax_document_id") references "tax_documents" ("id");
alter table "tax_document_transmissions" add constraint "fk_tax_document_transmissions_document" foreign key ("tax_document_id") references "tax_documents" ("id") on delete cascade;
alter table "tax_sri_settings" add constraint "fk_tax_sri_settings_tenant" foreign key ("tenant_id") references "erp_tenants" ("id");
alter table "people_counts" add constraint "fk_people_counts_tenant" foreign key ("tenant_id") references "erp_tenants" ("id");
alter table "people_counts_hourly" add constraint "fk_people_counts_hourly_tenant" foreign key ("tenant_id") references "erp_tenants" ("id");
alter table "user_preferences" add constraint "fk_user_preferences_user" foreign key ("user_id") references "users" ("id");
alter table "vision_detection_events" add constraint "fk_vision_detection_events_camera" foreign key ("camera_id") references "cameras" ("id");
alter table "vision_hourly_metrics" add constraint "fk_vision_hourly_metrics_camera" foreign key ("camera_id") references "cameras" ("id");

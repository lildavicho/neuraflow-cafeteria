-- ============================================================
-- V4: KARDEX completo + Seguridad ERP empresarial
-- ============================================================
set search_path to "bar_app", public;

-- ─── 1. Columnas de lote/serie en stock_movements ───
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "batch_code" varchar(60);
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "serial_number" varchar(60);
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "unit_cost" numeric(12,2);
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "total_cost" numeric(14,2);
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "warehouse" varchar(60) DEFAULT 'PRINCIPAL';
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "expiry_date" date;
ALTER TABLE "stock_movements" ADD COLUMN IF NOT EXISTS "document_number" varchar(60);

CREATE INDEX IF NOT EXISTS idx_stock_movement_batch ON "stock_movements" ("batch_code");
CREATE INDEX IF NOT EXISTS idx_stock_movement_warehouse ON "stock_movements" ("warehouse");
CREATE INDEX IF NOT EXISTS idx_stock_movement_type ON "stock_movements" ("type");

-- ─── 2. Columnas de lote/caducidad en products ───
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "batch_tracking" boolean DEFAULT false;
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "serial_tracking" boolean DEFAULT false;
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "reorder_point" integer DEFAULT 10;
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "max_stock" integer DEFAULT 500;
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "warehouse" varchar(60) DEFAULT 'PRINCIPAL';
ALTER TABLE "products" ADD COLUMN IF NOT EXISTS "barcode" varchar(60);

CREATE INDEX IF NOT EXISTS idx_product_barcode ON "products" ("barcode");

-- ─── 3. Tabla de auditoría de sistema ───
CREATE TABLE IF NOT EXISTS "audit_logs" (
    "id" bigserial PRIMARY KEY,
    "user_id" bigint,
    "user_email" varchar(120),
    "action" varchar(40) NOT NULL,
    "entity_type" varchar(60) NOT NULL,
    "entity_id" bigint,
    "details" text,
    "ip_address" varchar(45),
    "user_agent" varchar(500),
    "created_at" timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user ON "audit_logs" ("user_id");
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON "audit_logs" ("action");
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON "audit_logs" ("entity_type", "entity_id");
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON "audit_logs" ("created_at");

-- ─── 4. Tabla de login audit ───
CREATE TABLE IF NOT EXISTS "login_audit" (
    "id" bigserial PRIMARY KEY,
    "user_id" bigint,
    "email" varchar(120) NOT NULL,
    "action" varchar(20) NOT NULL,
    "success" boolean NOT NULL,
    "ip_address" varchar(45),
    "user_agent" varchar(500),
    "failure_reason" varchar(200),
    "created_at" timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_login_audit_email ON "login_audit" ("email");
CREATE INDEX IF NOT EXISTS idx_login_audit_created ON "login_audit" ("created_at");

-- ─── 5. Columnas de 2FA en users ───
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "totp_secret" varchar(64);
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "totp_enabled" boolean DEFAULT false;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "totp_verified" boolean DEFAULT false;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "failed_login_attempts" integer DEFAULT 0;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "locked_until" timestamp;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "last_login_at" timestamp;
ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "last_login_ip" varchar(45);

-- ─── 6. Tabla de alertas de inventario ───
CREATE TABLE IF NOT EXISTS "inventory_alerts" (
    "id" bigserial PRIMARY KEY,
    "product_id" bigint NOT NULL,
    "alert_type" varchar(30) NOT NULL,
    "message" varchar(300) NOT NULL,
    "severity" varchar(20) DEFAULT 'WARNING' NOT NULL,
    "acknowledged" boolean DEFAULT false,
    "acknowledged_by" bigint,
    "acknowledged_at" timestamp,
    "created_at" timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_alert_product ON "inventory_alerts" ("product_id");
CREATE INDEX IF NOT EXISTS idx_inventory_alert_ack ON "inventory_alerts" ("acknowledged");

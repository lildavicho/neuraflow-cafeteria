-- Composite indexes (tenant_id + fecha/estado) para acelerar queries filtradas por tenant
-- en tablas de alto volumen. Todos con IF NOT EXISTS para idempotencia.

CREATE INDEX IF NOT EXISTS idx_orders_tenant_created_at
    ON orders (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_tenant_status
    ON orders (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_sales_tenant_created_at
    ON sales (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_created_at
    ON stock_movements (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_product
    ON stock_movements (tenant_id, product_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created_at
    ON audit_logs (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_user
    ON audit_logs (tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_login_audit_tenant_created_at
    ON login_audit (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cash_registers_tenant_status
    ON cash_registers (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_products_tenant_status
    ON products (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_alerts_tenant_acknowledged
    ON inventory_alerts (tenant_id, acknowledged);

CREATE INDEX IF NOT EXISTS idx_notification_logs_tenant_sent_at
    ON notification_logs (tenant_id, sent_at DESC);

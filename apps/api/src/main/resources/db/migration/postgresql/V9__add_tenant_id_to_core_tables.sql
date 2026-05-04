-- ============================================================================
-- V7: Add tenant_id to ALL core tables for full multi-tenant data isolation
-- ============================================================================
-- Strategy:
--   1. Add tenant_id as NULLABLE
--   2. Backfill existing rows with the 'default' tenant
--   3. Set NOT NULL
--   4. Replace global unique indexes with composite (tenant_id, ...) indexes
--   5. Add FK constraints and plain indexes
-- ============================================================================

SET search_path TO "bar_app", public;

-- ── Helper: get default tenant id ──
DO $$
DECLARE
    v_default_tenant_id BIGINT;
BEGIN
    SELECT id INTO v_default_tenant_id FROM erp_tenants WHERE lower(tenant_code) = 'default' LIMIT 1;

    IF v_default_tenant_id IS NULL THEN
        INSERT INTO erp_tenants (tenant_code, legal_name, country_code, time_zone, currency_code, active, created_at, updated_at)
        VALUES ('default', 'Negocio por defecto', 'EC', 'America/Guayaquil', 'USD', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id INTO v_default_tenant_id;
    END IF;

    -- ═══════════════════════════════════════════
    -- 1. CATEGORIES
    -- ═══════════════════════════════════════════
    ALTER TABLE categories ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE categories SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE categories ALTER COLUMN tenant_id SET NOT NULL;

    -- Drop old global unique on name, create composite
    DROP INDEX IF EXISTS idx_categories_name;
    CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_tenant_name ON categories (tenant_id, name);
    CREATE INDEX IF NOT EXISTS idx_categories_tenant ON categories (tenant_id);

    -- ═══════════════════════════════════════════
    -- 2. PRODUCTS
    -- ═══════════════════════════════════════════
    ALTER TABLE products ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE products SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;

    DROP INDEX IF EXISTS idx_products_code;
    CREATE UNIQUE INDEX IF NOT EXISTS idx_products_tenant_code ON products (tenant_id, code);
    CREATE INDEX IF NOT EXISTS idx_products_tenant ON products (tenant_id);

    -- ═══════════════════════════════════════════
    -- 3. ORDERS
    -- ═══════════════════════════════════════════
    ALTER TABLE orders ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE orders SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE orders ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_orders_tenant ON orders (tenant_id);
    CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON orders (tenant_id, status);
    CREATE INDEX IF NOT EXISTS idx_orders_tenant_created ON orders (tenant_id, created_at DESC);

    -- ═══════════════════════════════════════════
    -- 4. ORDER_ITEMS (denormalize for query perf)
    -- ═══════════════════════════════════════════
    ALTER TABLE order_items ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE order_items SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE order_items ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_order_items_tenant ON order_items (tenant_id);

    -- ═══════════════════════════════════════════
    -- 5. SALES
    -- ═══════════════════════════════════════════
    ALTER TABLE sales ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE sales SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE sales ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_sales_tenant ON sales (tenant_id);

    -- ═══════════════════════════════════════════
    -- 6. SALE_ITEMS (denormalize)
    -- ═══════════════════════════════════════════
    ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE sale_items SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE sale_items ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_sale_items_tenant ON sale_items (tenant_id);

    -- ═══════════════════════════════════════════
    -- 7. STOCK_MOVEMENTS
    -- ═══════════════════════════════════════════
    ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE stock_movements SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE stock_movements ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant ON stock_movements (tenant_id);

    -- ═══════════════════════════════════════════
    -- 8. CASH_REGISTERS
    -- ═══════════════════════════════════════════
    ALTER TABLE cash_registers ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE cash_registers SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE cash_registers ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_cash_registers_tenant ON cash_registers (tenant_id);

    -- ═══════════════════════════════════════════
    -- 9. AUDIT_LOGS
    -- ═══════════════════════════════════════════
    ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE audit_logs SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON audit_logs (tenant_id);

    -- ═══════════════════════════════════════════
    -- 10. LOGIN_AUDIT
    -- ═══════════════════════════════════════════
    ALTER TABLE login_audit ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE login_audit SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE login_audit ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_login_audit_tenant ON login_audit (tenant_id);

    -- ═══════════════════════════════════════════
    -- 11. LOYALTY_POINTS
    -- ═══════════════════════════════════════════
    ALTER TABLE loyalty_points ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE loyalty_points SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE loyalty_points ALTER COLUMN tenant_id SET NOT NULL;

    -- Change unique on user_id to composite
    DROP INDEX IF EXISTS idx_loyalty_points_user_id;
    CREATE UNIQUE INDEX IF NOT EXISTS idx_loyalty_points_tenant_user ON loyalty_points (tenant_id, user_id);
    CREATE INDEX IF NOT EXISTS idx_loyalty_points_tenant ON loyalty_points (tenant_id);

    -- ═══════════════════════════════════════════
    -- 12. LOYALTY_LEDGER
    -- ═══════════════════════════════════════════
    ALTER TABLE loyalty_ledger ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE loyalty_ledger SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE loyalty_ledger ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_loyalty_ledger_tenant ON loyalty_ledger (tenant_id);

    -- ═══════════════════════════════════════════
    -- 13. CAMERAS
    -- ═══════════════════════════════════════════
    ALTER TABLE cameras ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE cameras SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE cameras ALTER COLUMN tenant_id SET NOT NULL;

    DROP INDEX IF EXISTS idx_cameras_name;
    CREATE UNIQUE INDEX IF NOT EXISTS idx_cameras_tenant_name ON cameras (tenant_id, name);
    CREATE INDEX IF NOT EXISTS idx_cameras_tenant ON cameras (tenant_id);

    -- ═══════════════════════════════════════════
    -- 14. SETTINGS
    -- ═══════════════════════════════════════════
    ALTER TABLE settings ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE settings SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE settings ALTER COLUMN tenant_id SET NOT NULL;

    DROP INDEX IF EXISTS idx_settings_key;
    CREATE UNIQUE INDEX IF NOT EXISTS idx_settings_tenant_key ON settings (tenant_id, setting_key);
    CREATE INDEX IF NOT EXISTS idx_settings_tenant ON settings (tenant_id);

    -- ═══════════════════════════════════════════
    -- 15. NOTIFICATION_LOGS
    -- ═══════════════════════════════════════════
    ALTER TABLE notification_logs ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE notification_logs SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE notification_logs ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_notification_logs_tenant ON notification_logs (tenant_id);

    -- ═══════════════════════════════════════════
    -- 16. PUSH_TOKENS
    -- ═══════════════════════════════════════════
    ALTER TABLE push_tokens ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE push_tokens SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE push_tokens ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_push_tokens_tenant ON push_tokens (tenant_id);

    -- ═══════════════════════════════════════════
    -- 17. INVENTORY_ALERTS
    -- ═══════════════════════════════════════════
    ALTER TABLE inventory_alerts ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE inventory_alerts SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE inventory_alerts ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_inventory_alerts_tenant ON inventory_alerts (tenant_id);

    -- ═══════════════════════════════════════════
    -- 18. PEOPLE_COUNTS
    -- ═══════════════════════════════════════════
    ALTER TABLE people_counts ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE people_counts SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE people_counts ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_people_counts_tenant ON people_counts (tenant_id);

    -- ═══════════════════════════════════════════
    -- 19. PEOPLE_COUNTS_HOURLY
    -- ═══════════════════════════════════════════
    ALTER TABLE people_counts_hourly ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE people_counts_hourly SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE people_counts_hourly ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_people_counts_hourly_tenant ON people_counts_hourly (tenant_id);
    DELETE FROM people_counts_hourly a
    USING people_counts_hourly b
    WHERE a.tenant_id = b.tenant_id
      AND a.hour_start = b.hour_start
      AND a.id < b.id;
    CREATE UNIQUE INDEX IF NOT EXISTS uk_people_counts_hourly_tenant_hour ON people_counts_hourly (tenant_id, hour_start);

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_people_counts_tenant') THEN
        ALTER TABLE people_counts ADD CONSTRAINT fk_people_counts_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_people_counts_hourly_tenant') THEN
        ALTER TABLE people_counts_hourly ADD CONSTRAINT fk_people_counts_hourly_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;

    -- ═══════════════════════════════════════════
    -- 20. ML_PREDICTIONS
    -- ═══════════════════════════════════════════
    ALTER TABLE ml_predictions ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE ml_predictions SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE ml_predictions ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_ml_predictions_tenant ON ml_predictions (tenant_id);

    -- ═══════════════════════════════════════════
    -- 21. MODEL_METRICS
    -- ═══════════════════════════════════════════
    ALTER TABLE model_metrics ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE model_metrics SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE model_metrics ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_model_metrics_tenant ON model_metrics (tenant_id);

    -- ═══════════════════════════════════════════
    -- 22. VISION_DETECTION_EVENTS
    -- ═══════════════════════════════════════════
    ALTER TABLE vision_detection_events ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE vision_detection_events SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE vision_detection_events ALTER COLUMN tenant_id SET NOT NULL;

    DROP INDEX IF EXISTS uk_vision_detection_events_camera_event;
    CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_detection_events_tenant_camera_event ON vision_detection_events (tenant_id, camera_id, event_key);
    CREATE INDEX IF NOT EXISTS idx_vision_detection_events_tenant ON vision_detection_events (tenant_id);

    -- ═══════════════════════════════════════════
    -- 23. VISION_HOURLY_METRICS
    -- ═══════════════════════════════════════════
    ALTER TABLE vision_hourly_metrics ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE vision_hourly_metrics SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE vision_hourly_metrics ALTER COLUMN tenant_id SET NOT NULL;

    DROP INDEX IF EXISTS uk_vision_hourly_metrics_camera_hour;
    CREATE UNIQUE INDEX IF NOT EXISTS uk_vision_hourly_metrics_tenant_camera_hour ON vision_hourly_metrics (tenant_id, camera_id, hour_start);
    CREATE INDEX IF NOT EXISTS idx_vision_hourly_metrics_tenant ON vision_hourly_metrics (tenant_id);

    -- ═══════════════════════════════════════════
    -- 24. USER_PREFERENCES
    -- ═══════════════════════════════════════════
    ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
    UPDATE user_preferences SET tenant_id = v_default_tenant_id WHERE tenant_id IS NULL;
    ALTER TABLE user_preferences ALTER COLUMN tenant_id SET NOT NULL;

    CREATE INDEX IF NOT EXISTS idx_user_preferences_tenant ON user_preferences (tenant_id);

END $$;

-- ═══════════════════════════════════════════════
-- FK CONSTRAINTS (outside DO block for cleaner error handling)
-- ═══════════════════════════════════════════════
DO $$
BEGIN
    -- categories
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_tenant') THEN
        ALTER TABLE categories ADD CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- products
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_tenant') THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- orders
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_tenant') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- order_items
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_tenant') THEN
        ALTER TABLE order_items ADD CONSTRAINT fk_order_items_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- sales
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sales_tenant') THEN
        ALTER TABLE sales ADD CONSTRAINT fk_sales_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- sale_items
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sale_items_tenant') THEN
        ALTER TABLE sale_items ADD CONSTRAINT fk_sale_items_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- stock_movements
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stock_movements_tenant') THEN
        ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- cash_registers
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cash_registers_tenant') THEN
        ALTER TABLE cash_registers ADD CONSTRAINT fk_cash_registers_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- audit_logs
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_logs_tenant') THEN
        ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- login_audit
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_login_audit_tenant') THEN
        ALTER TABLE login_audit ADD CONSTRAINT fk_login_audit_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- loyalty_points
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_loyalty_points_tenant') THEN
        ALTER TABLE loyalty_points ADD CONSTRAINT fk_loyalty_points_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- loyalty_ledger
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_loyalty_ledger_tenant') THEN
        ALTER TABLE loyalty_ledger ADD CONSTRAINT fk_loyalty_ledger_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- cameras
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cameras_tenant') THEN
        ALTER TABLE cameras ADD CONSTRAINT fk_cameras_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- settings
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_settings_tenant') THEN
        ALTER TABLE settings ADD CONSTRAINT fk_settings_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- notification_logs
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notification_logs_tenant') THEN
        ALTER TABLE notification_logs ADD CONSTRAINT fk_notification_logs_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- push_tokens
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_push_tokens_tenant') THEN
        ALTER TABLE push_tokens ADD CONSTRAINT fk_push_tokens_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- inventory_alerts
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_alerts_tenant') THEN
        ALTER TABLE inventory_alerts ADD CONSTRAINT fk_inventory_alerts_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- people_counts
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_people_counts_tenant') THEN
        ALTER TABLE people_counts ADD CONSTRAINT fk_people_counts_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- people_counts_hourly
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_people_counts_hourly_tenant') THEN
        ALTER TABLE people_counts_hourly ADD CONSTRAINT fk_people_counts_hourly_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- ml_predictions
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ml_predictions_tenant') THEN
        ALTER TABLE ml_predictions ADD CONSTRAINT fk_ml_predictions_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- model_metrics
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_model_metrics_tenant') THEN
        ALTER TABLE model_metrics ADD CONSTRAINT fk_model_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- vision_detection_events
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_vision_detection_events_tenant') THEN
        ALTER TABLE vision_detection_events ADD CONSTRAINT fk_vision_detection_events_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- vision_hourly_metrics
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_vision_hourly_metrics_tenant') THEN
        ALTER TABLE vision_hourly_metrics ADD CONSTRAINT fk_vision_hourly_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
    -- user_preferences
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_preferences_tenant') THEN
        ALTER TABLE user_preferences ADD CONSTRAINT fk_user_preferences_tenant FOREIGN KEY (tenant_id) REFERENCES erp_tenants (id);
    END IF;
END $$;

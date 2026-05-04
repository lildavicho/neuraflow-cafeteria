CREATE TABLE IF NOT EXISTS dashboard_snapshot_cache (
    tenant_id BIGINT PRIMARY KEY REFERENCES erp_tenants(id) ON DELETE CASCADE,
    tenant_code VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    computed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dashboard_snapshot_cache_expires_at
    ON dashboard_snapshot_cache (expires_at);

CREATE INDEX IF NOT EXISTS idx_dashboard_snapshot_cache_tenant_code
    ON dashboard_snapshot_cache (lower(tenant_code));

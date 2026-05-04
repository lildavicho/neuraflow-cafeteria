-- V23: Agrega numeracion interna y vinculo a motor de aprobaciones en compras
ALTER TABLE erp_purchases
    ADD COLUMN IF NOT EXISTS branch_id BIGINT,
    ADD COLUMN IF NOT EXISTS internal_number VARCHAR(40),
    ADD COLUMN IF NOT EXISTS approval_instance_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_erp_purchases_internal_number
    ON erp_purchases (tenant_id, internal_number);

CREATE INDEX IF NOT EXISTS idx_erp_purchases_branch
    ON erp_purchases (tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_erp_purchases_approval_instance
    ON erp_purchases (approval_instance_id);

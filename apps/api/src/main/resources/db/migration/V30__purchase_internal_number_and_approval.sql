ALTER TABLE erp_purchases
    ADD COLUMN branch_id BIGINT NULL,
    ADD COLUMN internal_number VARCHAR(40) NULL,
    ADD COLUMN approval_instance_id BIGINT NULL;

CREATE INDEX idx_erp_purchases_internal_number
    ON erp_purchases (tenant_id, internal_number);

CREATE INDEX idx_erp_purchases_branch
    ON erp_purchases (tenant_id, branch_id);

CREATE INDEX idx_erp_purchases_approval_instance
    ON erp_purchases (approval_instance_id);

-- V19: Motor de secuencias de propósito general por sucursal
-- Complementa acc_document_sequences (SRI) con secuencias internas configurables

CREATE TABLE IF NOT EXISTS erp_sequences (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES erp_tenants(id) ON DELETE CASCADE,
    branch_id       BIGINT       REFERENCES erp_branches(id) ON DELETE SET NULL,
    code            VARCHAR(60)  NOT NULL,  -- e.g. PURCHASE_ORDER, INTERNAL_RECEIPT, ADJUSTMENT
    name            VARCHAR(200) NOT NULL,
    prefix          VARCHAR(20),            -- e.g. OC-, AJ-, RC-
    suffix          VARCHAR(20),
    pad_length      INT          NOT NULL DEFAULT 7,  -- zero-padding length
    current_number  BIGINT       NOT NULL DEFAULT 0,
    increment_step  INT          NOT NULL DEFAULT 1,
    reset_policy    VARCHAR(20)  NOT NULL DEFAULT 'NEVER',  -- NEVER, YEARLY, MONTHLY
    last_reset_at   TIMESTAMP,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_erp_sequence_tenant_branch_code UNIQUE (tenant_id, branch_id, code)
);

CREATE INDEX IF NOT EXISTS idx_erp_sequence_tenant ON erp_sequences(tenant_id);
CREATE INDEX IF NOT EXISTS idx_erp_sequence_branch ON erp_sequences(tenant_id, branch_id);

-- Seed default sequences for existing tenants (global, no branch-specific)
INSERT INTO erp_sequences (tenant_id, code, name, prefix, pad_length, current_number)
SELECT t.id, 'PURCHASE_ORDER', 'Orden de Compra', 'OC-', 7, 0
FROM erp_tenants t
ON CONFLICT DO NOTHING;

INSERT INTO erp_sequences (tenant_id, code, name, prefix, pad_length, current_number)
SELECT t.id, 'INTERNAL_RECEIPT', 'Recibo Interno', 'RI-', 7, 0
FROM erp_tenants t
ON CONFLICT DO NOTHING;

INSERT INTO erp_sequences (tenant_id, code, name, prefix, pad_length, current_number)
SELECT t.id, 'INVENTORY_ADJUST', 'Ajuste de Inventario', 'AJ-', 7, 0
FROM erp_tenants t
ON CONFLICT DO NOTHING;

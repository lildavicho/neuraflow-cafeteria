-- V18: Motor de aprobaciones configurable por tenant
-- Permite definir flujos de aprobación multi-paso para cualquier entidad del ERP

-- Definición de flujos de aprobación (plantilla)
CREATE TABLE IF NOT EXISTS erp_approval_definitions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES erp_tenants(id) ON DELETE CASCADE,
    code            VARCHAR(60)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(60)  NOT NULL,  -- PURCHASE_ORDER, CREDIT_NOTE, INVENTORY_ADJUST, DISCOUNT, etc.
    condition_expr  TEXT,                    -- expresión SpEL o JSON para evaluar cuándo aplica (e.g., "amount > 500")
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_approval_def_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_approval_def_tenant ON erp_approval_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_approval_def_entity ON erp_approval_definitions(tenant_id, entity_type);

-- Pasos de aprobación (en orden)
CREATE TABLE IF NOT EXISTS erp_approval_steps (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES erp_tenants(id) ON DELETE CASCADE,
    definition_id   BIGINT       NOT NULL REFERENCES erp_approval_definitions(id) ON DELETE CASCADE,
    step_order      INT          NOT NULL DEFAULT 1,
    name            VARCHAR(200) NOT NULL,
    approver_role   VARCHAR(32)  NOT NULL,  -- UserRole: ADMIN, CASHIER, BUYER, etc.
    required_count  INT          NOT NULL DEFAULT 1,  -- cuántas aprobaciones se necesitan en este paso
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_approval_step_order UNIQUE (definition_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_approval_step_def ON erp_approval_steps(definition_id);

-- Instancias de aprobación (una por entidad que entra en flujo)
CREATE TABLE IF NOT EXISTS erp_approval_instances (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES erp_tenants(id) ON DELETE CASCADE,
    definition_id   BIGINT       NOT NULL REFERENCES erp_approval_definitions(id),
    entity_type     VARCHAR(60)  NOT NULL,
    entity_id       BIGINT       NOT NULL,
    current_step    INT          NOT NULL DEFAULT 1,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED, CANCELLED
    requested_by    BIGINT,      -- user id del solicitante
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_approval_instance UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_approval_inst_tenant ON erp_approval_instances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_approval_inst_status ON erp_approval_instances(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_approval_inst_entity ON erp_approval_instances(tenant_id, entity_type, entity_id);

-- Acciones de aprobación (historial de decisiones)
CREATE TABLE IF NOT EXISTS erp_approval_actions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES erp_tenants(id) ON DELETE CASCADE,
    instance_id     BIGINT       NOT NULL REFERENCES erp_approval_instances(id) ON DELETE CASCADE,
    step_order      INT          NOT NULL,
    action          VARCHAR(20)  NOT NULL,  -- APPROVE, REJECT, COMMENT
    user_id         BIGINT,
    user_name       VARCHAR(200),
    comment         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_action_inst ON erp_approval_actions(instance_id);

-- Permisos para aprobaciones
INSERT INTO permissions (code, description, module_code) VALUES
  ('approvals:read',      'Ver bandeja de aprobaciones',      'approvals'),
  ('approvals:approve',   'Aprobar solicitudes',              'approvals'),
  ('approvals:reject',    'Rechazar solicitudes',             'approvals'),
  ('approvals:configure', 'Configurar flujos de aprobación',  'approvals')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: todos los permisos de aprobaciones
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'ADMIN', p.id, NULL
FROM permissions p
WHERE p.module_code = 'approvals'
ON CONFLICT DO NOTHING;

-- BUYER: puede ver y aprobar (compras)
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'BUYER', p.id, NULL
FROM permissions p
WHERE p.code IN ('approvals:read', 'approvals:approve', 'approvals:reject')
ON CONFLICT DO NOTHING;

-- CASHIER: solo lectura de aprobaciones
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'CASHIER', p.id, NULL
FROM permissions p
WHERE p.code = 'approvals:read'
ON CONFLICT DO NOTHING;

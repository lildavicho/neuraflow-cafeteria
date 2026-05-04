-- V10: Sistema de permisos granulares por rol y tenant
-- Permite definir qué acciones puede ejecutar cada rol en cada módulo,
-- con posibilidad de overrides por tenant (tenant_id NULL = permiso default global).

CREATE TABLE IF NOT EXISTS permissions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(100) NOT NULL UNIQUE,
    description   VARCHAR(255),
    module_code   VARCHAR(60)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module_code);

CREATE TABLE IF NOT EXISTS role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role          VARCHAR(32) NOT NULL,
    permission_id BIGINT      NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    tenant_id     BIGINT      NULL     REFERENCES erp_tenants(id) ON DELETE CASCADE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_permissions UNIQUE (role, permission_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role     ON role_permissions(role);
CREATE INDEX IF NOT EXISTS idx_role_permissions_tenant   ON role_permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_lookup   ON role_permissions(role, tenant_id);

-- ---------------------------------------------------------------------------
-- Seed de permisos base
-- Patrón: <module>:<action>
-- Acciones estándar: read, create, update, delete, export
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, description, module_code) VALUES
  ('products:read',      'Ver catálogo de productos',        'products'),
  ('products:create',    'Crear productos',                  'products'),
  ('products:update',    'Actualizar productos',             'products'),
  ('products:delete',    'Eliminar productos',               'products'),
  ('products:export',    'Exportar catálogo',                'products'),

  ('categories:read',    'Ver categorías',                   'categories'),
  ('categories:manage',  'Gestionar categorías',             'categories'),

  ('orders:read',        'Ver pedidos',                      'orders'),
  ('orders:create',      'Crear pedidos',                    'orders'),
  ('orders:update',      'Editar pedidos',                   'orders'),
  ('orders:cancel',      'Cancelar pedidos',                 'orders'),
  ('orders:pay',         'Cobrar pedidos',                   'orders'),
  ('orders:refund',      'Aplicar reembolsos',               'orders'),

  ('inventory:read',     'Ver inventario y kardex',          'inventory'),
  ('inventory:move',     'Registrar movimientos de stock',   'inventory'),
  ('inventory:adjust',   'Ajustes manuales de inventario',   'inventory'),
  ('inventory:alerts',   'Gestionar alertas de stock',       'inventory'),

  ('purchases:read',     'Ver órdenes de compra',            'purchases'),
  ('purchases:create',   'Crear órdenes de compra',          'purchases'),
  ('purchases:approve',  'Aprobar órdenes de compra',        'purchases'),

  ('cash:read',          'Ver cajas',                        'cash'),
  ('cash:open',          'Abrir caja',                       'cash'),
  ('cash:close',         'Cerrar caja',                      'cash'),

  ('accounting:read',    'Ver contabilidad',                 'accounting'),
  ('accounting:manage',  'Gestionar CxC/CxP y asientos',     'accounting'),

  ('reports:read',       'Ver reportes',                     'reports'),
  ('reports:export',     'Exportar reportes',                'reports'),

  ('customers:read',     'Ver clientes',                     'customers'),
  ('customers:manage',   'Gestionar clientes',               'customers'),

  ('loyalty:read',       'Ver puntos de fidelidad',          'loyalty'),
  ('loyalty:manage',     'Gestionar puntos y canjes',        'loyalty'),

  ('users:read',         'Ver usuarios',                     'users'),
  ('users:manage',       'Gestionar usuarios y permisos',    'users'),

  ('settings:read',      'Ver configuración',                'settings'),
  ('settings:manage',    'Modificar configuración',          'settings'),

  ('audit:read',         'Ver auditoría',                    'audit'),

  ('vision:read',        'Ver módulo de visión por IA',      'vision'),
  ('vision:manage',      'Configurar cámaras y detecciones', 'vision'),

  ('analytics:read',     'Ver analíticas y dashboard',       'analytics')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Asignación default por rol (tenant_id NULL = aplica a todos los tenants)
-- ADMIN: acceso total
-- ---------------------------------------------------------------------------
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'ADMIN', p.id, NULL FROM permissions p
ON CONFLICT DO NOTHING;

-- CASHIER: operación de caja y pedidos
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'CASHIER', p.id, NULL
FROM permissions p
WHERE p.code IN (
  'products:read','categories:read',
  'orders:read','orders:create','orders:update','orders:pay',
  'cash:read','cash:open','cash:close',
  'customers:read','customers:manage',
  'loyalty:read','loyalty:manage',
  'reports:read','analytics:read',
  'inventory:read'
)
ON CONFLICT DO NOTHING;

-- BUYER: compras e inventario
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'BUYER', p.id, NULL
FROM permissions p
WHERE p.code IN (
  'products:read','products:create','products:update','products:export',
  'categories:read','categories:manage',
  'inventory:read','inventory:move','inventory:adjust','inventory:alerts',
  'purchases:read','purchases:create','purchases:approve',
  'reports:read','reports:export',
  'analytics:read'
)
ON CONFLICT DO NOTHING;

-- CUSTOMER: sólo lectura de su propio catálogo y puntos
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'CUSTOMER', p.id, NULL
FROM permissions p
WHERE p.code IN (
  'products:read','categories:read',
  'orders:read',
  'loyalty:read'
)
ON CONFLICT DO NOTHING;

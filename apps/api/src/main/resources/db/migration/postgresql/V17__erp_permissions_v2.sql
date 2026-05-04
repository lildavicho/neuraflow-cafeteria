-- V17: Permisos granulares para módulos ERP nuevos (branches, warehouses, parties)
-- Sigue el patrón <module>:<action> establecido en V10

INSERT INTO permissions (code, description, module_code) VALUES
  -- Branches (sucursales)
  ('branches:read',     'Ver sucursales',                   'branches'),
  ('branches:create',   'Crear sucursales',                 'branches'),
  ('branches:update',   'Actualizar sucursales',            'branches'),
  ('branches:delete',   'Desactivar sucursales',            'branches'),

  -- Warehouses (bodegas)
  ('warehouses:read',   'Ver bodegas',                      'warehouses'),
  ('warehouses:create', 'Crear bodegas',                    'warehouses'),
  ('warehouses:update', 'Actualizar bodegas',               'warehouses'),
  ('warehouses:delete', 'Desactivar bodegas',               'warehouses'),

  -- Parties (contactos: clientes, proveedores, empleados, estudiantes)
  ('parties:read',      'Ver contactos',                    'parties'),
  ('parties:create',    'Crear contactos',                  'parties'),
  ('parties:update',    'Actualizar contactos',             'parties'),
  ('parties:delete',    'Desactivar contactos',             'parties'),
  ('parties:roles',     'Gestionar roles de contactos',     'parties')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: acceso total a nuevos permisos
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'ADMIN', p.id, NULL
FROM permissions p
WHERE p.module_code IN ('branches', 'warehouses', 'parties')
ON CONFLICT DO NOTHING;

-- CASHIER: lectura de sucursales/bodegas/contactos + crear/actualizar contactos
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'CASHIER', p.id, NULL
FROM permissions p
WHERE p.code IN (
  'branches:read',
  'warehouses:read',
  'parties:read', 'parties:create', 'parties:update'
)
ON CONFLICT DO NOTHING;

-- BUYER: lectura de sucursales/bodegas + gestión de contactos (proveedores)
INSERT INTO role_permissions (role, permission_id, tenant_id)
SELECT 'BUYER', p.id, NULL
FROM permissions p
WHERE p.code IN (
  'branches:read',
  'warehouses:read',
  'parties:read', 'parties:create', 'parties:update'
)
ON CONFLICT DO NOTHING;

-- CUSTOMER: solo lectura de su propio perfil (sin acceso a parties/branches/warehouses)

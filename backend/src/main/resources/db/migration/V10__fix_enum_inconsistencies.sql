-- ========================================
-- V10: Corregir inconsistencias de enums
-- ========================================

-- 1. Corregir payment_status legado a valores válidos
UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status IN ('PAYMENT_IN_PROGRESS', 'PAYMENT_PENDING_CONF', 'PENDING_PAYMENT_CASH');

UPDATE orders
SET payment_status = 'PAID'
WHERE payment_status NOT IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED');

-- 2. Corregir order status si hay valores fuera del enum
UPDATE orders
SET status = 'DELIVERED'
WHERE status NOT IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'DELIVERED', 'COMPLETED', 'CANCELLED');

-- 3. Re-verificar encoding UTF-8 en columnas de texto
ALTER TABLE categories
  MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE products
  MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. Actualizar conexión por defecto
ALTER DATABASE ucacue_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 5. Crear índices (recrear si ya existen)
SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND index_name = 'idx_orders_status'
);
SET @sql := IF(@idx_exists > 0, 'DROP INDEX idx_orders_status ON orders', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
CREATE INDEX idx_orders_status ON orders(status);

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND index_name = 'idx_orders_payment_status'
);
SET @sql := IF(@idx_exists > 0, 'DROP INDEX idx_orders_payment_status ON orders', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
CREATE INDEX idx_orders_payment_status ON orders(payment_status);

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'orders'
    AND index_name = 'idx_orders_created_at'
);
SET @sql := IF(@idx_exists > 0, 'DROP INDEX idx_orders_created_at ON orders', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
CREATE INDEX idx_orders_created_at ON orders(created_at);

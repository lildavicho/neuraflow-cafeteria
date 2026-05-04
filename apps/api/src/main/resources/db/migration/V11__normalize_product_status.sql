-- ========================================
-- V11: Normalizar status de productos
-- ========================================

UPDATE products
SET status = 'AVAILABLE'
WHERE status = 'ACTIVE';

UPDATE products
SET status = 'AVAILABLE'
WHERE status IS NULL
   OR status NOT IN ('AVAILABLE', 'SOLD_OUT', 'DISCONTINUED');

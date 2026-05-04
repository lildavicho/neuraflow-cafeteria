-- Agregar campo 'prepared' para indicar si el producto aparece en POS
ALTER TABLE products ADD COLUMN prepared TINYINT(1) NOT NULL DEFAULT 1 AFTER status;

-- Actualizar productos existentes
-- Ingredientes (category_id = 6) NO deben aparecer en POS
UPDATE products SET prepared = 0 WHERE category_id = 6;

-- Todos los demás productos SÍ aparecen en POS
UPDATE products SET prepared = 1 WHERE category_id != 6;

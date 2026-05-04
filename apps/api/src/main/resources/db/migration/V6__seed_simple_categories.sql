-- Seed simplified categories for minimal product creation
INSERT INTO categories (name, description, active) VALUES
  ('Desayunos', 'Desayunos', 1),
  ('Almuerzos', 'Almuerzos', 1),
  ('Snacks', 'Snacks', 1),
  ('Bebidas', 'Bebidas', 1),
  ('Postres', 'Postres', 1),
  ('Ingredientes', 'Leche, azúcar, siropes (no aparecen en POS)', 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), active = VALUES(active);

-- Optional: deactivate legacy beverage categories to avoid duplicates in UI
UPDATE categories SET active = 0 WHERE name IN ('Bebidas Calientes','Bebidas Frías');

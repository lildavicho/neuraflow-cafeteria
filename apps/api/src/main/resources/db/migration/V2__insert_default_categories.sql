-- Insertar categorías por defecto para la cafetería
INSERT INTO categories (name, description, active) VALUES
('Bebidas Calientes', 'Café, té, chocolate caliente', 1),
('Bebidas Frías', 'Jugos, batidos, refrescos', 1),
('Postres', 'Pasteles, galletas, brownies', 1),
('Snacks', 'Papas, nachos, bocaditos', 1),
('Desayunos', 'Sándwiches, wraps, bagels', 1),
('Ingredientes', 'Leche, azúcar, siropes (no aparecen en POS)', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

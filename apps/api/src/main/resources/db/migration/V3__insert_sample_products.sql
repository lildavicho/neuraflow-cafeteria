-- Productos de ejemplo para la cafetería
-- Bebidas Calientes (category_id = 1)
INSERT INTO products (category_id, code, name, description, image_url, unit, price, purchase_price, stock, min_stock, status) VALUES
(1, 'CAFE-001', 'Café Americano', 'Café negro clásico', NULL, 'UNIDAD', 2.50, 0.80, 100, 10, 'ACTIVE'),
(1, 'CAFE-002', 'Café Latte', 'Café con leche espumosa', NULL, 'UNIDAD', 3.50, 1.20, 100, 10, 'ACTIVE'),
(1, 'CAFE-003', 'Cappuccino', 'Espresso con espuma de leche', NULL, 'UNIDAD', 3.75, 1.30, 100, 10, 'ACTIVE'),
(1, 'CAFE-004', 'Mocha', 'Café con chocolate', NULL, 'UNIDAD', 4.00, 1.50, 100, 10, 'ACTIVE'),
(1, 'TE-001', 'Té Verde', 'Té verde natural', NULL, 'UNIDAD', 2.00, 0.60, 80, 10, 'ACTIVE'),
(1, 'TE-002', 'Té Negro', 'Té negro aromático', NULL, 'UNIDAD', 2.00, 0.60, 80, 10, 'ACTIVE'),

-- Bebidas Frías (category_id = 2)
(2, 'JUG-001', 'Jugo de Naranja', 'Jugo natural de naranja', NULL, 'UNIDAD', 3.00, 1.00, 50, 10, 'ACTIVE'),
(2, 'JUG-002', 'Limonada', 'Limonada natural', NULL, 'UNIDAD', 2.50, 0.80, 50, 10, 'ACTIVE'),
(2, 'BAT-001', 'Batido de Fresa', 'Batido cremoso de fresa', NULL, 'UNIDAD', 4.50, 1.80, 40, 5, 'ACTIVE'),
(2, 'BAT-002', 'Batido de Mango', 'Batido tropical de mango', NULL, 'UNIDAD', 4.50, 1.80, 40, 5, 'ACTIVE'),
(2, 'FRAP-001', 'Frappé de Café', 'Café helado batido', NULL, 'UNIDAD', 4.75, 1.90, 60, 10, 'ACTIVE'),

-- Postres (category_id = 3)
(3, 'POST-001', 'Brownie', 'Brownie de chocolate', NULL, 'UNIDAD', 2.50, 1.00, 30, 5, 'ACTIVE'),
(3, 'POST-002', 'Cheesecake', 'Pastel de queso', NULL, 'UNIDAD', 3.50, 1.50, 20, 3, 'ACTIVE'),
(3, 'POST-003', 'Galletas Chips', 'Galletas con chispas de chocolate', NULL, 'UNIDAD', 1.50, 0.50, 50, 10, 'ACTIVE'),
(3, 'POST-004', 'Muffin Arándanos', 'Muffin de arándanos', NULL, 'UNIDAD', 2.00, 0.80, 40, 5, 'ACTIVE'),

-- Snacks (category_id = 4)
(4, 'SNK-001', 'Papas Fritas', 'Papas fritas saladas', NULL, 'UNIDAD', 1.50, 0.50, 100, 20, 'ACTIVE'),
(4, 'SNK-002', 'Nachos', 'Nachos con queso', NULL, 'UNIDAD', 3.00, 1.20, 50, 10, 'ACTIVE'),
(4, 'SNK-003', 'Pretzels', 'Pretzels salados', NULL, 'UNIDAD', 1.75, 0.60, 80, 15, 'ACTIVE'),

-- Desayunos (category_id = 5)
(5, 'DES-001', 'Sándwich Jamón Queso', 'Sándwich caliente de jamón y queso', NULL, 'UNIDAD', 4.00, 1.80, 30, 5, 'ACTIVE'),
(5, 'DES-002', 'Wrap Pollo', 'Wrap de pollo con vegetales', NULL, 'UNIDAD', 5.00, 2.20, 25, 5, 'ACTIVE'),
(5, 'DES-003', 'Bagel Cream Cheese', 'Bagel con queso crema', NULL, 'UNIDAD', 3.00, 1.20, 40, 8, 'ACTIVE'),
(5, 'DES-004', 'Croissant', 'Croissant de mantequilla', NULL, 'UNIDAD', 2.50, 1.00, 35, 5, 'ACTIVE'),

-- Ingredientes (category_id = 6) - NO aparecen en POS
(6, 'ING-001', 'Leche Entera', 'Leche entera 1L', NULL, 'LITRO', 1.50, 0.80, 50, 10, 'ACTIVE'),
(6, 'ING-002', 'Azúcar', 'Azúcar blanca 1kg', NULL, 'KILOGRAMO', 2.00, 1.00, 100, 20, 'ACTIVE'),
(6, 'ING-003', 'Sirope Vainilla', 'Sirope sabor vainilla', NULL, 'UNIDAD', 5.00, 3.00, 20, 5, 'ACTIVE'),
(6, 'ING-004', 'Sirope Caramelo', 'Sirope sabor caramelo', NULL, 'UNIDAD', 5.00, 3.00, 20, 5, 'ACTIVE')
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- V8: Seed admin user and sample products for system initialization
-- Password hash for 'admin123' using BCrypt (cost factor 10)

-- Insert admin user
INSERT INTO users (name, email, password_hash, phone, role, active) VALUES
  ('Administrador', 'admin@ucacue.edu.ec', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQr7LxMOONQ4lPbGxwLZSQhv6k8HUOO', '0999999999', 'ADMIN', 1),
  ('Cajero Demo', 'cajero@ucacue.edu.ec', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQr7LxMOONQ4lPbGxwLZSQhv6k8HUOO', '0988888888', 'CASHIER', 1),
  ('Cliente Demo', 'cliente@ucacue.edu.ec', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQr7LxMOONQ4lPbGxwLZSQhv6k8HUOO', '0977777777', 'CUSTOMER', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert sample products for each category
-- Desayunos (category_id = 1)
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, status, prepared) VALUES
  (1, 'DES-001', 'Desayuno Americano', 'Huevos, pan, jugo y café', 'unidad', 4.50, 2.50, 50, 'ACTIVE', 1),
  (1, 'DES-002', 'Bolón de Verde', 'Bolón con queso o chicharrón', 'unidad', 3.00, 1.50, 40, 'ACTIVE', 1),
  (1, 'DES-003', 'Empanadas (3 unidades)', 'Empanadas de queso o carne', 'porción', 2.50, 1.00, 60, 'ACTIVE', 1),
  (1, 'DES-004', 'Tostadas con Mermelada', 'Pan tostado con mermelada y mantequilla', 'unidad', 2.00, 0.80, 30, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Almuerzos (category_id = 2)
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, status, prepared) VALUES
  (2, 'ALM-001', 'Almuerzo Ejecutivo', 'Sopa, segundo, jugo y postre', 'unidad', 3.50, 2.00, 100, 'ACTIVE', 1),
  (2, 'ALM-002', 'Seco de Pollo', 'Arroz, pollo, menestra y ensalada', 'unidad', 4.00, 2.20, 80, 'ACTIVE', 1),
  (2, 'ALM-003', 'Chaulafán', 'Arroz frito con pollo y vegetales', 'unidad', 3.50, 1.80, 60, 'ACTIVE', 1),
  (2, 'ALM-004', 'Arroz con Menestra y Carne', 'Arroz, menestra de lenteja y bistec', 'unidad', 4.50, 2.50, 70, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Snacks (category_id = 3)
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, status, prepared) VALUES
  (3, 'SNK-001', 'Papas Fritas', 'Porción de papas fritas', 'porción', 1.50, 0.50, 100, 'ACTIVE', 1),
  (3, 'SNK-002', 'Hamburguesa Clásica', 'Hamburguesa con queso, lechuga y tomate', 'unidad', 3.00, 1.50, 50, 'ACTIVE', 1),
  (3, 'SNK-003', 'Hot Dog', 'Salchicha con pan y salsas', 'unidad', 2.00, 0.80, 60, 'ACTIVE', 1),
  (3, 'SNK-004', 'Nachos con Queso', 'Nachos con salsa de queso', 'porción', 2.50, 1.00, 40, 'ACTIVE', 1),
  (3, 'SNK-005', 'Sanduche de Pollo', 'Sanduche con pechuga de pollo', 'unidad', 2.50, 1.20, 45, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Bebidas (category_id = 4)
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, status, prepared) VALUES
  (4, 'BEB-001', 'Café Americano', 'Café negro tradicional', 'unidad', 1.00, 0.30, 200, 'ACTIVE', 1),
  (4, 'BEB-002', 'Café con Leche', 'Café con leche caliente', 'unidad', 1.25, 0.40, 180, 'ACTIVE', 1),
  (4, 'BEB-003', 'Capuchino', 'Café espresso con espuma de leche', 'unidad', 1.75, 0.60, 150, 'ACTIVE', 1),
  (4, 'BEB-004', 'Agua Mineral', 'Botella de agua 500ml', 'unidad', 0.75, 0.30, 300, 'ACTIVE', 0),
  (4, 'BEB-005', 'Gaseosa', 'Coca-Cola, Sprite o Fanta', 'unidad', 1.00, 0.50, 250, 'ACTIVE', 0),
  (4, 'BEB-006', 'Jugo Natural', 'Jugo de naranja, mora o tomate', 'unidad', 1.50, 0.60, 100, 'ACTIVE', 1),
  (4, 'BEB-007', 'Batido de Frutas', 'Batido con leche y frutas', 'unidad', 2.00, 0.80, 80, 'ACTIVE', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Postres (category_id = 5)
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, status, prepared) VALUES
  (5, 'POS-001', 'Torta de Chocolate', 'Porción de torta de chocolate', 'porción', 2.00, 0.80, 30, 'ACTIVE', 0),
  (5, 'POS-002', 'Helado de Vainilla', 'Copa de helado de vainilla', 'unidad', 1.50, 0.50, 50, 'ACTIVE', 0),
  (5, 'POS-003', 'Flan de Caramelo', 'Flan casero con caramelo', 'unidad', 1.75, 0.60, 25, 'ACTIVE', 0),
  (5, 'POS-004', 'Brownie', 'Brownie de chocolate con nueces', 'unidad', 1.50, 0.50, 40, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert initial settings
INSERT INTO settings (setting_key, setting_value) VALUES
  ('tax_rate', '0.12'),
  ('store_name', 'Cafetería Águilas Rojas UCACUE'),
  ('store_address', 'Campus UCACUE, Cuenca - Ecuador'),
  ('store_phone', '07-2830-751'),
  ('currency', 'USD'),
  ('loyalty_enabled', 'true')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- Initialize loyalty points for existing users
INSERT INTO loyalty_points (user_id, points)
SELECT id, 0 FROM users WHERE id NOT IN (SELECT user_id FROM loyalty_points);

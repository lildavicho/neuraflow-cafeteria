-- Seed Data for Development
USE ucacue_erp;

-- Insert default admin user (password: Admin123!)
-- Hash generated with BCrypt for "Admin123!"
INSERT INTO users (full_name, email, password_hash, identification, phone, role, active) VALUES
('Administrador Sistema', 'admin@ucacue.edu.ec', '$2a$10$8.UnLm6NjKGy2h4fGkXXOu.IuBDqEqhL8TTpZ/i1QjhaRM0ttN4m.', '0101234567', '0999123456', 'ADMIN', true),
('Comprador Demo', 'comprador@ucacue.edu.ec', '$2a$10$8.UnLm6NjKGy2h4fGkXXOu.IuBDqEqhL8TTpZ/i1QjhaRM0ttN4m.', '0102345678', '0998234567', 'COMPRADOR', true),
('Juan Pérez', 'juan.perez@ucacue.edu.ec', '$2a$10$8.UnLm6NjKGy2h4fGkXXOu.IuBDqEqhL8TTpZ/i1QjhaRM0ttN4m.', '0103456789', '0997345678', 'COMPRADOR', true);

-- Insert categories
INSERT INTO categories (name, description, active) VALUES
('Bebidas Calientes', 'Café, té, chocolate y otras bebidas calientes', true),
('Bebidas Frías', 'Jugos, gaseosas, agua y bebidas frías', true),
('Snacks', 'Papas, galletas, dulces y snacks varios', true),
('Comida Rápida', 'Empanadas, sándwiches y comida preparada', true),
('Postres', 'Pasteles, tortas y postres dulces', true),
('Desayunos', 'Opciones de desayuno completo', true);

-- Insert products
INSERT INTO products (category_id, code, name, description, unit, price, purchase_price, stock, min_stock, status) VALUES
-- Bebidas Calientes
(1, 'BEB001', 'Café Americano', 'Café negro tradicional', 'TAZA', 1.50, 0.50, 100, 20, 'AVAILABLE'),
(1, 'BEB002', 'Cappuccino', 'Café con leche espumosa', 'TAZA', 2.00, 0.70, 80, 15, 'AVAILABLE'),
(1, 'BEB003', 'Chocolate Caliente', 'Chocolate caliente cremoso', 'TAZA', 1.75, 0.60, 50, 10, 'AVAILABLE'),
(1, 'BEB004', 'Té Verde', 'Té verde natural', 'TAZA', 1.25, 0.40, 60, 15, 'AVAILABLE'),

-- Bebidas Frías
(2, 'BEB005', 'Jugo de Naranja', 'Jugo natural de naranja', 'VASO', 2.00, 0.80, 30, 10, 'AVAILABLE'),
(2, 'BEB006', 'Coca Cola', 'Gaseosa Coca Cola 500ml', 'BOTELLA', 1.50, 0.90, 100, 20, 'AVAILABLE'),
(2, 'BEB007', 'Agua Mineral', 'Agua mineral sin gas 500ml', 'BOTELLA', 1.00, 0.50, 150, 30, 'AVAILABLE'),

-- Snacks
(3, 'SNK001', 'Papas Ruffles', 'Papas fritas Ruffles 45g', 'UNIDAD', 1.25, 0.70, 50, 15, 'AVAILABLE'),
(3, 'SNK002', 'Doritos', 'Doritos Nachos 50g', 'UNIDAD', 1.50, 0.80, 40, 12, 'AVAILABLE'),
(3, 'SNK003', 'Galletas Oreo', 'Paquete de galletas Oreo', 'UNIDAD', 1.75, 1.00, 35, 10, 'AVAILABLE'),

-- Comida Rápida
(4, 'COM001', 'Empanada de Queso', 'Empanada horneada de queso', 'UNIDAD', 1.50, 0.60, 30, 10, 'AVAILABLE'),
(4, 'COM002', 'Empanada de Carne', 'Empanada horneada de carne', 'UNIDAD', 1.75, 0.70, 25, 10, 'AVAILABLE'),
(4, 'COM003', 'Sándwich de Jamón y Queso', 'Sándwich en pan integral', 'UNIDAD', 2.50, 1.20, 20, 8, 'AVAILABLE'),
(4, 'COM004', 'Hot Dog', 'Hot dog con salchicha y aderezos', 'UNIDAD', 2.00, 0.90, 15, 5, 'AVAILABLE'),

-- Postres
(5, 'POS001', 'Porción de Torta de Chocolate', 'Deliciosa torta de chocolate', 'PORCIÓN', 2.50, 1.00, 12, 5, 'AVAILABLE'),
(5, 'POS002', 'Cheesecake', 'Porción de cheesecake', 'PORCIÓN', 3.00, 1.50, 10, 4, 'AVAILABLE'),
(5, 'POS003', 'Flan', 'Flan casero', 'UNIDAD', 2.00, 0.80, 15, 5, 'AVAILABLE'),

-- Desayunos
(6, 'DES001', 'Desayuno Continental', 'Café, pan, mermelada y jugo', 'COMBO', 4.50, 2.00, 10, 5, 'AVAILABLE');

-- Insert system settings
INSERT INTO system_settings (setting_key, setting_value, description, data_type, editable) VALUES
('iva_rate', '0.15', 'Tasa de IVA aplicable', 'DECIMAL', true),
('business_name', 'Cafetería UCACUE', 'Nombre del negocio', 'STRING', true),
('business_ruc', '0190123456001', 'RUC del negocio', 'STRING', true),
('business_address', 'Av. de las Américas y Humbolt, Cuenca', 'Dirección del negocio', 'STRING', true),
('business_phone', '072831608', 'Teléfono del negocio', 'STRING', true),
('low_stock_notification', 'true', 'Activar notificaciones de stock bajo', 'BOOLEAN', true),
('notification_email', 'admin@ucacue.edu.ec', 'Email para notificaciones', 'STRING', true);

-- Insert demo camera
INSERT INTO cameras (name, rtsp_url, description, location, enabled, stream_path) VALUES
('Cámara Principal', 'rtsp://admin:Adm1n99.@172.16.41.178:554/Streaming/Channels/101', 'Cámara principal del área de cafetería', 'Entrada Principal', true, 'cam1');

-- Insert suppliers
INSERT INTO suppliers (name, ruc, email, phone, address, active) VALUES
('Distribuidora Cuenca S.A.', '0190345678001', 'ventas@distcuenca.com', '072850123', 'Av. Ordóñez Lasso, Cuenca', true),
('Alimentos del Sur', '0190456789001', 'contacto@alimentossur.com', '072860234', 'Parque Industrial, Cuenca', true);

-- Insert sample sales (last 30 days)
INSERT INTO sales (invoice_number, user_id, subtotal, discount, iva, total, payment_method, order_type, status, created) VALUES
('FAC-2025-000001', 2, 8.50, 0.00, 1.28, 9.78, 'CASH', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 25 DAY)),
('FAC-2025-000002', 3, 5.25, 0.00, 0.79, 6.04, 'TRANSFER', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 24 DAY)),
('FAC-2025-000003', 2, 12.00, 1.00, 1.65, 12.65, 'PAYPHONE', 'TABLE', 'PAID', DATE_SUB(NOW(), INTERVAL 20 DAY)),
('FAC-2025-000004', 3, 7.75, 0.00, 1.16, 8.91, 'CASH', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 15 DAY)),
('FAC-2025-000005', 2, 15.50, 0.00, 2.33, 17.83, 'DATAFAST', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 10 DAY)),
('FAC-2025-000006', 3, 4.50, 0.00, 0.68, 5.18, 'CASH', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('FAC-2025-000007', 2, 9.25, 0.00, 1.39, 10.64, 'QR', 'TABLE', 'PAID', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('FAC-2025-000008', 3, 6.00, 0.00, 0.90, 6.90, 'CASH', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('FAC-2025-000009', 2, 11.50, 0.00, 1.73, 13.23, 'TRANSFER', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('FAC-2025-000010', 3, 8.00, 0.00, 1.20, 9.20, 'CASH', 'CARRY', 'PAID', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Insert sale items for sample sales
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
-- Sale 1
(1, 1, 2, 1.50, 3.00),
(1, 11, 1, 1.50, 1.50),
(1, 6, 2, 2.00, 4.00),

-- Sale 2
(2, 2, 1, 2.00, 2.00),
(2, 8, 1, 1.25, 1.25),
(2, 10, 1, 2.00, 2.00),

-- Sale 3
(3, 13, 2, 2.50, 5.00),
(3, 1, 2, 1.50, 3.00),
(3, 15, 1, 2.50, 2.50),
(3, 11, 1, 1.50, 1.50),

-- Sale 4
(4, 4, 1, 1.25, 1.25),
(4, 12, 1, 1.75, 1.75),
(4, 1, 3, 1.50, 4.50),

-- Sale 5
(5, 18, 1, 4.50, 4.50),
(5, 16, 1, 3.00, 3.00),
(5, 2, 2, 2.00, 4.00),
(5, 6, 2, 2.00, 4.00),

-- Sale 6
(6, 1, 2, 1.50, 3.00),
(6, 11, 1, 1.50, 1.50),

-- Sale 7
(7, 13, 1, 2.50, 2.50),
(7, 2, 1, 2.00, 2.00),
(7, 7, 2, 1.00, 2.00),
(7, 9, 1, 1.50, 1.50),
(7, 4, 1, 1.25, 1.25),

-- Sale 8
(8, 11, 2, 1.50, 3.00),
(8, 1, 2, 1.50, 3.00),

-- Sale 9
(9, 14, 1, 2.00, 2.00),
(9, 12, 2, 1.75, 3.50),
(9, 15, 1, 2.50, 2.50),
(9, 2, 1, 2.00, 2.00),
(9, 8, 1, 1.25, 1.25),

-- Sale 10
(10, 1, 2, 1.50, 3.00),
(10, 11, 1, 1.50, 1.50),
(10, 13, 1, 2.50, 2.50),
(10, 7, 1, 1.00, 1.00);

-- Update stock movements for sales
INSERT INTO stock_movements (product_id, type, quantity, stock_before, stock_after, reason, user_id, reference_type, reference_id) 
SELECT 
    si.product_id,
    'OUT',
    si.quantity,
    p.stock + si.quantity,
    p.stock,
    CONCAT('Venta #', s.invoice_number),
    s.user_id,
    'SALE',
    s.id
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN products p ON si.product_id = p.id
WHERE s.status = 'PAID';

-- Insert initial people count data (dummy data for testing)
INSERT INTO people_counts (timestamp, count, camera_id) VALUES
(DATE_SUB(NOW(), INTERVAL 1 HOUR), 12, 1),
(DATE_SUB(NOW(), INTERVAL 30 MINUTE), 15, 1),
(NOW(), 8, 1);

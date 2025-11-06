-- ==========================================================
-- ARCHIVO ÚNICO COMPLETO: ESQUEMA + SEEDS + TRIGGERS CORREGIDOS
-- ==========================================================

CREATE DATABASE IF NOT EXISTS ucacue_erp
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ucacue_erp;

-- ==========================================================
-- APP USER
-- ==========================================================
CREATE USER IF NOT EXISTS 'ucacue_user'@'%' IDENTIFIED BY 'ucacue_pass';
GRANT ALL PRIVILEGES ON ucacue_erp.* TO 'ucacue_user'@'%';
FLUSH PRIVILEGES;

-- ==========================================================
-- TABLA SYNC_OUTBOX CORREGIDA
-- ==========================================================
CREATE TABLE IF NOT EXISTS sync_outbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  op_type ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  payload JSON NOT NULL,
  status ENUM('PENDING','DONE','ERROR') DEFAULT 'PENDING',
  processed_at TIMESTAMP NULL,
  error_msg TEXT,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ==========================================================
-- USERS
-- ==========================================================
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  phone VARCHAR(15),
  identification VARCHAR(10),
  role ENUM('ADMIN','COMPRADOR') DEFAULT 'COMPRADOR',
  provider ENUM('LOCAL','GOOGLE','APPLE','MICROSOFT') DEFAULT 'LOCAL',
  firebase_uid VARCHAR(100) UNIQUE,
  avatar_url VARCHAR(255),
  active BOOLEAN DEFAULT TRUE,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ==========================================================
-- ROLES
-- ==========================================================
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) UNIQUE NOT NULL,
  description VARCHAR(200),
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_key VARCHAR(100) NOT NULL,
  allowed BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==========================================================
-- CATEGORIES
-- ==========================================================
CREATE TABLE IF NOT EXISTS categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  active BOOLEAN DEFAULT TRUE,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ==========================================================
-- PRODUCTS
-- ==========================================================
CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  code VARCHAR(20) UNIQUE,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  image_url VARCHAR(255),
  unit VARCHAR(20) DEFAULT 'UNIDAD',
  price DECIMAL(10,2) NOT NULL,
  purchase_price DECIMAL(10,2),
  stock INT DEFAULT 0,
  min_stock INT DEFAULT 5,
  status ENUM('AVAILABLE','SOLD_OUT','DISCONTINUED') DEFAULT 'AVAILABLE',
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

-- ==========================================================
-- SALES
-- ==========================================================
CREATE TABLE IF NOT EXISTS sales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(50) UNIQUE NOT NULL,
  user_id BIGINT NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  discount DECIMAL(10,2) DEFAULT 0,
  total DECIMAL(10,2) NOT NULL,
  payment_method ENUM('EFECTIVO','TRANSFERENCIA','TARJETA','QR') DEFAULT 'EFECTIVO',
  comprobante_url VARCHAR(255),
  comprobante_qr VARCHAR(255),
  order_type ENUM('TABLE','CARRY') DEFAULT 'CARRY',
  status ENUM('PENDING','PAID','READY','DELIVERED','CANCELLED') DEFAULT 'PENDING',
  notes VARCHAR(500),
  payment_reference VARCHAR(100),
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ==========================================================
-- SALE ITEMS
-- ==========================================================
CREATE TABLE IF NOT EXISTS sale_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- ==========================================================
-- STOCK MOVEMENTS
-- ==========================================================
CREATE TABLE IF NOT EXISTS stock_movements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  type ENUM('IN','OUT','ADJUST') NOT NULL,
  quantity INT NOT NULL,
  stock_before INT NOT NULL,
  stock_after INT NOT NULL,
  reason VARCHAR(200),
  user_id BIGINT NOT NULL,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ==========================================================
-- SYSTEM SETTINGS
-- ==========================================================
CREATE TABLE IF NOT EXISTS system_settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  setting_key VARCHAR(100) UNIQUE NOT NULL,
  setting_value TEXT,
  description VARCHAR(200),
  data_type VARCHAR(50) DEFAULT 'STRING',
  editable BOOLEAN DEFAULT TRUE,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ==========================================================
-- NOTIFICATIONS
-- ==========================================================
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  title VARCHAR(100) NOT NULL,
  message TEXT NOT NULL,
  type ENUM('INFO','PROMO','ORDER','STOCK','SYSTEM') DEFAULT 'INFO',
  icon VARCHAR(50) DEFAULT 'bell',
  target_url VARCHAR(255),
  read_status BOOLEAN DEFAULT FALSE,
  priority ENUM('LOW','MEDIUM','HIGH') DEFAULT 'MEDIUM',
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ==========================================================
-- CAMERAS
-- ==========================================================
CREATE TABLE IF NOT EXISTS cameras (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  rtsp_url VARCHAR(255) NOT NULL,
  description VARCHAR(200),
  location VARCHAR(50),
  enabled BOOLEAN DEFAULT TRUE,
  stream_path VARCHAR(50),
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ==========================================================
-- YOLO - PEOPLE COUNTS
-- ==========================================================
CREATE TABLE IF NOT EXISTS people_counts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  count INT NOT NULL,
  camera_id BIGINT,
  FOREIGN KEY (camera_id) REFERENCES cameras(id)
) ENGINE=InnoDB;

-- ==========================================================
-- PAYMENT RECEIPTS
-- ==========================================================
CREATE TABLE IF NOT EXISTS payment_receipts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_id BIGINT UNIQUE NOT NULL,
  user_id BIGINT NOT NULL,
  receipt_code VARCHAR(100) UNIQUE NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  payment_method ENUM('EFECTIVO','TRANSFERENCIA','TARJETA','QR') NOT NULL,
  payment_reference VARCHAR(100),
  status ENUM('GENERATED','VALIDATED','USED','CANCELLED') DEFAULT 'GENERATED',
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  validated_at TIMESTAMP NULL,
  used_at TIMESTAMP NULL,
  validated_by BIGINT NULL,
  FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (validated_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- ==========================================================
-- SEEDS - DATOS INICIALES CORREGIDOS
-- ==========================================================

-- ADMIN DEFAULT
INSERT IGNORE INTO users (id, full_name, email, password_hash, identification, phone, role, provider, active) VALUES 
(1, 'Administrador General', 'admin@ucacue.edu.ec', '$2a$10$8.UnLm6NjKGy2h4fGkXXOu.IuBDqEqhL8TTpZ/i1QjhaRM0ttN4m.', '0101234567', '0999123456', 'ADMIN', 'LOCAL', true);

-- ROLES Y PERMISOS
INSERT IGNORE INTO roles (id, role_name, description) VALUES
(1, 'ADMIN', 'Acceso total al sistema y gestión general'),
(2, 'COMPRADOR', 'Usuarios registrados mediante Google, Apple, Microsoft o portal ERP');

INSERT IGNORE INTO role_permissions (role_id, permission_key, allowed) VALUES
(1, 'dashboard.view', true),
(1, 'inventory.manage', true),
(1, 'pos.access', true),
(1, 'reports.view', true),
(1, 'users.manage', true),
(1, 'notifications.manage', true),
(1, 'settings.full', true),
(2, 'pos.access', true),
(2, 'profile.access', true),
(2, 'notifications.view', true),
(2, 'settings.partial', true);

-- AJUSTES DEL SISTEMA
INSERT IGNORE INTO system_settings (setting_key, setting_value, description, data_type, editable) VALUES
('business_name', 'Cafetería UCACUE', 'Nombre del negocio', 'STRING', true),
('backup_enabled', 'true', 'Respaldo automático en la nube', 'BOOLEAN', false),
('notifications_enabled', 'true', 'Activar notificaciones en tiempo real', 'BOOLEAN', true),
('multi_provider_auth', 'true', 'Permitir login con Google, Apple, Microsoft o ERP', 'BOOLEAN', true);

-- CÁMARA DEMO
INSERT IGNORE INTO cameras (name, rtsp_url, description, location, enabled, stream_path) VALUES
('Cámara Principal', 'rtsp://admin:Adm1n99.@172.16.41.178:554/Streaming/Channels/101', 'Cámara del área de atención principal', 'Entrada', true, 'cam1');

-- NOTIFICACIONES INICIALES
INSERT IGNORE INTO notifications (user_id, title, message, type, icon, priority) VALUES
(1, 'Bienvenido Administrador', 'Sistema UCACUE Bar configurado correctamente.', 'SYSTEM', 'bell', 'LOW');

-- CATEGORÍAS Y PRODUCTOS CORREGIDOS
INSERT IGNORE INTO categories (id, name, description, active) VALUES
(1, 'Bebidas', 'Bebidas y refrescos', true),
(2, 'Snacks', 'Snacks y botanas', true),
(3, 'Comidas', 'Comidas principales', true),
(4, 'Postres', 'Postres y dulces', true);

INSERT IGNORE INTO products (id, category_id, name, description, price, stock, status) VALUES
(101, 1, 'Coca Cola 355ml', 'Bebida gaseosa', 1.20, 120, 'AVAILABLE'),
(102, 1, 'Jugo Naranja 500ml', 'Jugo natural', 1.50, 80, 'AVAILABLE'),
(201, 2, 'Papitas 35g', 'Snack salado', 0.80, 150, 'AVAILABLE'),
(202, 2, 'Galletas Chocochips', 'Paquete 6 und', 0.90, 100, 'AVAILABLE'),
(301, 3, 'Sanduche de Jamón', 'Pan + jamón + queso', 2.50, 40, 'AVAILABLE'),
(302, 3, 'Empanada de Queso', 'Horneada', 1.00, 60, 'AVAILABLE'),
(401, 4, 'Brownie', 'Chocolate', 1.80, 20, 'AVAILABLE');

-- ==========================================================
-- STORED PROCEDURES
-- ==========================================================
DELIMITER $$

CREATE PROCEDURE sp_notify_order_update(IN p_sale_id BIGINT, IN p_status VARCHAR(20))
BEGIN
  DECLARE v_user_id BIGINT;
  DECLARE v_admin_id BIGINT;
  SELECT user_id INTO v_user_id FROM sales WHERE id = p_sale_id;
  SELECT id INTO v_admin_id FROM users WHERE role = 'ADMIN' LIMIT 1;

  IF p_status = 'PAID' THEN
    INSERT INTO notifications (user_id, title, message, type, icon, priority)
    VALUES (v_user_id, 'Pago confirmado', 'Tu pedido ha sido pagado y se está procesando.', 'ORDER', 'credit-card', 'MEDIUM');
    INSERT INTO notifications (user_id, title, message, type, icon, priority)
    VALUES (v_admin_id, 'Nueva Venta Pagada', CONCAT('Se ha registrado un pago para la venta #', p_sale_id), 'ORDER', 'dollar-sign', 'MEDIUM');
  ELSEIF p_status = 'READY' THEN
    INSERT INTO notifications (user_id, title, message, type, icon, priority)
    VALUES (v_user_id, 'Pedido listo', 'Tu pedido está listo para recoger.', 'ORDER', 'check-circle', 'HIGH');
  ELSEIF p_status = 'DELIVERED' THEN
    INSERT INTO notifications (user_id, title, message, type, icon, priority)
    VALUES (v_user_id, 'Pedido entregado', 'Gracias por tu compra.', 'ORDER', 'package', 'LOW');
  ELSEIF p_status = 'CANCELLED' THEN
    INSERT INTO notifications (user_id, title, message, type, icon, priority)
    VALUES (v_user_id, 'Pedido cancelado', 'Tu pedido fue cancelado.', 'ORDER', 'x-circle', 'HIGH');
  END IF;
END$$

DELIMITER ;

-- ==========================================================
-- TRIGGERS COMPLETOS CORREGIDOS
-- ==========================================================
DELIMITER $$

-- PRODUCTS
DROP TRIGGER IF EXISTS trg_products_after_insert$$
CREATE TRIGGER trg_products_after_insert AFTER INSERT ON products FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('products', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'category_id', NEW.category_id, 'code', NEW.code, 'name', NEW.name, 'description', NEW.description, 'image_url', NEW.image_url, 'price', NEW.price, 'stock', NEW.stock, 'status', NEW.status, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_products_after_update$$
CREATE TRIGGER trg_products_after_update AFTER UPDATE ON products FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('products', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'category_id', NEW.category_id, 'code', NEW.code, 'name', NEW.name, 'description', NEW.description, 'image_url', NEW.image_url, 'price', NEW.price, 'stock', NEW.stock, 'status', NEW.status, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_products_after_delete$$
CREATE TRIGGER trg_products_after_delete AFTER DELETE ON products FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('products', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- CATEGORIES
DROP TRIGGER IF EXISTS trg_categories_after_insert$$
CREATE TRIGGER trg_categories_after_insert AFTER INSERT ON categories FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('categories', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'name', NEW.name, 'description', NEW.description, 'active', NEW.active, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_categories_after_update$$
CREATE TRIGGER trg_categories_after_update AFTER UPDATE ON categories FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('categories', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'name', NEW.name, 'description', NEW.description, 'active', NEW.active, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_categories_after_delete$$
CREATE TRIGGER trg_categories_after_delete AFTER DELETE ON categories FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('categories', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- SALES
DROP TRIGGER IF EXISTS trg_sales_after_insert$$
CREATE TRIGGER trg_sales_after_insert AFTER INSERT ON sales FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sales', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'invoice_number', NEW.invoice_number, 'user_id', NEW.user_id, 'subtotal', NEW.subtotal, 'discount', NEW.discount, 'total', NEW.total, 'payment_method', NEW.payment_method, 'comprobante_url', NEW.comprobante_url, 'comprobante_qr', NEW.comprobante_qr, 'order_type', NEW.order_type, 'status', NEW.status, 'notes', NEW.notes, 'payment_reference', NEW.payment_reference, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_sales_after_update$$
CREATE TRIGGER trg_sales_after_update AFTER UPDATE ON sales FOR EACH ROW
BEGIN 
  INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sales', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'invoice_number', NEW.invoice_number, 'user_id', NEW.user_id, 'subtotal', NEW.subtotal, 'discount', NEW.discount, 'total', NEW.total, 'payment_method', NEW.payment_method, 'comprobante_url', NEW.comprobante_url, 'comprobante_qr', NEW.comprobante_qr, 'order_type', NEW.order_type, 'status', NEW.status, 'notes', NEW.notes, 'payment_reference', NEW.payment_reference, 'created', NEW.created, 'updated', NEW.updated));
  IF OLD.status <> NEW.status THEN CALL sp_notify_order_update(NEW.id, NEW.status); END IF;
END$$

DROP TRIGGER IF EXISTS trg_sales_after_delete$$
CREATE TRIGGER trg_sales_after_delete AFTER DELETE ON sales FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sales', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- SALE_ITEMS
DROP TRIGGER IF EXISTS trg_sale_items_after_insert$$
CREATE TRIGGER trg_sale_items_after_insert AFTER INSERT ON sale_items FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sale_items', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'sale_id', NEW.sale_id, 'product_id', NEW.product_id, 'quantity', NEW.quantity, 'unit_price', NEW.unit_price, 'subtotal', NEW.subtotal)); END$$

DROP TRIGGER IF EXISTS trg_sale_items_after_update$$
CREATE TRIGGER trg_sale_items_after_update AFTER UPDATE ON sale_items FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sale_items', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'sale_id', NEW.sale_id, 'product_id', NEW.product_id, 'quantity', NEW.quantity, 'unit_price', NEW.unit_price, 'subtotal', NEW.subtotal)); END$$

DROP TRIGGER IF EXISTS trg_sale_items_after_delete$$
CREATE TRIGGER trg_sale_items_after_delete AFTER DELETE ON sale_items FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('sale_items', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- NOTIFICATIONS
DROP TRIGGER IF EXISTS trg_notifications_after_insert$$
CREATE TRIGGER trg_notifications_after_insert AFTER INSERT ON notifications FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('notifications', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'user_id', NEW.user_id, 'title', NEW.title, 'message', NEW.message, 'type', NEW.type, 'icon', NEW.icon, 'target_url', NEW.target_url, 'read_status', NEW.read_status, 'priority', NEW.priority, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_notifications_after_update$$
CREATE TRIGGER trg_notifications_after_update AFTER UPDATE ON notifications FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('notifications', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'user_id', NEW.user_id, 'title', NEW.title, 'message', NEW.message, 'type', NEW.type, 'icon', NEW.icon, 'target_url', NEW.target_url, 'read_status', NEW.read_status, 'priority', NEW.priority, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_notifications_after_delete$$
CREATE TRIGGER trg_notifications_after_delete AFTER DELETE ON notifications FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('notifications', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- STOCK_MOVEMENTS
DROP TRIGGER IF EXISTS trg_stock_after_insert$$
CREATE TRIGGER trg_stock_after_insert AFTER INSERT ON stock_movements FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('stock_movements', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'product_id', NEW.product_id, 'type', NEW.type, 'quantity', NEW.quantity, 'stock_before', NEW.stock_before, 'stock_after', NEW.stock_after, 'reason', NEW.reason, 'user_id', NEW.user_id, 'created', NEW.created)); END$$

DROP TRIGGER IF EXISTS trg_stock_after_update$$
CREATE TRIGGER trg_stock_after_update AFTER UPDATE ON stock_movements FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('stock_movements', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'product_id', NEW.product_id, 'type', NEW.type, 'quantity', NEW.quantity, 'stock_before', NEW.stock_before, 'stock_after', NEW.stock_after, 'reason', NEW.reason, 'user_id', NEW.user_id, 'created', NEW.created)); END$$

DROP TRIGGER IF EXISTS trg_stock_after_delete$$
CREATE TRIGGER trg_stock_after_delete AFTER DELETE ON stock_movements FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('stock_movements', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- PEOPLE_COUNTS
DROP TRIGGER IF EXISTS trg_people_counts_after_insert$$
CREATE TRIGGER trg_people_counts_after_insert AFTER INSERT ON people_counts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('people_counts', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'timestamp', NEW.timestamp, 'count', NEW.count, 'camera_id', NEW.camera_id)); END$$

DROP TRIGGER IF EXISTS trg_people_counts_after_update$$
CREATE TRIGGER trg_people_counts_after_update AFTER UPDATE ON people_counts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('people_counts', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'timestamp', NEW.timestamp, 'count', NEW.count, 'camera_id', NEW.camera_id)); END$$

DROP TRIGGER IF EXISTS trg_people_counts_after_delete$$
CREATE TRIGGER trg_people_counts_after_delete AFTER DELETE ON people_counts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('people_counts', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- SYSTEM_SETTINGS
DROP TRIGGER IF EXISTS trg_system_settings_after_insert$$
CREATE TRIGGER trg_system_settings_after_insert AFTER INSERT ON system_settings FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('system_settings', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'setting_key', NEW.setting_key, 'setting_value', NEW.setting_value, 'description', NEW.description, 'data_type', NEW.data_type, 'editable', NEW.editable, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_system_settings_after_update$$
CREATE TRIGGER trg_system_settings_after_update AFTER UPDATE ON system_settings FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('system_settings', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'setting_key', NEW.setting_key, 'setting_value', NEW.setting_value, 'description', NEW.description, 'data_type', NEW.data_type, 'editable', NEW.editable, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_system_settings_after_delete$$
CREATE TRIGGER trg_system_settings_after_delete AFTER DELETE ON system_settings FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('system_settings', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- ROLES
DROP TRIGGER IF EXISTS trg_roles_after_insert$$
CREATE TRIGGER trg_roles_after_insert AFTER INSERT ON roles FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('roles', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'role_name', NEW.role_name, 'description', NEW.description, 'created', NEW.created)); END$$

DROP TRIGGER IF EXISTS trg_roles_after_update$$
CREATE TRIGGER trg_roles_after_update AFTER UPDATE ON roles FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('roles', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'role_name', NEW.role_name, 'description', NEW.description, 'created', NEW.created)); END$$

DROP TRIGGER IF EXISTS trg_roles_after_delete$$
CREATE TRIGGER trg_roles_after_delete AFTER DELETE ON roles FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('roles', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- ROLE_PERMISSIONS
DROP TRIGGER IF EXISTS trg_role_permissions_after_insert$$
CREATE TRIGGER trg_role_permissions_after_insert AFTER INSERT ON role_permissions FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('role_permissions', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'role_id', NEW.role_id, 'permission_key', NEW.permission_key, 'allowed', NEW.allowed)); END$$

DROP TRIGGER IF EXISTS trg_role_permissions_after_update$$
CREATE TRIGGER trg_role_permissions_after_update AFTER UPDATE ON role_permissions FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('role_permissions', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'role_id', NEW.role_id, 'permission_key', NEW.permission_key, 'allowed', NEW.allowed)); END$$

DROP TRIGGER IF EXISTS trg_role_permissions_after_delete$$
CREATE TRIGGER trg_role_permissions_after_delete AFTER DELETE ON role_permissions FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('role_permissions', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- CAMERAS
DROP TRIGGER IF EXISTS trg_cameras_after_insert$$
CREATE TRIGGER trg_cameras_after_insert AFTER INSERT ON cameras FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('cameras', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'name', NEW.name, 'rtsp_url', NEW.rtsp_url, 'description', NEW.description, 'location', NEW.location, 'enabled', NEW.enabled, 'stream_path', NEW.stream_path, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_cameras_after_update$$
CREATE TRIGGER trg_cameras_after_update AFTER UPDATE ON cameras FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('cameras', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'name', NEW.name, 'rtsp_url', NEW.rtsp_url, 'description', NEW.description, 'location', NEW.location, 'enabled', NEW.enabled, 'stream_path', NEW.stream_path, 'created', NEW.created, 'updated', NEW.updated)); END$$

DROP TRIGGER IF EXISTS trg_cameras_after_delete$$
CREATE TRIGGER trg_cameras_after_delete AFTER DELETE ON cameras FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('cameras', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

-- PAYMENT_RECEIPTS
DROP TRIGGER IF EXISTS trg_payment_receipts_after_insert$$
CREATE TRIGGER trg_payment_receipts_after_insert AFTER INSERT ON payment_receipts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('payment_receipts', NEW.id, 'INSERT', JSON_OBJECT('id', NEW.id, 'sale_id', NEW.sale_id, 'user_id', NEW.user_id, 'receipt_code', NEW.receipt_code, 'amount', NEW.amount, 'payment_method', NEW.payment_method, 'payment_reference', NEW.payment_reference, 'status', NEW.status, 'generated_at', NEW.generated_at, 'validated_at', NEW.validated_at, 'used_at', NEW.used_at, 'validated_by', NEW.validated_by)); END$$

DROP TRIGGER IF EXISTS trg_payment_receipts_after_update$$
CREATE TRIGGER trg_payment_receipts_after_update AFTER UPDATE ON payment_receipts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('payment_receipts', NEW.id, 'UPDATE', JSON_OBJECT('id', NEW.id, 'sale_id', NEW.sale_id, 'user_id', NEW.user_id, 'receipt_code', NEW.receipt_code, 'amount', NEW.amount, 'payment_method', NEW.payment_method, 'payment_reference', NEW.payment_reference, 'status', NEW.status, 'generated_at', NEW.generated_at, 'validated_at', NEW.validated_at, 'used_at', NEW.used_at, 'validated_by', NEW.validated_by)); END$$

DROP TRIGGER IF EXISTS trg_payment_receipts_after_delete$$
CREATE TRIGGER trg_payment_receipts_after_delete AFTER DELETE ON payment_receipts FOR EACH ROW
BEGIN INSERT INTO sync_outbox (entity_type, entity_id, op_type, payload) VALUES ('payment_receipts', OLD.id, 'DELETE', JSON_OBJECT('id', OLD.id)); END$$

DELIMITER ;

-- ==========================================================
-- VISTAS CORREGIDAS
-- ==========================================================
CREATE OR REPLACE VIEW v_top_products_30d AS
SELECT p.id, p.name, COALESCE(SUM(si.quantity),0) AS qty_30d
FROM products p
LEFT JOIN sale_items si ON si.product_id=p.id
LEFT JOIN sales s ON s.id=si.sale_id AND s.created>=DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY p.id,p.name;

CREATE OR REPLACE VIEW v_sales_daily AS
SELECT DATE(s.created) AS day, SUM(s.total) AS total
FROM sales s
GROUP BY DATE(s.created)
ORDER BY day DESC;

-- ==========================================================
-- INSERCIONES INICIALES EN SYNC_OUTBOX PARA DATOS EXISTENTES
-- ==========================================================
INSERT IGNORE INTO sync_outbox (entity_type, entity_id, op_type, payload, status)
SELECT 'users', id, 'INSERT', JSON_OBJECT('id', id, 'full_name', full_name, 'email', email, 'role', role, 'active', active, 'created', created, 'updated', updated), 'PENDING' FROM users;

INSERT IGNORE INTO sync_outbox (entity_type, entity_id, op_type, payload, status)
SELECT 'categories', id, 'INSERT', JSON_OBJECT('id', id, 'name', name, 'description', description, 'active', active, 'created', created, 'updated', updated), 'PENDING' FROM categories;

INSERT IGNORE INTO sync_outbox (entity_type, entity_id, op_type, payload, status)
SELECT 'products', id, 'INSERT', JSON_OBJECT('id', id, 'category_id', category_id, 'name', name, 'description', description, 'price', price, 'stock', stock, 'status', status, 'created', created, 'updated', updated), 'PENDING' FROM products;

INSERT IGNORE INTO sync_outbox (entity_type, entity_id, op_type, payload, status)
SELECT 'system_settings', id, 'INSERT', JSON_OBJECT('id', id, 'setting_key', setting_key, 'setting_value', setting_value, 'description', description, 'data_type', data_type, 'editable', editable, 'created', created, 'updated', updated), 'PENDING' FROM system_settings;

-- ==========================================================
-- MENSAJE FINAL
-- ==========================================================
SELECT '✅ BASE DE DATOS UCACUE ERP INSTALADA CORRECTAMENTE' AS status;
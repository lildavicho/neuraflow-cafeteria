-- Database Setup for UCACUE Bar System
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS ucacue_erp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ucacue_erp;

-- Create application user (matches .env DB_USER/DB_PASS)
CREATE USER IF NOT EXISTS 'ucacue_user'@'%' IDENTIFIED BY 'ucacue_pass';
GRANT ALL PRIVILEGES ON ucacue_erp.* TO 'ucacue_user'@'%';
FLUSH PRIVILEGES;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    identification VARCHAR(10) UNIQUE,
    role ENUM('ADMIN', 'COMPRADOR') NOT NULL DEFAULT 'COMPRADOR',
    active BOOLEAN DEFAULT TRUE,
    firebase_uid VARCHAR(100),
    two_factor_code VARCHAR(6),
    two_factor_expiry TIMESTAMP NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_identification (identification)
) ENGINE=InnoDB;

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    active BOOLEAN DEFAULT TRUE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(255),
    unit VARCHAR(20) DEFAULT 'UNIDAD',
    price DECIMAL(10, 2) NOT NULL,
    purchase_price DECIMAL(10, 2),
    stock INT NOT NULL DEFAULT 0,
    min_stock INT DEFAULT 5,
    status ENUM('AVAILABLE', 'SOLD_OUT', 'DISCONTINUED') DEFAULT 'AVAILABLE',
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_product_name (name),
    INDEX idx_product_code (code),
    INDEX idx_product_category (category_id)
) ENGINE=InnoDB;

-- Sales table
CREATE TABLE IF NOT EXISTS sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) DEFAULT 0,
    iva DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'TRANSFER', 'PAYPHONE', 'DATAFAST', 'QR') NOT NULL,
    order_type ENUM('TABLE', 'CARRY') DEFAULT 'CARRY',
    status ENUM('PENDING', 'PAID', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    notes VARCHAR(500),
    payment_reference VARCHAR(100),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_sale_created (created),
    INDEX idx_sale_user (user_id),
    INDEX idx_sale_status (status)
) ENGINE=InnoDB;

-- Sale Items table
CREATE TABLE IF NOT EXISTS sale_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    subtotal_discount DECIMAL(10, 2) DEFAULT 0,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_sale_item_sale (sale_id),
    INDEX idx_sale_item_product (product_id)
) ENGINE=InnoDB;

-- Stock Movements table
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    type ENUM('IN', 'OUT', 'ADJUST') NOT NULL,
    quantity INT NOT NULL,
    stock_before INT NOT NULL,
    stock_after INT NOT NULL,
    reason VARCHAR(200),
    user_id BIGINT NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_stock_movement_product (product_id),
    INDEX idx_stock_movement_created (created)
) ENGINE=InnoDB;

-- Idempotent Requests table
CREATE TABLE IF NOT EXISTS idempotent_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    response TEXT NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_idempotent_key (idempotency_key)
) ENGINE=InnoDB;

-- System Settings table
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(200),
    data_type VARCHAR(50) DEFAULT 'STRING',
    editable BOOLEAN DEFAULT TRUE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Cameras table
CREATE TABLE IF NOT EXISTS cameras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    rtsp_url VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    location VARCHAR(50),
    enabled BOOLEAN DEFAULT TRUE,
    stream_path VARCHAR(50),
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    ruc VARCHAR(13) UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(15),
    address VARCHAR(200),
    active BOOLEAN DEFAULT TRUE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Purchase Orders table
CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('PENDING', 'APPROVED', 'RECEIVED', 'CANCELLED') DEFAULT 'PENDING',
    subtotal DECIMAL(10, 2) NOT NULL,
    taxes DECIMAL(10, 2) DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL,
    notes VARCHAR(500),
    created_by BIGINT NOT NULL,
    approved_by BIGINT,
    received_by BIGINT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id),
    FOREIGN KEY (received_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Purchase Order Details table
CREATE TABLE IF NOT EXISTS purchase_order_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

-- People Count table (for YOLO integration)
CREATE TABLE IF NOT EXISTS people_counts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    count INT NOT NULL,
    camera_id BIGINT,
    FOREIGN KEY (camera_id) REFERENCES cameras(id),
    INDEX idx_people_count_timestamp (timestamp)
) ENGINE=InnoDB;

-- User Activities (Audit Log)
CREATE TABLE IF NOT EXISTS user_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_activity_user (user_id),
    INDEX idx_activity_created (created)
) ENGINE=InnoDB;

-- Views for reporting

-- Daily Sales Summary View
CREATE OR REPLACE VIEW v_sales_daily AS
SELECT 
    DATE(created) as sale_date,
    COUNT(*) as total_transactions,
    SUM(subtotal) as total_subtotal,
    SUM(discount) as total_discount,
    SUM(iva) as total_tax,
    SUM(total) as total_sales,
    AVG(total) as average_sale
FROM sales
WHERE status = 'PAID'
GROUP BY DATE(created);

-- Top Products (30 days)
CREATE OR REPLACE VIEW v_top_products_30d AS
SELECT 
    p.id,
    p.name,
    p.code,
    c.name as category_name,
    SUM(si.quantity) as total_quantity_sold,
    SUM(si.subtotal) as total_revenue,
    COUNT(DISTINCT si.sale_id) as times_sold
FROM sale_items si
JOIN products p ON si.product_id = p.id
JOIN categories c ON p.category_id = c.id
JOIN sales s ON si.sale_id = s.id
WHERE s.status = 'PAID' 
    AND s.created >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY p.id, p.name, p.code, c.name
ORDER BY total_quantity_sold DESC;

-- Low Stock Products View
CREATE OR REPLACE VIEW v_low_stock_products AS
SELECT 
    p.id,
    p.code,
    p.name,
    c.name as category_name,
    p.stock,
    p.min_stock,
    (p.min_stock - p.stock) as units_needed
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.stock <= p.min_stock 
    AND p.status = 'AVAILABLE'
ORDER BY units_needed DESC;

-- User Sales Summary
CREATE OR REPLACE VIEW v_user_sales_summary AS
SELECT 
    u.id as user_id,
    u.full_name,
    u.email,
    u.role,
    COUNT(s.id) as total_purchases,
    COALESCE(SUM(s.total), 0) as total_spent,
    COALESCE(AVG(s.total), 0) as average_purchase,
    MAX(s.created) as last_purchase_date
FROM users u
LEFT JOIN sales s ON u.id = s.user_id AND s.status = 'PAID'
GROUP BY u.id, u.full_name, u.email, u.role;

-- Stored Procedures

DELIMITER $$

-- Procedure to update product stock
CREATE PROCEDURE sp_update_stock(
    IN p_product_id BIGINT,
    IN p_quantity INT,
    IN p_type VARCHAR(10),
    IN p_user_id BIGINT,
    IN p_reason VARCHAR(200)
)
BEGIN
    DECLARE v_current_stock INT;
    DECLARE v_new_stock INT;
    
    -- Get current stock
    SELECT stock INTO v_current_stock 
    FROM products 
    WHERE id = p_product_id
    FOR UPDATE;
    
    -- Calculate new stock
    IF p_type = 'IN' THEN
        SET v_new_stock = v_current_stock + p_quantity;
    ELSEIF p_type = 'OUT' THEN
        SET v_new_stock = v_current_stock - p_quantity;
    ELSE -- ADJUST
        SET v_new_stock = p_quantity;
    END IF;
    
    -- Check if stock would be negative
    IF v_new_stock < 0 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Stock insuficiente';
    END IF;
    
    -- Update product stock
    UPDATE products 
    SET stock = v_new_stock 
    WHERE id = p_product_id;
    
    -- Record stock movement
    INSERT INTO stock_movements (
        product_id, type, quantity, 
        stock_before, stock_after, 
        reason, user_id
    ) VALUES (
        p_product_id, p_type, p_quantity,
        v_current_stock, v_new_stock,
        p_reason, p_user_id
    );
END$$

DELIMITER ;

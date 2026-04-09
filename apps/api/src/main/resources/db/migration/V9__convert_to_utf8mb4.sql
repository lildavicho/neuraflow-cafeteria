-- V9: Convert schema to UTF-8 / utf8mb4
-- ALTER DATABASE is omitted here because some local MySQL/XAMPP versions
-- do not support it through Flyway's execution protocol.

ALTER TABLE products CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE categories CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE orders CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE order_items CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sales CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

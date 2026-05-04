-- MySQL fallback (legacy environment): mirror customer billing data on orders.
ALTER TABLE orders
    ADD COLUMN customer_email VARCHAR(180) NULL,
    ADD COLUMN customer_name VARCHAR(200) NULL,
    ADD COLUMN customer_identification VARCHAR(20) NULL,
    ADD COLUMN customer_identification_type VARCHAR(10) NULL,
    ADD COLUMN customer_address VARCHAR(300) NULL,
    ADD COLUMN customer_phone VARCHAR(30) NULL;

CREATE INDEX idx_orders_customer_email ON orders (customer_email);

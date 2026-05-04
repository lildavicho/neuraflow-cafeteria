-- Walk-in customer billing data on orders so RIDE/SRI documents can be issued
-- without requiring a registered user. customer_identification_type follows
-- SRI catalog 07 codes (04=RUC, 05=Cedula, 06=Pasaporte, 07=Consumidor Final, 08=Identif. Exterior).
ALTER TABLE bar_app.orders
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(180),
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS customer_identification VARCHAR(20),
    ADD COLUMN IF NOT EXISTS customer_identification_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS customer_address VARCHAR(300),
    ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_orders_customer_email ON bar_app.orders (customer_email);

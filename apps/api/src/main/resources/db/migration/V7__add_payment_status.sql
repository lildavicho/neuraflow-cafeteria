ALTER TABLE orders
  ADD COLUMN payment_status VARCHAR(30) NULL AFTER payment_reference;

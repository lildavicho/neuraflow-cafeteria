ALTER TABLE orders
    ADD COLUMN transaction_status VARCHAR(30) NULL AFTER status,
    ADD COLUMN paid_at DATETIME NULL AFTER payment_status,
    ADD COLUMN cancelled_at DATETIME NULL AFTER paid_at,
    ADD COLUMN refunded_at DATETIME NULL AFTER cancelled_at;

UPDATE orders o
LEFT JOIN sales s ON s.order_id = o.id
SET o.transaction_status = CASE
    WHEN o.status = 'CANCELLED' AND o.payment_status = 'REFUNDED' THEN 'REFUNDED'
    WHEN o.status = 'CANCELLED' THEN 'CANCELLED'
    WHEN s.id IS NOT NULL OR o.payment_status = 'PAID' OR o.status IN ('DELIVERED', 'COMPLETED') THEN 'PAID'
    ELSE 'PENDING_PAYMENT'
END
WHERE o.transaction_status IS NULL;

UPDATE orders o
LEFT JOIN sales s ON s.order_id = o.id
SET o.paid_at = COALESCE(o.paid_at, s.paid_at, o.updated_at, o.created_at)
WHERE o.transaction_status = 'PAID'
  AND o.paid_at IS NULL;

UPDATE orders
SET cancelled_at = COALESCE(cancelled_at, updated_at, created_at)
WHERE transaction_status = 'CANCELLED'
  AND cancelled_at IS NULL;

UPDATE orders
SET refunded_at = COALESCE(refunded_at, updated_at, created_at)
WHERE transaction_status = 'REFUNDED'
  AND refunded_at IS NULL;

ALTER TABLE orders
    MODIFY COLUMN transaction_status VARCHAR(30) NOT NULL;

CREATE INDEX idx_orders_transaction_status ON orders (transaction_status);
CREATE INDEX idx_orders_paid_at ON orders (paid_at);

ALTER TABLE sales
    ADD COLUMN sale_status VARCHAR(20) NULL AFTER paid_at,
    ADD COLUMN refunded_at DATETIME NULL AFTER sale_status;

UPDATE sales
SET sale_status = 'PAID'
WHERE sale_status IS NULL;

UPDATE sales s
JOIN orders o ON o.id = s.order_id
SET s.refunded_at = COALESCE(s.refunded_at, o.refunded_at, o.updated_at, o.created_at)
WHERE o.transaction_status = 'REFUNDED'
  AND s.refunded_at IS NULL;

UPDATE sales s
JOIN orders o ON o.id = s.order_id
SET s.sale_status = 'REFUNDED'
WHERE o.transaction_status = 'REFUNDED';

ALTER TABLE sales
    MODIFY COLUMN sale_status VARCHAR(20) NOT NULL;

CREATE INDEX idx_sales_sale_status ON sales (sale_status);

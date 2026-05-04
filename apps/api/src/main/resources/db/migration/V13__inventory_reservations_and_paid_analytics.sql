ALTER TABLE orders
  ADD COLUMN inventory_status VARCHAR(30) NULL AFTER payment_status;

UPDATE orders o
LEFT JOIN sales s ON s.order_id = o.id
SET o.inventory_status = CASE
  WHEN o.status = 'CANCELLED' THEN 'RELEASED'
  WHEN s.id IS NOT NULL OR o.payment_status = 'PAID' OR o.status IN ('DELIVERED', 'COMPLETED') THEN 'COMMITTED'
  ELSE 'RESERVED'
END
WHERE o.inventory_status IS NULL;

INSERT INTO sales (order_id, total, created_at, paid_at)
SELECT o.id,
       o.total,
       COALESCE(o.created_at, CURRENT_TIMESTAMP),
       COALESCE(o.updated_at, o.created_at, CURRENT_TIMESTAMP)
FROM orders o
LEFT JOIN sales s ON s.order_id = o.id
WHERE s.id IS NULL
  AND o.inventory_status = 'COMMITTED';

UPDATE sales s
JOIN orders o ON o.id = s.order_id
SET s.paid_at = COALESCE(s.paid_at, o.updated_at, o.created_at, CURRENT_TIMESTAMP)
WHERE o.inventory_status = 'COMMITTED'
  AND s.paid_at IS NULL;

UPDATE products p
JOIN (
  SELECT oi.product_id, SUM(oi.quantity) AS qty
  FROM order_items oi
  JOIN orders o ON o.id = oi.order_id
  WHERE o.inventory_status IN ('RESERVED', 'RELEASED')
  GROUP BY oi.product_id
) adjustments ON adjustments.product_id = p.id
SET p.stock = p.stock + adjustments.qty;

UPDATE products
SET status = CASE
  WHEN status <> 'DISCONTINUED' AND stock <= 0 THEN 'SOLD_OUT'
  WHEN status = 'SOLD_OUT' AND stock > 0 THEN 'AVAILABLE'
  ELSE status
END;

ALTER TABLE orders
  MODIFY COLUMN inventory_status VARCHAR(30) NOT NULL;

CREATE INDEX idx_orders_inventory_status ON orders (inventory_status);

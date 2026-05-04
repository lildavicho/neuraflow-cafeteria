-- Track whether each POS order should be emitted as a Nota de Venta (no SRI doc)
-- or as Factura electronica (triggers SRI XML + RIDE + email).
ALTER TABLE bar_app.orders
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'NOTA_VENTA';

CREATE INDEX IF NOT EXISTS idx_orders_document_type ON bar_app.orders (document_type);

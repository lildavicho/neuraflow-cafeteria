-- MySQL fallback: track whether each POS order should be emitted as a Nota de Venta
-- (no SRI doc) or as Factura electronica.
ALTER TABLE orders
    ADD COLUMN document_type VARCHAR(20) NOT NULL DEFAULT 'NOTA_VENTA';

CREATE INDEX idx_orders_document_type ON orders (document_type);

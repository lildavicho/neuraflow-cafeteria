set search_path to "bar_app", public;

create index if not exists "idx_perf_sales_status_paid_order"
    on "sales" ("sale_status", "paid_at", "order_id");

create index if not exists "idx_perf_order_items_order_product"
    on "order_items" ("order_id", "product_id");

create index if not exists "idx_perf_order_items_product_order"
    on "order_items" ("product_id", "order_id");

create index if not exists "idx_perf_orders_transfer_pending"
    on "orders" ("payment_method", "payment_status", "status", "created_at" desc);

create index if not exists "idx_perf_products_status_stock"
    on "products" ("status", "stock", "min_stock");

create index if not exists "idx_perf_tax_documents_tenant_status_created"
    on "tax_documents" ("tenant_id", "status", "created_at" desc);

create index if not exists "idx_perf_stock_movements_product_created"
    on "stock_movements" ("product_id", "created_at" desc);

create index if not exists "idx_perf_audit_logs_user_created"
    on "audit_logs" ("user_id", "created_at" desc);

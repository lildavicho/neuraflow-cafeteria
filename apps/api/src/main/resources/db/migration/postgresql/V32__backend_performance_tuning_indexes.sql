set search_path to "bar_app", public;

create index if not exists "idx_perf_sales_tenant_status_paid"
    on "sales" ("tenant_id", "sale_status", "paid_at" desc, "order_id");

create index if not exists "idx_perf_orders_tenant_tx_created"
    on "orders" ("tenant_id", "transaction_status", "created_at" desc);

create index if not exists "idx_perf_orders_tenant_payment_pending"
    on "orders" ("tenant_id", "payment_method", "payment_status", "status", "created_at" desc);

create index if not exists "idx_perf_order_items_tenant_product_order"
    on "order_items" ("tenant_id", "product_id", "order_id");

create index if not exists "idx_perf_products_tenant_public"
    on "products" ("tenant_id", "prepared", "status", "category_id", "name");

create index if not exists "idx_perf_products_tenant_low_stock"
    on "products" ("tenant_id", "status", "stock", "min_stock");

create index if not exists "idx_perf_stock_movements_tenant_product_created"
    on "stock_movements" ("tenant_id", "product_id", "created_at" desc);

create index if not exists "idx_perf_stock_movements_tenant_type_created"
    on "stock_movements" ("tenant_id", "type", "created_at" desc);

create index if not exists "idx_perf_cash_registers_tenant_status_opened"
    on "cash_registers" ("tenant_id", "status", "opened_at" desc);

create index if not exists "idx_perf_tax_documents_tenant_created"
    on "tax_documents" ("tenant_id", "created_at" desc);

create index if not exists "idx_perf_tax_documents_tenant_status_last"
    on "tax_documents" ("tenant_id", "status", "last_status_at");

create index if not exists "idx_perf_tax_documents_tenant_ride_email"
    on "tax_documents" ("tenant_id", "ride_email_status");

create index if not exists "idx_perf_tax_documents_authorized_missing_pdf"
    on "tax_documents" ("tenant_id", "created_at")
    where "status" = 'AUTHORIZED' and "ride_pdf_path" is null;

create index if not exists "idx_perf_acc_journal_entries_tenant_created"
    on "acc_journal_entries" ("tenant_id", "created_at" desc);

create index if not exists "idx_perf_acc_receivables_tenant_issue_created"
    on "acc_receivables" ("tenant_id", "issue_date" desc, "created_at" desc);

create index if not exists "idx_perf_acc_payables_tenant_issue_created"
    on "acc_payables" ("tenant_id", "issue_date" desc, "created_at" desc);

create index if not exists "idx_perf_acc_ats_periods_tenant_created"
    on "acc_ats_periods" ("tenant_id", "created_at" desc);

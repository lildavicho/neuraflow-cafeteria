-- Sprint A.3: Anexo Transaccional Simplificado (ATS) period tracker
-- Stores generated XML per fiscal month (year/month) with status and aggregated totals.

CREATE TABLE IF NOT EXISTS acc_ats_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    fiscal_year INT NOT NULL,
    fiscal_month INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    sales_count INT NOT NULL DEFAULT 0,
    purchases_count INT NOT NULL DEFAULT 0,
    withholdings_count INT NOT NULL DEFAULT 0,
    total_sales DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_purchases DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_withheld DECIMAL(14,2) NOT NULL DEFAULT 0,
    xml_payload LONGTEXT,
    generated_at DATETIME,
    submitted_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_acc_ats_periods_tenant_year_month (tenant_id, fiscal_year, fiscal_month),
    INDEX idx_acc_ats_periods_tenant_status (tenant_id, status)
) ENGINE=InnoDB;

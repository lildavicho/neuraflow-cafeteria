CREATE TABLE IF NOT EXISTS tax_ride_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_code VARCHAR(60) NOT NULL DEFAULT 'DEFAULT',
    template_name VARCHAR(120) NOT NULL DEFAULT 'InsightVision RIDE',
    layout_json TEXT NULL,
    html_content TEXT NULL,
    custom TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_ride_templates_tenant
        FOREIGN KEY (tenant_id) REFERENCES erp_tenants(id),
    UNIQUE KEY uk_tax_ride_templates_tenant_code (tenant_id, template_code),
    INDEX idx_tax_ride_templates_tenant_active (tenant_id, active)
) ENGINE=InnoDB;

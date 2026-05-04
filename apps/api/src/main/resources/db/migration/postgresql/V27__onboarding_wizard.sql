CREATE TABLE IF NOT EXISTS tenant_onboarding_sessions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES erp_tenants(id),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    current_step VARCHAR(80) NOT NULL DEFAULT 'business-discovery',
    progress_percent INTEGER NOT NULL DEFAULT 0,
    vertical_template VARCHAR(80),
    business_profile JSONB NOT NULL DEFAULT '{}'::jsonb,
    legal_entity JSONB NOT NULL DEFAULT '{}'::jsonb,
    tax_profile JSONB NOT NULL DEFAULT '{}'::jsonb,
    certificate_status JSONB NOT NULL DEFAULT '{}'::jsonb,
    emission_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    accounting_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    sales_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    purchases_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    inventory_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    growth_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    brand_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    permissions_setup JSONB NOT NULL DEFAULT '{}'::jsonb,
    checklist JSONB NOT NULL DEFAULT '{}'::jsonb,
    warnings JSONB NOT NULL DEFAULT '[]'::jsonb,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tenant_onboarding_sessions_tenant UNIQUE (tenant_id),
    CONSTRAINT ck_tenant_onboarding_sessions_progress CHECK (progress_percent >= 0 AND progress_percent <= 100)
);

CREATE INDEX IF NOT EXISTS idx_tenant_onboarding_sessions_status
    ON tenant_onboarding_sessions(status);

CREATE TABLE IF NOT EXISTS onboarding_audit_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES erp_tenants(id),
    session_id BIGINT REFERENCES tenant_onboarding_sessions(id),
    event_type VARCHAR(80) NOT NULL,
    actor VARCHAR(160),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_onboarding_audit_events_tenant_created
    ON onboarding_audit_events(tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS onboarding_catalog_items (
    id BIGSERIAL PRIMARY KEY,
    catalog_type VARCHAR(80) NOT NULL,
    item_code VARCHAR(80) NOT NULL,
    label VARCHAR(200) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_onboarding_catalog_items_type_code UNIQUE (catalog_type, item_code)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_catalog_items_type_active
    ON onboarding_catalog_items(catalog_type, active);

INSERT INTO onboarding_catalog_items (catalog_type, item_code, label, payload) VALUES
    ('sri_document_type', '01', 'Factura', '{"codDoc":"01"}'::jsonb),
    ('sri_document_type', '03', 'Liquidacion de compra', '{"codDoc":"03"}'::jsonb),
    ('sri_document_type', '04', 'Nota de credito', '{"codDoc":"04"}'::jsonb),
    ('sri_document_type', '05', 'Nota de debito', '{"codDoc":"05"}'::jsonb),
    ('sri_document_type', '06', 'Guia de remision', '{"codDoc":"06"}'::jsonb),
    ('sri_document_type', '07', 'Comprobante de retencion', '{"codDoc":"07"}'::jsonb),
    ('sri_payment_method', '01', 'Sin utilizacion del sistema financiero', '{}'::jsonb),
    ('sri_payment_method', '15', 'Compensacion de deudas', '{}'::jsonb),
    ('sri_payment_method', '16', 'Tarjeta de debito', '{}'::jsonb),
    ('sri_payment_method', '17', 'Dinero electronico', '{}'::jsonb),
    ('sri_payment_method', '18', 'Tarjeta prepago', '{}'::jsonb),
    ('sri_payment_method', '19', 'Tarjeta de credito', '{}'::jsonb),
    ('sri_payment_method', '20', 'Otros con utilizacion del sistema financiero', '{}'::jsonb),
    ('sri_payment_method', '21', 'Endoso de titulos', '{}'::jsonb),
    ('vertical_template', 'restaurant', 'Restaurante / bar', '{"modules":["pos","inventory","purchases","sri"],"accent":"#0f766e"}'::jsonb),
    ('vertical_template', 'education', 'Educacion', '{"modules":["sales","crm","reports","sri"],"accent":"#2563eb"}'::jsonb),
    ('vertical_template', 'retail', 'Retail', '{"modules":["pos","inventory","purchases","sri"],"accent":"#be123c"}'::jsonb),
    ('vertical_template', 'commercial', 'Empresa comercial', '{"modules":["sales","purchases","accounting","crm","sri"],"accent":"#374151"}'::jsonb),
    ('fiscal_flag', 'retention_agent', 'Agente de retencion designado', '{"requiresResolution":true}'::jsonb),
    ('fiscal_flag', 'special_taxpayer', 'Contribuyente especial', '{"requiresResolution":true}'::jsonb),
    ('fiscal_flag', 'rimpe', 'Regimen RIMPE', '{"requiresLegend":true}'::jsonb),
    ('fiscal_flag', 'large_taxpayer', 'Gran contribuyente', '{"requiresResolution":true}'::jsonb),
    ('fiscal_flag', 'habitual_exporter', 'Exportador habitual', '{}'::jsonb),
    ('fiscal_flag', 'construction_materials', 'Materiales de construccion', '{"specialAnnex":true}'::jsonb),
    ('fiscal_flag', 'commercial_transport', 'Transporte comercial', '{"specialAnnex":true}'::jsonb),
    ('fiscal_flag', 'fuel_sales', 'Venta de combustibles', '{"requiresPlate":true}'::jsonb),
    ('fiscal_flag', 'tourism_special_vat', 'Turismo con tarifa especial', '{"specialVat":true}'::jsonb)
ON CONFLICT (catalog_type, item_code) DO NOTHING;

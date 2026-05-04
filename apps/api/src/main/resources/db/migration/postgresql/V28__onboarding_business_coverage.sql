CREATE TABLE IF NOT EXISTS onboarding_reference_files (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES erp_tenants(id),
    session_id BIGINT REFERENCES tenant_onboarding_sessions(id),
    file_kind VARCHAR(40) NOT NULL,
    file_name VARCHAR(240) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_onboarding_reference_files_tenant_kind
    ON onboarding_reference_files(tenant_id, file_kind, created_at DESC);

INSERT INTO onboarding_catalog_items (catalog_type, item_code, label, payload) VALUES
    ('vertical_template', 'food_restaurant', 'Restaurante, cafeteria, bar o comida preparada', '{"modules":["pos","inventory","purchases","sri"],"accent":"#0f766e","group":"Alimentos"}'::jsonb),
    ('vertical_template', 'grocery_store', 'Tienda de barrio, minimarket o abarrotes', '{"modules":["pos","inventory","purchases","sri"],"accent":"#15803d","group":"Comercio"}'::jsonb),
    ('vertical_template', 'supermarket', 'Supermercado o autoservicio', '{"modules":["pos","inventory","purchases","sri"],"accent":"#2563eb","group":"Comercio"}'::jsonb),
    ('vertical_template', 'retail_clothing', 'Ropa, calzado, accesorios o boutique', '{"modules":["pos","inventory","pricing","sri"],"accent":"#be123c","group":"Comercio"}'::jsonb),
    ('vertical_template', 'hardware_store', 'Ferreteria, materiales o construccion', '{"modules":["pos","inventory","purchases","sri"],"accent":"#b45309","group":"Construccion"}'::jsonb),
    ('vertical_template', 'pharmacy', 'Farmacia o productos de salud', '{"modules":["pos","inventory","purchases","sri"],"accent":"#047857","group":"Salud"}'::jsonb),
    ('vertical_template', 'medical_services', 'Consultorio, clinica, laboratorio o servicios medicos', '{"modules":["sales","customers","sri"],"accent":"#0369a1","group":"Salud"}'::jsonb),
    ('vertical_template', 'education_services', 'Escuela, colegio, instituto o capacitacion', '{"modules":["sales","customers","basic-reports","sri"],"accent":"#2563eb","group":"Educacion"}'::jsonb),
    ('vertical_template', 'hotel_tourism', 'Hotel, hostal, turismo o agencia de viajes', '{"modules":["sales","pos","customers","sri"],"accent":"#0891b2","group":"Turismo"}'::jsonb),
    ('vertical_template', 'transport_logistics', 'Transporte, encomiendas, delivery o logistica', '{"modules":["sales","inventory","sri"],"accent":"#475569","group":"Transporte"}'::jsonb),
    ('vertical_template', 'automotive_workshop', 'Taller automotriz, repuestos o lubricadora', '{"modules":["pos","inventory","customers","sri"],"accent":"#334155","group":"Servicios"}'::jsonb),
    ('vertical_template', 'beauty_wellness', 'Peluqueria, spa, belleza o bienestar', '{"modules":["pos","customers","sri"],"accent":"#db2777","group":"Servicios"}'::jsonb),
    ('vertical_template', 'professional_services', 'Servicios profesionales, consultoria o asesoria', '{"modules":["sales","customers","accounting","sri"],"accent":"#374151","group":"Servicios"}'::jsonb),
    ('vertical_template', 'technology_services', 'Tecnologia, software, soporte o servicios digitales', '{"modules":["sales","crm","sri"],"accent":"#2563eb","group":"Servicios digitales"}'::jsonb),
    ('vertical_template', 'manufacturing', 'Manufactura, produccion o taller industrial', '{"modules":["inventory","purchases","sales","sri"],"accent":"#7c2d12","group":"Produccion"}'::jsonb),
    ('vertical_template', 'agriculture_livestock', 'Agricultura, ganaderia o agroindustria', '{"modules":["inventory","purchases","sales","sri"],"accent":"#4d7c0f","group":"Agro"}'::jsonb),
    ('vertical_template', 'fishing_aquaculture', 'Pesca, acuicultura o mariscos', '{"modules":["inventory","purchases","sales","sri"],"accent":"#0284c7","group":"Agro"}'::jsonb),
    ('vertical_template', 'fuel_station', 'Gasolinera, combustibles o lubricantes', '{"modules":["pos","inventory","sri"],"accent":"#b91c1c","group":"Combustibles"}'::jsonb),
    ('vertical_template', 'real_estate', 'Inmobiliaria, arriendos o administracion de propiedades', '{"modules":["sales","customers","accounting","sri"],"accent":"#525252","group":"Servicios"}'::jsonb),
    ('vertical_template', 'events_entertainment', 'Eventos, entretenimiento, deportes o cultura', '{"modules":["sales","pos","customers","sri"],"accent":"#7e22ce","group":"Servicios"}'::jsonb),
    ('vertical_template', 'security_cleaning', 'Seguridad, limpieza o mantenimiento', '{"modules":["sales","hr","customers","sri"],"accent":"#0f766e","group":"Servicios"}'::jsonb),
    ('vertical_template', 'financial_cooperative', 'Cooperativa, financiera o servicios de cobro', '{"modules":["sales","accounting","basic-reports","sri"],"accent":"#1d4ed8","group":"Finanzas"}'::jsonb),
    ('vertical_template', 'nonprofit_foundation', 'Fundacion, asociacion, iglesia u organizacion social', '{"modules":["accounting","customers","basic-reports","sri"],"accent":"#166534","group":"Organizaciones"}'::jsonb),
    ('vertical_template', 'artisan_handmade', 'Artesanias, manualidades o productor independiente', '{"modules":["pos","inventory","sri"],"accent":"#a16207","group":"Produccion"}'::jsonb),
    ('vertical_template', 'gym_sports', 'Gimnasio, deporte o entrenamiento', '{"modules":["pos","customers","sri"],"accent":"#dc2626","group":"Servicios"}'::jsonb),
    ('vertical_template', 'wholesale_distribution', 'Mayorista, distribuidora o importadora', '{"modules":["sales","inventory","purchases","sri"],"accent":"#0f172a","group":"Comercio"}'::jsonb),
    ('vertical_template', 'other_custom', 'Otra actividad / no aparece en la lista', '{"modules":["sales","accounting","sri"],"accent":"#0f766e","group":"Personalizado","custom":true}'::jsonb)
ON CONFLICT (catalog_type, item_code) DO UPDATE
SET label = EXCLUDED.label,
    payload = EXCLUDED.payload,
    active = true;

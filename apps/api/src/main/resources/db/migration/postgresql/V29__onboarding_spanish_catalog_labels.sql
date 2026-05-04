WITH labels(catalog_type, item_code, label) AS (
    VALUES
        ('sri_document_type', '03', 'Liquidación de compra'),
        ('sri_document_type', '04', 'Nota de crédito'),
        ('sri_document_type', '05', 'Nota de débito'),
        ('sri_document_type', '06', 'Guía de remisión'),
        ('sri_document_type', '07', 'Comprobante de retención'),
        ('sri_payment_method', '01', 'Efectivo o sin banco'),
        ('sri_payment_method', '15', 'Compensación de deudas'),
        ('sri_payment_method', '16', 'Tarjeta de débito'),
        ('sri_payment_method', '17', 'Dinero electrónico'),
        ('sri_payment_method', '18', 'Tarjeta prepago'),
        ('sri_payment_method', '19', 'Tarjeta de crédito'),
        ('sri_payment_method', '20', 'Transferencia, depósito u otro pago bancario'),
        ('sri_payment_method', '21', 'Endoso de títulos'),
        ('fiscal_flag', 'retention_agent', 'Soy agente de retención'),
        ('fiscal_flag', 'special_taxpayer', 'Soy contribuyente especial'),
        ('fiscal_flag', 'rimpe', 'Estoy en Régimen RIMPE'),
        ('fiscal_flag', 'large_taxpayer', 'Soy gran contribuyente'),
        ('fiscal_flag', 'habitual_exporter', 'Soy exportador habitual'),
        ('fiscal_flag', 'construction_materials', 'Vendo materiales de construcción'),
        ('fiscal_flag', 'commercial_transport', 'Hago transporte comercial'),
        ('fiscal_flag', 'fuel_sales', 'Vendo combustibles'),
        ('fiscal_flag', 'tourism_special_vat', 'Turismo con tarifa especial'),
        ('vertical_template', 'food_restaurant', 'Restaurante, cafetería, bar o comida preparada'),
        ('vertical_template', 'grocery_store', 'Tienda de barrio, minimarket o abarrotes'),
        ('vertical_template', 'supermarket', 'Supermercado o autoservicio'),
        ('vertical_template', 'retail_clothing', 'Ropa, calzado, accesorios o boutique'),
        ('vertical_template', 'hardware_store', 'Ferretería, materiales o construcción'),
        ('vertical_template', 'pharmacy', 'Farmacia o productos de salud'),
        ('vertical_template', 'medical_services', 'Consultorio, clínica, laboratorio o servicios médicos'),
        ('vertical_template', 'education_services', 'Escuela, colegio, instituto o capacitación'),
        ('vertical_template', 'hotel_tourism', 'Hotel, hostal, turismo o agencia de viajes'),
        ('vertical_template', 'transport_logistics', 'Transporte, encomiendas, delivery o logística'),
        ('vertical_template', 'automotive_workshop', 'Taller automotriz, repuestos o lubricadora'),
        ('vertical_template', 'beauty_wellness', 'Peluquería, spa, belleza o bienestar'),
        ('vertical_template', 'professional_services', 'Servicios profesionales, consultoría o asesoría'),
        ('vertical_template', 'technology_services', 'Tecnología, software, soporte o servicios digitales'),
        ('vertical_template', 'manufacturing', 'Manufactura, producción o taller industrial'),
        ('vertical_template', 'agriculture_livestock', 'Agricultura, ganadería o agroindustria'),
        ('vertical_template', 'fishing_aquaculture', 'Pesca, acuicultura o mariscos'),
        ('vertical_template', 'fuel_station', 'Gasolinera, combustibles o lubricantes'),
        ('vertical_template', 'real_estate', 'Inmobiliaria, arriendos o administración de propiedades'),
        ('vertical_template', 'events_entertainment', 'Eventos, entretenimiento, deportes o cultura'),
        ('vertical_template', 'security_cleaning', 'Seguridad, limpieza o mantenimiento'),
        ('vertical_template', 'financial_cooperative', 'Cooperativa, financiera o servicios de cobro'),
        ('vertical_template', 'nonprofit_foundation', 'Fundación, asociación, iglesia u organización social'),
        ('vertical_template', 'artisan_handmade', 'Artesanías, manualidades o productor independiente'),
        ('vertical_template', 'gym_sports', 'Gimnasio, deporte o entrenamiento'),
        ('vertical_template', 'wholesale_distribution', 'Mayorista, distribuidora o importadora'),
        ('vertical_template', 'other_custom', 'Otra actividad o no aparece en la lista')
)
UPDATE onboarding_catalog_items item
SET label = labels.label
FROM labels
WHERE item.catalog_type = labels.catalog_type
  AND item.item_code = labels.item_code;

UPDATE onboarding_catalog_items
SET active = false
WHERE catalog_type = 'vertical_template'
  AND item_code IN ('restaurant', 'education', 'retail', 'commercial');

UPDATE tenant_onboarding_sessions
SET vertical_template = CASE vertical_template
        WHEN 'restaurant' THEN 'food_restaurant'
        WHEN 'education' THEN 'education_services'
        WHEN 'retail' THEN 'grocery_store'
        WHEN 'commercial' THEN 'wholesale_distribution'
        ELSE vertical_template
    END,
    business_profile = jsonb_set(
        COALESCE(business_profile, '{}'::jsonb),
        '{verticalTemplate}',
        to_jsonb(CASE vertical_template
            WHEN 'restaurant' THEN 'food_restaurant'
            WHEN 'education' THEN 'education_services'
            WHEN 'retail' THEN 'grocery_store'
            WHEN 'commercial' THEN 'wholesale_distribution'
            ELSE vertical_template
        END),
        true)
WHERE vertical_template IN ('restaurant', 'education', 'retail', 'commercial');

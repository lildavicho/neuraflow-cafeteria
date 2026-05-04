ALTER TABLE tax_sri_settings
    ADD COLUMN rimpe_contributor TINYINT(1) NOT NULL DEFAULT 0 AFTER obligated_accounting;

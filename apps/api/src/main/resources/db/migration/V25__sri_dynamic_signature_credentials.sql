ALTER TABLE tax_sri_settings
    ADD COLUMN signature_mode VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER obligated_accounting,
    ADD COLUMN p12_content LONGBLOB NULL AFTER signature_mode,
    ADD COLUMN p12_password_encrypted TEXT NULL AFTER p12_content,
    ADD COLUMN sri_encryption_salt VARCHAR(80) NULL AFTER p12_password_encrypted,
    ADD COLUMN p12_key_alias VARCHAR(120) NULL AFTER sri_encryption_salt,
    ADD COLUMN signature_updated_at DATETIME NULL AFTER p12_key_alias;

UPDATE acc_tax_rules
SET taxable_object_code = '4',
    updated_at = CURRENT_TIMESTAMP
WHERE tax_authority_code = '2'
  AND ROUND(rate, 2) = 15.00
  AND COALESCE(taxable_object_code, '') <> '4';

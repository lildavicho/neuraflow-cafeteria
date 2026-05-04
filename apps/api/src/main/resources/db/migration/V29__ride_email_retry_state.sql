ALTER TABLE tax_documents
    ADD COLUMN ride_email_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN ride_email_next_retry_at DATETIME NULL;

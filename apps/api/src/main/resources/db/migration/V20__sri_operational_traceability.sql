ALTER TABLE tax_document_transmissions
    ADD COLUMN attempt_number INT NOT NULL DEFAULT 1 AFTER http_status,
    ADD COLUMN trace_id VARCHAR(100) NULL AFTER attempt_number,
    ADD COLUMN transport_error TINYINT(1) NOT NULL DEFAULT 0 AFTER trace_id;

UPDATE tax_document_transmissions t
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY tax_document_id, phase ORDER BY attempted_at, id) AS attempt_number
    FROM tax_document_transmissions
) ranked ON ranked.id = t.id
SET t.attempt_number = ranked.attempt_number;

CREATE INDEX idx_tax_document_transmissions_trace
    ON tax_document_transmissions (trace_id);

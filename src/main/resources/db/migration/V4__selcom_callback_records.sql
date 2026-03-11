-- V4: Idempotency for Selcom webhook callbacks
CREATE TABLE IF NOT EXISTS selcom_callback_records (
    id CHAR(36) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    transid VARCHAR(64),
    result VARCHAR(20),
    processed_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_selcom_callback_order ON selcom_callback_records(order_id);
CREATE INDEX IF NOT EXISTS idx_selcom_callback_transid ON selcom_callback_records(transid);

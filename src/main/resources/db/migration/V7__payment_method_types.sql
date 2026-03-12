-- Payment method types (categories) and sub-types for Add Payment Method form.
-- Seeded by V8__seed_payment_method_types.sql

CREATE TABLE IF NOT EXISTS payment_method_types (
    id VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    display_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS payment_method_sub_types (
    id VARCHAR(50) PRIMARY KEY,
    payment_method_type_id VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    display_order INT DEFAULT 0,
    CONSTRAINT fk_sub_type_type FOREIGN KEY (payment_method_type_id) REFERENCES payment_method_types(id)
);

CREATE INDEX IF NOT EXISTS idx_sub_type_type ON payment_method_sub_types(payment_method_type_id);

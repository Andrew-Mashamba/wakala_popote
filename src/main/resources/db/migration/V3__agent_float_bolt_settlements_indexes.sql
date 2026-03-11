-- V3: agent_float_transactions, bolt_settlements (PROJECT.md §6.4), and all indexes from PROJECT.md §6.4
-- Safe to run with IF NOT EXISTS for dev (JPA ddl-auto) and prod (validate).

-- Agent float transactions (PROJECT.md §6.4)
CREATE TABLE IF NOT EXISTS agent_float_transactions (
    id CHAR(36) PRIMARY KEY,
    agent_id CHAR(36) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    request_id CHAR(36),
    settlement_id CHAR(36),
    reference VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_float_agent ON agent_float_transactions(agent_id);
CREATE INDEX IF NOT EXISTS idx_float_created ON agent_float_transactions(created_at);

-- Bolt settlements (optional reconciliation table)
CREATE TABLE IF NOT EXISTS bolt_settlements (
    id CHAR(36) PRIMARY KEY,
    request_id CHAR(36) NOT NULL,
    bolt_job_id VARCHAR(64) NOT NULL,
    reference_id VARCHAR(64),
    payout_amount DECIMAL(15,2) NOT NULL,
    payout_status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bolt_settlement_job ON bolt_settlements(bolt_job_id);
CREATE INDEX IF NOT EXISTS idx_bolt_settlement_request ON bolt_settlements(request_id);
CREATE INDEX IF NOT EXISTS idx_bolt_settlement_status ON bolt_settlements(payout_status);

-- Indexes from PROJECT.md §6.4 (column names as in current schema)
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(uid);
CREATE INDEX IF NOT EXISTS idx_users_location ON users(current_lat, current_lng);
CREATE INDEX IF NOT EXISTS idx_agents_available ON agents(is_available, current_lat, current_lng);
CREATE INDEX IF NOT EXISTS idx_agents_selcom ON agents(selcom_account_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON cash_requests(status);
CREATE INDEX IF NOT EXISTS idx_requests_location ON cash_requests(delivery_lat, delivery_lng);
CREATE INDEX IF NOT EXISTS idx_settlements_status ON settlements(client_debit_status, bolt_settlement_status);
CREATE INDEX IF NOT EXISTS idx_deposits_status ON deposit_requests(status);
CREATE INDEX IF NOT EXISTS idx_deposits_client ON deposit_requests(client_user_id);
CREATE INDEX IF NOT EXISTS idx_deposits_agent ON deposit_requests(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_deposits_location ON deposit_requests(collection_lat, collection_lng);
CREATE INDEX IF NOT EXISTS idx_deposits_bank ON deposit_requests(destination_bank_code);
CREATE INDEX IF NOT EXISTS idx_applications_status ON selcom_account_applications(status);
CREATE INDEX IF NOT EXISTS idx_applications_nida ON selcom_account_applications(nida_number);
CREATE INDEX IF NOT EXISTS idx_applications_phone ON selcom_account_applications(phone_number);
CREATE INDEX IF NOT EXISTS idx_applications_referrer ON selcom_account_applications(referred_by_agent_id);
CREATE INDEX IF NOT EXISTS idx_onboarding_application ON agent_onboarding_progress(application_id);

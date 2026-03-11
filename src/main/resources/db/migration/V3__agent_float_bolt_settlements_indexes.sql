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

-- Indexes on users, agents, cash_requests, etc. are created by JPA @Table annotations
-- when ddl-auto=update. Skipped here since Flyway runs before JPA schema creation.

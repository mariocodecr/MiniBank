CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('CRC', 'USD')),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query performance
CREATE INDEX idx_ledger_entries_payment_id ON ledger_entries(payment_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_occurred_at ON ledger_entries(occurred_at);
CREATE INDEX idx_ledger_entries_type_account ON ledger_entries(entry_type, account_id);

-- Composite index for common queries
CREATE INDEX idx_ledger_entries_payment_type ON ledger_entries(payment_id, entry_type);

COMMENT ON TABLE ledger_entries IS 'Append-only double-entry ledger for all financial transactions';
COMMENT ON COLUMN ledger_entries.amount_minor IS 'Amount stored in minor units (cents), always positive';
COMMENT ON COLUMN ledger_entries.entry_type IS 'DEBIT (money out) or CREDIT (money in)';
COMMENT ON COLUMN ledger_entries.occurred_at IS 'Business timestamp when entry was recorded';
COMMENT ON COLUMN ledger_entries.created_at IS 'Technical timestamp when row was inserted';

-- Constraint to ensure ledger entries reference valid payments
-- (This would be a foreign key in a single-service scenario)
-- For microservices, we rely on application-level consistency
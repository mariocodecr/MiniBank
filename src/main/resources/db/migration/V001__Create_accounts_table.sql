CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('CRC', 'USD')),
    balance_minor BIGINT NOT NULL DEFAULT 0 CHECK (balance_minor >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_accounts_status ON accounts(status);

CREATE UNIQUE INDEX idx_accounts_user_currency ON accounts(user_id, currency)
    WHERE status != 'CLOSED';

COMMENT ON TABLE accounts IS 'Bank accounts with optimistic locking support';
COMMENT ON COLUMN accounts.balance_minor IS 'Balance stored in minor units (cents)';
COMMENT ON COLUMN accounts.version IS 'Version for optimistic locking';
COMMENT ON CONSTRAINT accounts_balance_minor_check ON accounts IS 'Balance cannot be negative';
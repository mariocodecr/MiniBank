CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id VARCHAR(255) NOT NULL UNIQUE,
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('CRC', 'USD')),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN (
        'REQUESTED', 'DEBITED', 'CREDITED', 'COMPLETED',
        'FAILED_INSUFFICIENT_FUNDS', 'FAILED_ACCOUNT_INACTIVE', 
        'FAILED_SYSTEM_ERROR', 'COMPENSATED'
    )),
    failure_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_request_id ON payments(request_id);
CREATE INDEX idx_payments_from_account ON payments(from_account_id);
CREATE INDEX idx_payments_to_account ON payments(to_account_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

COMMENT ON TABLE payments IS 'Payment transactions with saga orchestration status tracking';
COMMENT ON COLUMN payments.amount_minor IS 'Amount stored in minor units (cents)';
COMMENT ON COLUMN payments.request_id IS 'Client-provided idempotency key';
COMMENT ON COLUMN payments.version IS 'Version for optimistic locking';
COMMENT ON COLUMN payments.status IS 'Payment status in saga orchestration flow';
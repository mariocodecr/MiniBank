-- Payment Saga state management table
CREATE TABLE payment_saga (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL UNIQUE,
    request_id VARCHAR(255) NOT NULL,
    saga_state VARCHAR(50) NOT NULL,
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    completion_status VARCHAR(50),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for finding active sagas
CREATE INDEX idx_payment_saga_active ON payment_saga (saga_state, started_at) 
WHERE saga_state IN ('STARTED', 'DEBITING', 'CREDITING', 'COMPLETING');

-- Index for payment lookup
CREATE UNIQUE INDEX idx_payment_saga_payment_id ON payment_saga (payment_id);

-- Index for request ID lookup (idempotency)
CREATE INDEX idx_payment_saga_request_id ON payment_saga (request_id);

-- Index for cleanup and monitoring
CREATE INDEX idx_payment_saga_completed_at ON payment_saga (completed_at) 
WHERE completed_at IS NOT NULL;
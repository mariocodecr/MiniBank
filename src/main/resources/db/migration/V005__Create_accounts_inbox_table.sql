-- Inbox table for accounts service to handle payment events
CREATE TABLE accounts_events_inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    error_message TEXT,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for finding unprocessed events
CREATE INDEX idx_accounts_inbox_unprocessed ON accounts_events_inbox (processed, received_at) 
WHERE processed = false;

-- Index for event deduplication
CREATE UNIQUE INDEX idx_accounts_inbox_event_id ON accounts_events_inbox (event_id);

-- Index for cleanup queries
CREATE INDEX idx_accounts_inbox_processed_at ON accounts_events_inbox (processed_at) 
WHERE processed = true;
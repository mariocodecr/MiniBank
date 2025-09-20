CREATE TABLE payment_events_outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    payment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP NULL,
    error_message TEXT NULL
);

CREATE INDEX idx_payment_outbox_published ON payment_events_outbox(published, created_at);
CREATE INDEX idx_payment_outbox_payment_id ON payment_events_outbox(payment_id);
CREATE INDEX idx_payment_outbox_event_type ON payment_events_outbox(event_type);
CREATE INDEX idx_payment_outbox_correlation_id ON payment_events_outbox(correlation_id);

COMMENT ON TABLE payment_events_outbox IS 'Transactional outbox for payment events';
COMMENT ON COLUMN payment_events_outbox.event_id IS 'Unique event identifier for deduplication';
COMMENT ON COLUMN payment_events_outbox.payload IS 'Event payload in JSON format';
COMMENT ON COLUMN payment_events_outbox.published IS 'Whether event has been successfully published';
COMMENT ON COLUMN payment_events_outbox.retry_count IS 'Number of publish retry attempts';
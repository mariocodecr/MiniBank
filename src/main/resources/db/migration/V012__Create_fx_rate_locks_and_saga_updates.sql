-- FX rate locks for payment processing
CREATE TABLE fx_rate_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lock_id VARCHAR(100) UNIQUE NOT NULL,
    payment_id UUID,
    saga_id UUID,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    locked_rate DECIMAL(15,8) NOT NULL,
    spread_bps INTEGER NOT NULL,
    effective_rate DECIMAL(15,8) NOT NULL, -- Rate including spread
    lock_amount_minor BIGINT NOT NULL,
    converted_amount_minor BIGINT,
    lock_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    provider VARCHAR(50) NOT NULL,
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    released_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_lock_amount_positive CHECK (lock_amount_minor > 0),
    CONSTRAINT check_lock_status CHECK (lock_status IN ('ACTIVE', 'USED', 'EXPIRED', 'RELEASED', 'FAILED')),
    CONSTRAINT check_different_currencies CHECK (base_currency != quote_currency),
    CONSTRAINT check_effective_rate_positive CHECK (effective_rate > 0),
    CONSTRAINT check_expires_after_locked CHECK (expires_at > locked_at)
);

-- Indexes for FX rate locks
CREATE INDEX idx_fx_rate_locks_lock_id ON fx_rate_locks(lock_id);
CREATE INDEX idx_fx_rate_locks_payment_id ON fx_rate_locks(payment_id);
CREATE INDEX idx_fx_rate_locks_saga_id ON fx_rate_locks(saga_id);
CREATE INDEX idx_fx_rate_locks_status_expires ON fx_rate_locks(lock_status, expires_at);
CREATE INDEX idx_fx_rate_locks_currencies ON fx_rate_locks(base_currency, quote_currency);
CREATE INDEX idx_fx_rate_locks_expires_at ON fx_rate_locks(expires_at) WHERE lock_status = 'ACTIVE';

-- Cross-currency payment saga improvements
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS from_currency VARCHAR(3);
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS to_currency VARCHAR(3);
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS fx_rate_lock_id UUID;
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS locked_exchange_rate DECIMAL(15,8);
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS fx_spread_bps INTEGER;
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS fx_provider VARCHAR(50);
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS rate_lock_expires_at TIMESTAMP;
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS fx_conversion_id UUID;
ALTER TABLE payment_saga ADD COLUMN IF NOT EXISTS converted_amount_minor BIGINT;

-- Add foreign key constraint for FX rate lock
ALTER TABLE payment_saga 
ADD CONSTRAINT fk_payment_saga_fx_rate_lock 
FOREIGN KEY (fx_rate_lock_id) REFERENCES fx_rate_locks(id);

-- Payment saga step tracking for cross-currency payments
CREATE TABLE payment_saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_id UUID NOT NULL REFERENCES payment_saga(id),
    step_name VARCHAR(50) NOT NULL,
    step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    step_order INTEGER NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    step_data JSONB,
    compensation_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_step_status CHECK (step_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT unique_saga_step_order UNIQUE (saga_id, step_order)
);

-- Indexes for saga steps
CREATE INDEX idx_payment_saga_steps_saga_id ON payment_saga_steps(saga_id, step_order);
CREATE INDEX idx_payment_saga_steps_status ON payment_saga_steps(step_status, created_at DESC);
CREATE INDEX idx_payment_saga_steps_step_name ON payment_saga_steps(step_name, step_status);

-- FX conversion audit log
CREATE TABLE fx_conversion_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversion_id UUID NOT NULL,
    lock_id UUID,
    payment_id UUID,
    event_type VARCHAR(30) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    rate_before DECIMAL(15,8),
    rate_after DECIMAL(15,8),
    amount_before_minor BIGINT,
    amount_after_minor BIGINT,
    event_data JSONB,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    CONSTRAINT check_event_type CHECK (event_type IN (
        'RATE_LOCKED', 'RATE_LOCK_EXPIRED', 'RATE_LOCK_RELEASED', 
        'CONVERSION_STARTED', 'CONVERSION_COMPLETED', 'CONVERSION_FAILED',
        'AMOUNT_ADJUSTED', 'MANUAL_INTERVENTION', 'SYSTEM_CORRECTION'
    ))
);

-- Indexes for conversion audit
CREATE INDEX idx_fx_conversion_audit_conversion_id ON fx_conversion_audit(conversion_id, event_timestamp DESC);
CREATE INDEX idx_fx_conversion_audit_lock_id ON fx_conversion_audit(lock_id, event_timestamp DESC);
CREATE INDEX idx_fx_conversion_audit_payment_id ON fx_conversion_audit(payment_id, event_timestamp DESC);
CREATE INDEX idx_fx_conversion_audit_event_type ON fx_conversion_audit(event_type, event_timestamp DESC);

-- Treasury risk metrics (daily aggregations)
CREATE TABLE treasury_risk_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL DEFAULT CURRENT_DATE,
    currency VARCHAR(3) NOT NULL,
    total_exposure_minor BIGINT NOT NULL DEFAULT 0,
    max_exposure_minor BIGINT NOT NULL DEFAULT 0,
    exposure_threshold_minor BIGINT NOT NULL DEFAULT 0,
    threshold_breaches_count INTEGER NOT NULL DEFAULT 0,
    avg_daily_volume_minor BIGINT NOT NULL DEFAULT 0,
    max_single_transaction_minor BIGINT NOT NULL DEFAULT 0,
    cross_border_volume_minor BIGINT NOT NULL DEFAULT 0,
    cross_border_transaction_count INTEGER NOT NULL DEFAULT 0,
    var_95_minor BIGINT, -- Value at Risk (95% confidence)
    var_99_minor BIGINT, -- Value at Risk (99% confidence)
    volatility_percentage DECIMAL(5,4), -- Daily volatility
    liquidity_ratio DECIMAL(5,4), -- Available liquidity / Total exposure
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_currency_metric_date UNIQUE (currency, metric_date),
    CONSTRAINT check_exposure_non_negative CHECK (total_exposure_minor >= 0),
    CONSTRAINT check_var_values CHECK (var_95_minor IS NULL OR var_99_minor IS NULL OR var_95_minor <= var_99_minor)
);

-- Indexes for risk metrics
CREATE INDEX idx_treasury_risk_metrics_currency_date ON treasury_risk_metrics(currency, metric_date DESC);
CREATE INDEX idx_treasury_risk_metrics_date ON treasury_risk_metrics(metric_date DESC);
CREATE INDEX idx_treasury_risk_metrics_exposure ON treasury_risk_metrics(total_exposure_minor DESC, metric_date DESC);

-- Compliance case management
CREATE TABLE compliance_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(50) UNIQUE NOT NULL,
    case_type VARCHAR(30) NOT NULL,
    case_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    account_id UUID,
    payment_id UUID,
    screening_result_id UUID,
    assigned_to VARCHAR(100),
    case_description TEXT,
    investigation_notes TEXT,
    resolution_notes TEXT,
    escalated_to VARCHAR(100),
    escalated_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    due_date TIMESTAMP,
    regulatory_filing_required BOOLEAN NOT NULL DEFAULT false,
    regulatory_filing_completed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_case_type CHECK (case_type IN (
        'AML_ALERT', 'SANCTIONS_HIT', 'PEP_MATCH', 'SUSPICIOUS_ACTIVITY',
        'HIGH_RISK_TRANSACTION', 'THRESHOLD_BREACH', 'MANUAL_REVIEW'
    )),
    CONSTRAINT check_case_status CHECK (case_status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED', 'RESOLVED', 'CLOSED')),
    CONSTRAINT check_priority_level CHECK (priority_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Indexes for compliance cases
CREATE INDEX idx_compliance_cases_case_number ON compliance_cases(case_number);
CREATE INDEX idx_compliance_cases_status_priority ON compliance_cases(case_status, priority_level, created_at DESC);
CREATE INDEX idx_compliance_cases_account_id ON compliance_cases(account_id);
CREATE INDEX idx_compliance_cases_payment_id ON compliance_cases(payment_id);
CREATE INDEX idx_compliance_cases_assigned_to ON compliance_cases(assigned_to, case_status);
CREATE INDEX idx_compliance_cases_due_date ON compliance_cases(due_date) WHERE case_status IN ('OPEN', 'IN_PROGRESS');

-- Create update triggers for new tables
CREATE TRIGGER update_fx_rate_locks_updated_at 
    BEFORE UPDATE ON fx_rate_locks 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_saga_steps_updated_at 
    BEFORE UPDATE ON payment_saga_steps 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_treasury_risk_metrics_updated_at 
    BEFORE UPDATE ON treasury_risk_metrics 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_compliance_cases_updated_at 
    BEFORE UPDATE ON compliance_cases 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to clean up expired rate locks
CREATE OR REPLACE FUNCTION cleanup_expired_fx_rate_locks()
RETURNS INTEGER AS $$
DECLARE
    expired_count INTEGER;
BEGIN
    UPDATE fx_rate_locks 
    SET lock_status = 'EXPIRED', 
        updated_at = CURRENT_TIMESTAMP
    WHERE lock_status = 'ACTIVE' 
      AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS expired_count = ROW_COUNT;
    
    RETURN expired_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to calculate daily risk metrics
CREATE OR REPLACE FUNCTION calculate_daily_risk_metrics(target_date DATE DEFAULT CURRENT_DATE)
RETURNS VOID AS $$
DECLARE
    currency_rec RECORD;
BEGIN
    -- For each supported currency
    FOR currency_rec IN SELECT currency_code FROM supported_currencies WHERE is_active = true
    LOOP
        INSERT INTO treasury_risk_metrics (
            metric_date,
            currency,
            total_exposure_minor,
            max_exposure_minor,
            exposure_threshold_minor,
            threshold_breaches_count,
            avg_daily_volume_minor,
            max_single_transaction_minor,
            cross_border_volume_minor,
            cross_border_transaction_count
        )
        SELECT 
            target_date,
            currency_rec.currency_code,
            COALESCE(cp.net_position_minor, 0) as total_exposure_minor,
            COALESCE(cp.net_position_minor, 0) as max_exposure_minor,
            COALESCE(cp.exposure_limit_minor, 0) as exposure_threshold_minor,
            COALESCE(alert_counts.breach_count, 0) as threshold_breaches_count,
            COALESCE(payment_stats.avg_volume, 0) as avg_daily_volume_minor,
            COALESCE(payment_stats.max_amount, 0) as max_single_transaction_minor,
            COALESCE(cross_border_stats.volume, 0) as cross_border_volume_minor,
            COALESCE(cross_border_stats.count, 0) as cross_border_transaction_count
        FROM (SELECT currency_rec.currency_code as currency) base
        LEFT JOIN currency_positions cp ON cp.currency = currency_rec.currency_code AND cp.position_date = target_date
        LEFT JOIN (
            SELECT COUNT(*) as breach_count
            FROM exposure_alerts 
            WHERE currency = currency_rec.currency_code 
              AND alert_type = 'EXPOSURE_BREACH'
              AND DATE(created_at) = target_date
        ) alert_counts ON true
        LEFT JOIN (
            -- This would need to join with actual payment tables when available
            SELECT 0 as avg_volume, 0 as max_amount
        ) payment_stats ON true
        LEFT JOIN (
            SELECT 
                COALESCE(SUM(transaction_amount_minor), 0) as volume,
                COUNT(*) as count
            FROM cross_border_transactions 
            WHERE transaction_currency = currency_rec.currency_code
              AND DATE(created_at) = target_date
        ) cross_border_stats ON true
        ON CONFLICT (currency, metric_date) DO UPDATE SET
            total_exposure_minor = EXCLUDED.total_exposure_minor,
            max_exposure_minor = EXCLUDED.max_exposure_minor,
            exposure_threshold_minor = EXCLUDED.exposure_threshold_minor,
            threshold_breaches_count = EXCLUDED.threshold_breaches_count,
            avg_daily_volume_minor = EXCLUDED.avg_daily_volume_minor,
            max_single_transaction_minor = EXCLUDED.max_single_transaction_minor,
            cross_border_volume_minor = EXCLUDED.cross_border_volume_minor,
            cross_border_transaction_count = EXCLUDED.cross_border_transaction_count,
            updated_at = CURRENT_TIMESTAMP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Insert initial compliance case number sequence
CREATE SEQUENCE compliance_case_sequence START 1000;

-- Create function to generate case numbers
CREATE OR REPLACE FUNCTION generate_case_number()
RETURNS TEXT AS $$
BEGIN
    RETURN 'CASE-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || 
           LPAD(nextval('compliance_case_sequence')::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

-- Add some initial FX rate data for testing
INSERT INTO fx_rates (base_currency, quote_currency, rate, spread, provider, effective_timestamp) VALUES
('USD', 'EUR', 0.8525, 0.0015, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('EUR', 'USD', 1.1728, 0.0015, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'GBP', 0.7850, 0.0018, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('GBP', 'USD', 1.2739, 0.0018, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'JPY', 110.25, 0.0012, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('JPY', 'USD', 0.009070, 0.0012, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'CAD', 1.2580, 0.0010, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('CAD', 'USD', 0.7949, 0.0010, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'AUD', 1.3420, 0.0016, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('AUD', 'USD', 0.7453, 0.0016, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'CHF', 0.9180, 0.0014, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('CHF', 'USD', 1.0894, 0.0014, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('USD', 'CRC', 625.50, 0.0030, 'MOCK_PROVIDER', CURRENT_TIMESTAMP),
('CRC', 'USD', 0.001599, 0.0030, 'MOCK_PROVIDER', CURRENT_TIMESTAMP);

-- Create initial treasury risk metrics for today
SELECT calculate_daily_risk_metrics(CURRENT_DATE);

-- Add comments to tables for documentation
COMMENT ON TABLE fx_rate_locks IS 'FX rate locks for ensuring consistent rates during payment processing';
COMMENT ON TABLE payment_saga_steps IS 'Detailed step tracking for payment saga orchestration';
COMMENT ON TABLE fx_conversion_audit IS 'Audit trail for all FX conversion activities';
COMMENT ON TABLE treasury_risk_metrics IS 'Daily aggregated risk metrics by currency';
COMMENT ON TABLE compliance_cases IS 'Compliance case management for AML and regulatory issues';

COMMENT ON COLUMN fx_rate_locks.effective_rate IS 'Exchange rate including spread, used for actual conversion';
COMMENT ON COLUMN fx_rate_locks.expires_at IS 'Rate lock expiration time, typically 15-30 minutes';
COMMENT ON COLUMN treasury_risk_metrics.var_95_minor IS 'Value at Risk at 95% confidence level in minor currency units';
COMMENT ON COLUMN treasury_risk_metrics.liquidity_ratio IS 'Ratio of available liquidity to total exposure';
COMMENT ON COLUMN compliance_cases.case_number IS 'Human-readable case identifier (e.g., CASE-2024-001234)';
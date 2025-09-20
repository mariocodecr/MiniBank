-- Treasury currency positions and exposures
CREATE TABLE currency_positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency VARCHAR(3) NOT NULL,
    long_position_minor BIGINT NOT NULL DEFAULT 0,
    short_position_minor BIGINT NOT NULL DEFAULT 0,
    net_position_minor BIGINT NOT NULL DEFAULT 0,
    exposure_limit_minor BIGINT NOT NULL DEFAULT 1000000000, -- 10M default limit
    liquidity_threshold_minor BIGINT NOT NULL DEFAULT 100000000, -- 1M default threshold
    last_rebalanced_at TIMESTAMP,
    position_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_currency_date UNIQUE (currency, position_date),
    CONSTRAINT check_net_position CHECK (net_position_minor = long_position_minor - short_position_minor)
);

-- Index for currency position queries
CREATE INDEX idx_currency_positions_currency_date ON currency_positions(currency, position_date DESC);
CREATE INDEX idx_currency_positions_updated_at ON currency_positions(updated_at DESC);

-- Insert initial currency positions
INSERT INTO currency_positions (currency, position_date) VALUES
('USD', CURRENT_DATE),
('EUR', CURRENT_DATE),
('GBP', CURRENT_DATE),
('JPY', CURRENT_DATE),
('CAD', CURRENT_DATE),
('AUD', CURRENT_DATE),
('CHF', CURRENT_DATE),
('CNY', CURRENT_DATE),
('CRC', CURRENT_DATE);

-- Treasury exposure alerts
CREATE TABLE exposure_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency VARCHAR(3) NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    threshold_minor BIGINT NOT NULL,
    current_value_minor BIGINT NOT NULL,
    breach_percentage DECIMAL(5,2),
    alert_level VARCHAR(10) NOT NULL,
    alert_message TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_alert_type CHECK (alert_type IN ('EXPOSURE_BREACH', 'LIQUIDITY_LOW', 'POSITION_LIMIT')),
    CONSTRAINT check_alert_level CHECK (alert_level IN ('INFO', 'WARNING', 'CRITICAL'))
);

-- Index for active alerts
CREATE INDEX idx_exposure_alerts_active_currency ON exposure_alerts(is_active, currency, created_at DESC);
CREATE INDEX idx_exposure_alerts_level_created ON exposure_alerts(alert_level, created_at DESC);

-- Hedging transactions (basic structure for future enhancement)
CREATE TABLE hedging_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hedge_id VARCHAR(100) UNIQUE NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    hedge_amount_minor BIGINT NOT NULL,
    hedge_rate DECIMAL(15,8),
    hedge_type VARCHAR(20) NOT NULL,
    hedge_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    execution_timestamp TIMESTAMP,
    expiry_timestamp TIMESTAMP,
    counterparty VARCHAR(100),
    reference_exposure_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_hedge_type CHECK (hedge_type IN ('FORWARD', 'SPOT', 'OPTION', 'SWAP')),
    CONSTRAINT check_hedge_status CHECK (hedge_status IN ('PENDING', 'EXECUTED', 'EXPIRED', 'CANCELLED'))
);

-- AML screening results
CREATE TABLE aml_screening_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID,
    account_id UUID,
    screening_type VARCHAR(20) NOT NULL,
    risk_score INTEGER NOT NULL DEFAULT 0,
    risk_level VARCHAR(10) NOT NULL,
    screening_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    alerts_triggered TEXT[],
    sanctions_hits TEXT[],
    pep_matches TEXT[],
    screening_provider VARCHAR(50),
    screening_reference VARCHAR(100),
    manual_review_required BOOLEAN NOT NULL DEFAULT false,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    review_decision VARCHAR(20),
    review_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_screening_type CHECK (screening_type IN ('PAYMENT', 'ACCOUNT', 'CROSS_BORDER', 'HIGH_VALUE')),
    CONSTRAINT check_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT check_screening_status CHECK (screening_status IN ('PENDING', 'COMPLETED', 'FAILED', 'MANUAL_REVIEW')),
    CONSTRAINT check_review_decision CHECK (review_decision IS NULL OR review_decision IN ('APPROVED', 'REJECTED', 'ESCALATED'))
);

-- Indexes for AML screening
CREATE INDEX idx_aml_screening_payment_id ON aml_screening_results(payment_id);
CREATE INDEX idx_aml_screening_account_id ON aml_screening_results(account_id);
CREATE INDEX idx_aml_screening_status_created ON aml_screening_results(screening_status, created_at DESC);
CREATE INDEX idx_aml_screening_manual_review ON aml_screening_results(manual_review_required, created_at DESC) WHERE manual_review_required = true;
CREATE INDEX idx_aml_screening_risk_level ON aml_screening_results(risk_level, created_at DESC);

-- Regulatory reporting queue
CREATE TABLE regulatory_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type VARCHAR(20) NOT NULL,
    reporting_period DATE NOT NULL,
    currency VARCHAR(3),
    transaction_count INTEGER NOT NULL DEFAULT 0,
    total_amount_minor BIGINT NOT NULL DEFAULT 0,
    report_data JSONB NOT NULL,
    report_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filed_at TIMESTAMP,
    filing_reference VARCHAR(100),
    regulatory_authority VARCHAR(50),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_report_type CHECK (report_type IN ('CTR', 'SAR', 'FBAR', 'BSA', 'EU_REPORTING')),
    CONSTRAINT check_report_status CHECK (report_status IN ('PENDING', 'GENERATED', 'FILED', 'FAILED', 'ACKNOWLEDGED')),
    CONSTRAINT unique_report_type_period UNIQUE (report_type, reporting_period, currency)
);

-- Indexes for regulatory reporting
CREATE INDEX idx_regulatory_reports_status_created ON regulatory_reports(report_status, created_at DESC);
CREATE INDEX idx_regulatory_reports_type_period ON regulatory_reports(report_type, reporting_period DESC);
CREATE INDEX idx_regulatory_reports_retry ON regulatory_reports(next_retry_at) WHERE report_status = 'FAILED' AND next_retry_at IS NOT NULL;

-- Cross-border transaction monitoring
CREATE TABLE cross_border_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    source_country VARCHAR(3),
    destination_country VARCHAR(3),
    transaction_amount_minor BIGINT NOT NULL,
    transaction_currency VARCHAR(3) NOT NULL,
    usd_equivalent_minor BIGINT,
    swift_code VARCHAR(11),
    correspondent_bank VARCHAR(100),
    purpose_code VARCHAR(10),
    reporting_threshold_breached BOOLEAN NOT NULL DEFAULT false,
    requires_reporting BOOLEAN NOT NULL DEFAULT false,
    reported_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_countries_different CHECK (source_country IS NULL OR destination_country IS NULL OR source_country != destination_country)
);

-- Indexes for cross-border monitoring
CREATE INDEX idx_cross_border_payment_id ON cross_border_transactions(payment_id);
CREATE INDEX idx_cross_border_reporting ON cross_border_transactions(requires_reporting, reported_at) WHERE requires_reporting = true;
CREATE INDEX idx_cross_border_threshold ON cross_border_transactions(reporting_threshold_breached, created_at DESC) WHERE reporting_threshold_breached = true;
CREATE INDEX idx_cross_border_amount_currency ON cross_border_transactions(transaction_currency, transaction_amount_minor DESC);

-- Update triggers for all new tables
CREATE TRIGGER update_currency_positions_updated_at 
    BEFORE UPDATE ON currency_positions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hedging_transactions_updated_at 
    BEFORE UPDATE ON hedging_transactions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_aml_screening_results_updated_at 
    BEFORE UPDATE ON aml_screening_results 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_regulatory_reports_updated_at 
    BEFORE UPDATE ON regulatory_reports 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
-- FX rates storage and historical tracking
CREATE TABLE fx_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(15,8) NOT NULL,
    bid_rate DECIMAL(15,8),
    ask_rate DECIMAL(15,8),
    spread DECIMAL(6,4) DEFAULT 0.0015, -- 15 basis points default
    provider VARCHAR(50) NOT NULL,
    effective_timestamp TIMESTAMP NOT NULL,
    received_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_rate_positive CHECK (rate > 0),
    CONSTRAINT check_bid_rate_positive CHECK (bid_rate IS NULL OR bid_rate > 0),
    CONSTRAINT check_ask_rate_positive CHECK (ask_rate IS NULL OR ask_rate > 0),
    CONSTRAINT check_spread_non_negative CHECK (spread >= 0),
    CONSTRAINT check_different_currencies CHECK (base_currency != quote_currency)
);

-- Indexes for optimal query performance
CREATE INDEX idx_fx_rates_currencies_timestamp ON fx_rates(base_currency, quote_currency, effective_timestamp DESC);
CREATE INDEX idx_fx_rates_provider_timestamp ON fx_rates(provider, effective_timestamp DESC);
CREATE INDEX idx_fx_rates_active_timestamp ON fx_rates(is_active, effective_timestamp DESC) WHERE is_active = true;
CREATE INDEX idx_fx_rates_received_timestamp ON fx_rates(received_timestamp DESC);

-- FX rate providers configuration
CREATE TABLE fx_rate_providers (
    provider_name VARCHAR(50) PRIMARY KEY,
    provider_url VARCHAR(500),
    api_key_reference VARCHAR(100), -- Reference to secure key storage
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    priority_order INTEGER NOT NULL DEFAULT 999,
    rate_refresh_interval_seconds INTEGER NOT NULL DEFAULT 30,
    timeout_seconds INTEGER NOT NULL DEFAULT 5,
    max_retries INTEGER NOT NULL DEFAULT 3,
    supported_currencies TEXT[], -- Array of supported currency codes
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_primary_provider EXCLUDE (is_primary WITH =) WHERE (is_primary = true)
);

-- Insert FX rate provider configurations
INSERT INTO fx_rate_providers (provider_name, is_primary, is_active, priority_order, rate_refresh_interval_seconds, supported_currencies) VALUES
('MOCK_PROVIDER', true, true, 1, 30, ARRAY['USD','EUR','GBP','JPY','CAD','AUD','CHF','CNY','CRC']),
('FALLBACK_PROVIDER', false, true, 2, 60, ARRAY['USD','EUR','GBP','JPY','CAD','AUD','CHF','CNY','CRC']);

-- FX spread configurations by currency pair
CREATE TABLE fx_spread_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    spread_bps INTEGER NOT NULL DEFAULT 15, -- Basis points (15 = 0.15%)
    min_spread_bps INTEGER NOT NULL DEFAULT 5,
    max_spread_bps INTEGER NOT NULL DEFAULT 100,
    volume_tier_1_threshold BIGINT DEFAULT 100000, -- Minor units
    volume_tier_1_spread_bps INTEGER DEFAULT 10,
    volume_tier_2_threshold BIGINT DEFAULT 1000000,
    volume_tier_2_spread_bps INTEGER DEFAULT 8,
    off_hours_spread_multiplier DECIMAL(3,2) DEFAULT 1.5,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_currency_pair UNIQUE (base_currency, quote_currency),
    CONSTRAINT check_spread_positive CHECK (spread_bps > 0),
    CONSTRAINT check_min_max_spread CHECK (min_spread_bps <= spread_bps AND spread_bps <= max_spread_bps)
);

-- Insert default spread configurations
INSERT INTO fx_spread_config (base_currency, quote_currency, spread_bps) VALUES
('USD', 'EUR', 15),
('EUR', 'USD', 15),
('USD', 'GBP', 18),
('GBP', 'USD', 18),
('USD', 'JPY', 12),
('JPY', 'USD', 12),
('USD', 'CAD', 10),
('CAD', 'USD', 10),
('USD', 'AUD', 16),
('AUD', 'USD', 16),
('USD', 'CHF', 14),
('CHF', 'USD', 14),
('USD', 'CNY', 25),
('CNY', 'USD', 25),
('USD', 'CRC', 30),
('CRC', 'USD', 30),
('EUR', 'GBP', 20),
('GBP', 'EUR', 20);

-- FX conversion audit trail
CREATE TABLE fx_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversion_id VARCHAR(100) UNIQUE NOT NULL,
    payment_id UUID,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    base_amount_minor BIGINT NOT NULL,
    quote_amount_minor BIGINT NOT NULL,
    exchange_rate DECIMAL(15,8) NOT NULL,
    spread_bps INTEGER NOT NULL,
    spread_amount_minor BIGINT NOT NULL,
    rate_quote_id VARCHAR(100),
    rate_locked_at TIMESTAMP,
    rate_expired_at TIMESTAMP,
    conversion_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    converted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_conversion_amounts_positive CHECK (base_amount_minor > 0 AND quote_amount_minor > 0),
    CONSTRAINT check_conversion_status CHECK (conversion_status IN ('PENDING', 'LOCKED', 'CONVERTED', 'EXPIRED', 'FAILED'))
);

-- Indexes for conversion queries
CREATE INDEX idx_fx_conversions_payment_id ON fx_conversions(payment_id);
CREATE INDEX idx_fx_conversions_conversion_id ON fx_conversions(conversion_id);
CREATE INDEX idx_fx_conversions_status_created ON fx_conversions(conversion_status, created_at);
CREATE INDEX idx_fx_conversions_rate_quote_id ON fx_conversions(rate_quote_id);

-- Update triggers
CREATE TRIGGER update_fx_rate_providers_updated_at 
    BEFORE UPDATE ON fx_rate_providers 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fx_spread_config_updated_at 
    BEFORE UPDATE ON fx_spread_config 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fx_conversions_updated_at 
    BEFORE UPDATE ON fx_conversions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
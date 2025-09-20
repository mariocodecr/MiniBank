-- Multi-currency account balances
CREATE TABLE account_currency_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    currency VARCHAR(3) NOT NULL,
    available_amount_minor BIGINT NOT NULL DEFAULT 0,
    reserved_amount_minor BIGINT NOT NULL DEFAULT 0,
    total_amount_minor BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    
    CONSTRAINT check_available_non_negative CHECK (available_amount_minor >= 0),
    CONSTRAINT check_reserved_non_negative CHECK (reserved_amount_minor >= 0),
    CONSTRAINT check_total_equals_sum CHECK (total_amount_minor = available_amount_minor + reserved_amount_minor),
    CONSTRAINT unique_account_currency UNIQUE (account_id, currency)
);

-- Indexes for performance
CREATE INDEX idx_account_currency_balances_account_id ON account_currency_balances(account_id);
CREATE INDEX idx_account_currency_balances_currency ON account_currency_balances(currency);
CREATE INDEX idx_account_currency_balances_updated_at ON account_currency_balances(updated_at);

-- Supported currencies configuration
CREATE TABLE supported_currencies (
    currency_code VARCHAR(3) PRIMARY KEY,
    currency_name VARCHAR(100) NOT NULL,
    decimal_places INTEGER NOT NULL DEFAULT 2,
    minimum_amount_minor BIGINT NOT NULL DEFAULT 0,
    maximum_amount_minor BIGINT,
    symbol VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial supported currencies
INSERT INTO supported_currencies (currency_code, currency_name, decimal_places, minimum_amount_minor, symbol, is_active) VALUES
('USD', 'US Dollar', 2, 1, '$', true),
('EUR', 'Euro', 2, 1, '€', true),
('GBP', 'British Pound', 2, 1, '£', true),
('JPY', 'Japanese Yen', 0, 1, '¥', true),
('CAD', 'Canadian Dollar', 2, 1, 'C$', true),
('AUD', 'Australian Dollar', 2, 1, 'A$', true),
('CHF', 'Swiss Franc', 2, 1, 'CHF', true),
('CNY', 'Chinese Yuan', 2, 1, '¥', true),
('CRC', 'Costa Rican Colon', 2, 1, '₡', true);

-- Migrate existing account balances to multi-currency (USD default)
INSERT INTO account_currency_balances (account_id, currency, available_amount_minor, reserved_amount_minor, total_amount_minor, created_at, updated_at)
SELECT 
    id as account_id,
    'USD' as currency,
    COALESCE(balance_minor, 0) as available_amount_minor,
    0 as reserved_amount_minor,
    COALESCE(balance_minor, 0) as total_amount_minor,
    created_at,
    updated_at
FROM accounts
WHERE balance_minor IS NOT NULL;

-- Add foreign key constraint after data migration
ALTER TABLE account_currency_balances 
ADD CONSTRAINT fk_currency_code 
FOREIGN KEY (currency) REFERENCES supported_currencies(currency_code);

-- Update trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_account_currency_balances_updated_at 
    BEFORE UPDATE ON account_currency_balances 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_supported_currencies_updated_at 
    BEFORE UPDATE ON supported_currencies 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
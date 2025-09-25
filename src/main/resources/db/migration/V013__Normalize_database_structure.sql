-- V013: Normalize database structure according to normalization rules
-- This migration fixes several normalization violations identified in the schema

-- Step 1: Create FX providers table (new normalized entity)
CREATE TABLE fx_providers (
    provider_code VARCHAR(20) PRIMARY KEY,
    provider_name VARCHAR(100) NOT NULL,
    default_spread NUMERIC(15, 8) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    priority_order INTEGER NOT NULL,
    rate_validity_minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default FX providers
INSERT INTO fx_providers (provider_code, provider_name, default_spread, priority_order, rate_validity_minutes) VALUES
('FIXER_IO', 'Fixer.io', 0.0025, 1, 60),
('EXCHANGE_RATES_API', 'ExchangeRatesAPI', 0.0030, 2, 60),
('CURRENCYLAYER', 'CurrencyLayer', 0.0028, 3, 60);

-- Step 2: Rename and normalize currency tables
-- Rename supported_currencies to currencies for consistency
ALTER TABLE supported_currencies RENAME TO currencies;

-- Step 3: Normalize account_currency_balances table
-- First, rename table and fix structure
ALTER TABLE account_currency_balances RENAME TO account_balances;

-- Add foreign key constraint to currencies
ALTER TABLE account_balances
    RENAME COLUMN currency TO currency_code;

ALTER TABLE account_balances
    ADD CONSTRAINT fk_account_balances_currency
    FOREIGN KEY (currency_code) REFERENCES currencies(currency_code);

-- Remove calculated field total_amount_minor (violates 1NF)
ALTER TABLE account_balances DROP COLUMN total_amount_minor;

-- Add unique constraint to prevent duplicate balances per account-currency
ALTER TABLE account_balances
    ADD CONSTRAINT uk_account_balances_account_currency
    UNIQUE (account_id, currency_code);

-- Step 4: Normalize exchange rates table
-- Rename table and update structure
ALTER TABLE fx_rates RENAME TO exchange_rates;

-- Update column names and add relationships
ALTER TABLE exchange_rates
    RENAME COLUMN base_currency TO base_currency_code;

ALTER TABLE exchange_rates
    RENAME COLUMN quote_currency TO quote_currency_code;

ALTER TABLE exchange_rates
    RENAME COLUMN provider TO provider_code;

-- Remove spread column (will be obtained from provider)
ALTER TABLE exchange_rates DROP COLUMN spread;

-- Update provider_code column to match new constraint
ALTER TABLE exchange_rates
    ALTER COLUMN provider_code TYPE VARCHAR(20);

-- Add foreign key constraints
ALTER TABLE exchange_rates
    ADD CONSTRAINT fk_exchange_rates_base_currency
    FOREIGN KEY (base_currency_code) REFERENCES currencies(currency_code);

ALTER TABLE exchange_rates
    ADD CONSTRAINT fk_exchange_rates_quote_currency
    FOREIGN KEY (quote_currency_code) REFERENCES currencies(currency_code);

ALTER TABLE exchange_rates
    ADD CONSTRAINT fk_exchange_rates_provider
    FOREIGN KEY (provider_code) REFERENCES fx_providers(provider_code);

-- Update indexes
DROP INDEX IF EXISTS idx_fx_rates_pair;
DROP INDEX IF EXISTS idx_fx_rates_provider;
DROP INDEX IF EXISTS idx_fx_rates_timestamp;
DROP INDEX IF EXISTS idx_fx_rates_valid_until;

CREATE INDEX idx_exchange_rates_pair ON exchange_rates(base_currency_code, quote_currency_code);
CREATE INDEX idx_exchange_rates_provider ON exchange_rates(provider_code);
CREATE INDEX idx_exchange_rates_timestamp ON exchange_rates(timestamp);
CREATE INDEX idx_exchange_rates_valid_until ON exchange_rates(valid_until);

-- Step 5: Normalize FX conversions table
-- Update column names and relationships
ALTER TABLE fx_conversions
    RENAME COLUMN from_currency TO from_currency_code;

ALTER TABLE fx_conversions
    RENAME COLUMN to_currency TO to_currency_code;

ALTER TABLE fx_conversions
    RENAME COLUMN provider TO provider_code;

-- Remove spread column (will be obtained from provider relationship)
ALTER TABLE fx_conversions DROP COLUMN spread;

-- Update provider_code column type
ALTER TABLE fx_conversions
    ALTER COLUMN provider_code TYPE VARCHAR(20);

-- Add foreign key constraints
ALTER TABLE fx_conversions
    ADD CONSTRAINT fk_fx_conversions_from_currency
    FOREIGN KEY (from_currency_code) REFERENCES currencies(currency_code);

ALTER TABLE fx_conversions
    ADD CONSTRAINT fk_fx_conversions_to_currency
    FOREIGN KEY (to_currency_code) REFERENCES currencies(currency_code);

ALTER TABLE fx_conversions
    ADD CONSTRAINT fk_fx_conversions_provider
    FOREIGN KEY (provider_code) REFERENCES fx_providers(provider_code);

-- Update indexes
DROP INDEX IF EXISTS idx_fx_conversions_pair;
CREATE INDEX idx_fx_conversions_pair ON fx_conversions(from_currency_code, to_currency_code);

-- Step 6: Remove currency redundancy from payments table
-- Currency will be derived from account relationships
ALTER TABLE payments DROP COLUMN currency;

-- Step 7: Remove currency redundancy from ledger_entries table
-- Currency will be derived from account relationships
ALTER TABLE ledger_entries DROP COLUMN currency;

-- Step 8: Add missing foreign key constraints for data integrity
-- Add foreign key from account_balances to accounts (if not exists)
ALTER TABLE account_balances
    ADD CONSTRAINT fk_account_balances_account
    FOREIGN KEY (account_id) REFERENCES accounts(id);

-- Add foreign key from payments to accounts (if not exists)
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_from_account
    FOREIGN KEY (from_account_id) REFERENCES accounts(id);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_to_account
    FOREIGN KEY (to_account_id) REFERENCES accounts(id);

-- Add foreign key from ledger_entries to accounts (if not exists)
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_account
    FOREIGN KEY (account_id) REFERENCES accounts(id);

-- Add foreign key from ledger_entries to payments (if not exists)
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_payment
    FOREIGN KEY (payment_id) REFERENCES payments(id);

-- Add foreign key from fx_conversions to accounts (if not exists)
ALTER TABLE fx_conversions
    ADD CONSTRAINT fk_fx_conversions_account
    FOREIGN KEY (account_id) REFERENCES accounts(id);

COMMIT;
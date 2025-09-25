-- Create individual databases for each microservice
CREATE DATABASE minibank_accounts;
CREATE DATABASE minibank_payments;
CREATE DATABASE minibank_ledger;
CREATE DATABASE minibank_fx;
CREATE DATABASE minibank_compliance;
CREATE DATABASE minibank_notifications;

-- Create schemas within each database
\c minibank_accounts;
CREATE SCHEMA IF NOT EXISTS accounts;

\c minibank_payments;
CREATE SCHEMA IF NOT EXISTS payments;

\c minibank_ledger;
CREATE SCHEMA IF NOT EXISTS ledger;

\c minibank_fx;
CREATE SCHEMA IF NOT EXISTS fx;

\c minibank_compliance;
CREATE SCHEMA IF NOT EXISTS compliance;

\c minibank_notifications;
CREATE SCHEMA IF NOT EXISTS notifications;
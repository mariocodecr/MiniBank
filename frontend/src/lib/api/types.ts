// Base types used across all services
export type Currency = 'USD' | 'EUR' | 'GBP' | 'JPY' | 'CHF' | 'CAD' | 'AUD' | 'CRC';

export type PaymentStatus = 
  | 'REQUESTED' 
  | 'DEBITED' 
  | 'CREDITED' 
  | 'COMPLETED' 
  | 'FAILED'
  | 'COMPENSATED';

export type FXProviderStatus = 'HEALTHY' | 'DEGRADED' | 'UNAVAILABLE';

// Account types
export interface Account {
  id: string;
  userId: string;
  currency: Currency;
  balance: number;
  balanceMinor: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export interface MultiCurrencyAccount {
  id: string;
  userId: string;
  balances: CurrencyBalance[];
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export interface CurrencyBalance {
  currency: Currency;
  availableAmount: number;
  reservedAmount: number;
  totalAmount: number;
  availableAmountMinor: number;
  reservedAmountMinor: number;
  totalAmountMinor: number;
}

export interface CreateAccountRequest {
  userId: string;
  currency: Currency;
}

export interface MoneyTransactionRequest {
  amount: number;
  currency: Currency;
}

// Payment types
export interface Payment {
  id: string;
  requestId: string;
  fromAccountId: string;
  toAccountId: string;
  amountMinor: number;
  currency: Currency;
  status: PaymentStatus;
  createdAt: string;
  updatedAt: string;
  failureReason?: string;
}

export interface CreatePaymentRequest {
  requestId: string;
  fromAccount: string;
  toAccount: string;
  amountMinor: number;
  currency: Currency;
}

// Ledger types
export interface LedgerEntry {
  id: string;
  accountId: string;
  paymentId?: string;
  debitAmount?: number;
  creditAmount?: number;
  currency: Currency;
  description: string;
  timestamp: string;
  balanceAfter: number;
}

export interface BalanceInquiry {
  accountId: string;
  balance: number;
  currency: Currency;
  timestamp: string;
}

// FX types
export interface ExchangeRate {
  id: string;
  baseCurrency: Currency;
  quoteCurrency: Currency;
  midRate: number;
  buyRate: number;
  sellRate: number;
  spread: number;
  provider: string;
  timestamp: string;
  validUntil: string;
  expired: boolean;
}

export interface FXRateLock {
  id: string;
  baseCurrency: Currency;
  quoteCurrency: Currency;
  lockedRate: number;
  expiresAt: string;
  createdAt: string;
}

export interface ProviderStatusResponse {
  providerName: string;
  status: FXProviderStatus;
  priority: number;
  enabled: boolean;
  lastUpdate: string;
}

export interface CreateFXRateLockRequest {
  baseCurrency: Currency;
  quoteCurrency: Currency;
  amountMinor: number;
  durationMinutes?: number;
}

// Dashboard metrics types
export interface PaymentMetrics {
  totalPayments: number;
  successfulPayments: number;
  failedPayments: number;
  successRate: number;
  averageAmount: number;
  totalVolume: number;
  p95Latency: number;
  compensationRate: number;
}

export interface FXMetrics {
  totalConversions: number;
  totalVolumeUSD: number;
  averageSpread: number;
  providersOnline: number;
  rateUpdatesLast24h: number;
}

export interface SystemMetrics {
  uptime: number;
  memoryUsage: number;
  cpuUsage: number;
  activeConnections: number;
  requestsPerSecond: number;
}

// Error types
export interface ApiErrorResponse {
  message: string;
  timestamp: string;
  path: string;
  status: number;
  error: string;
}
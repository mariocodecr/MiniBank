// Export all API clients and types
export * from './types';
export * from './base';
export * from './accounts';
export * from './payments';
export * from './ledger';
export * from './fx';

// Create centralized API instance
import { AccountsClient } from './accounts';
import { PaymentsClient } from './payments';
import { LedgerClient } from './ledger';
import { FXClient } from './fx';

class MBApiClient {
  public accounts: AccountsClient;
  public payments: PaymentsClient;
  public ledger: LedgerClient;
  public fx: FXClient;

  constructor() {
    this.accounts = new AccountsClient();
    this.payments = new PaymentsClient();
    this.ledger = new LedgerClient();
    this.fx = new FXClient();
  }
}

export const apiClient = new MBApiClient();

// Utility functions
export function formatCurrency(amount: number, currency: string, locale: string = 'en-US'): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(amount);
}

export function formatMinorCurrency(amountMinor: number, currency: string, locale: string = 'en-US'): string {
  const amount = amountMinor / 100; // Convert minor units to major units
  return formatCurrency(amount, currency, locale);
}

export function toMinorUnits(amount: number): number {
  return Math.round(amount * 100);
}

export function fromMinorUnits(amountMinor: number): number {
  return amountMinor / 100;
}

export function generateRequestId(): string {
  return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

export function formatRelativeTime(date: Date | string): string {
  const now = new Date();
  const targetDate = typeof date === 'string' ? new Date(date) : date;
  const diffInSeconds = Math.floor((now.getTime() - targetDate.getTime()) / 1000);

  if (diffInSeconds < 60) {
    return 'Just now';
  } else if (diffInSeconds < 3600) {
    const minutes = Math.floor(diffInSeconds / 60);
    return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
  } else if (diffInSeconds < 86400) {
    const hours = Math.floor(diffInSeconds / 3600);
    return `${hours} hour${hours > 1 ? 's' : ''} ago`;
  } else {
    const days = Math.floor(diffInSeconds / 86400);
    return `${days} day${days > 1 ? 's' : ''} ago`;
  }
}
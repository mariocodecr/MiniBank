import { ApiClient, ApiResponse } from './base';
import { LedgerEntry, BalanceInquiry, Currency } from './types';

export class LedgerClient extends ApiClient {
  async getLedgerEntries(
    accountId: string,
    filters?: {
      fromDate?: string;
      toDate?: string;
      currency?: Currency;
      limit?: number;
      offset?: number;
    }
  ): Promise<ApiResponse<LedgerEntry[]>> {
    const params = new URLSearchParams();
    params.append('accountId', accountId);
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }

    return this.get<LedgerEntry[]>(`/ledger/entries?${params.toString()}`);
  }

  async getLedgerEntry(entryId: string): Promise<ApiResponse<LedgerEntry>> {
    return this.get<LedgerEntry>(`/ledger/entries/${entryId}`);
  }

  async getLedgerEntriesForPayment(paymentId: string): Promise<ApiResponse<LedgerEntry[]>> {
    return this.get<LedgerEntry[]>(`/ledger/entries/payment/${paymentId}`);
  }

  async getAccountBalance(accountId: string, currency?: Currency): Promise<ApiResponse<BalanceInquiry>> {
    const params = new URLSearchParams();
    params.append('accountId', accountId);
    
    if (currency) {
      params.append('currency', currency);
    }

    return this.get<BalanceInquiry>(`/ledger/balance?${params.toString()}`);
  }

  async getAccountBalances(accountId: string): Promise<ApiResponse<BalanceInquiry[]>> {
    return this.get<BalanceInquiry[]>(`/ledger/balances/${accountId}`);
  }

  // Transaction audit trail
  async getTransactionHistory(
    accountId: string,
    filters?: {
      fromDate?: string;
      toDate?: string;
      transactionType?: 'DEBIT' | 'CREDIT';
      currency?: Currency;
      minAmount?: number;
      maxAmount?: number;
      limit?: number;
      offset?: number;
    }
  ): Promise<ApiResponse<{
    entries: LedgerEntry[];
    totalCount: number;
    totalDebits: number;
    totalCredits: number;
  }>> {
    const params = new URLSearchParams();
    params.append('accountId', accountId);
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }

    return this.get(`/ledger/history?${params.toString()}`);
  }

  // Balance verification
  async verifyAccountBalance(accountId: string, currency?: Currency): Promise<ApiResponse<{
    accountId: string;
    currency?: Currency;
    calculatedBalance: number;
    accountBalance: number;
    isBalanced: boolean;
    lastVerified: string;
  }>> {
    const params = new URLSearchParams();
    params.append('accountId', accountId);
    
    if (currency) {
      params.append('currency', currency);
    }

    return this.post(`/ledger/verify-balance?${params.toString()}`);
  }

  // Reporting
  async getAccountSummary(
    accountId: string,
    fromDate: string,
    toDate: string
  ): Promise<ApiResponse<{
    accountId: string;
    period: { from: string; to: string };
    currencies: {
      [currency: string]: {
        openingBalance: number;
        totalDebits: number;
        totalCredits: number;
        closingBalance: number;
        transactionCount: number;
      };
    };
  }>> {
    return this.get(`/ledger/summary/${accountId}?from=${fromDate}&to=${toDate}`);
  }
}

export const ledgerClient = new LedgerClient();
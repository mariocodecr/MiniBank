import { ApiClient, ApiResponse } from './base';
import { 
  Account, 
  MultiCurrencyAccount, 
  CreateAccountRequest, 
  MoneyTransactionRequest,
  Currency,
  CurrencyBalance
} from './types';

export class AccountsClient extends ApiClient {
  // Basic account operations
  async createAccount(request: CreateAccountRequest): Promise<ApiResponse<Account>> {
    return this.post<Account>('/accounts', request);
  }

  async getAccount(id: string): Promise<ApiResponse<Account>> {
    return this.get<Account>(`/accounts/${id}`);
  }

  async getAccountsByUserId(userId: string): Promise<ApiResponse<Account[]>> {
    return this.get<Account[]>(`/accounts?userId=${userId}`);
  }

  async reserveFunds(accountId: string, request: MoneyTransactionRequest): Promise<ApiResponse<Account>> {
    return this.post<Account>(`/accounts/${accountId}/reserve`, request);
  }

  async postDebit(accountId: string, request: MoneyTransactionRequest): Promise<ApiResponse<Account>> {
    return this.post<Account>(`/accounts/${accountId}/post?operation=debit`, request);
  }

  async postCredit(accountId: string, request: MoneyTransactionRequest): Promise<ApiResponse<Account>> {
    return this.post<Account>(`/accounts/${accountId}/post?operation=credit`, request);
  }

  // Multi-currency account operations
  async createMultiCurrencyAccount(userId: string): Promise<ApiResponse<MultiCurrencyAccount>> {
    return this.post<MultiCurrencyAccount>('/accounts/multi-currency', { userId });
  }

  async getMultiCurrencyAccount(id: string): Promise<ApiResponse<MultiCurrencyAccount>> {
    return this.get<MultiCurrencyAccount>(`/accounts/multi-currency/${id}`);
  }

  async enableCurrency(accountId: string, currency: Currency): Promise<ApiResponse<MultiCurrencyAccount>> {
    return this.post<MultiCurrencyAccount>(`/accounts/multi-currency/${accountId}/currencies`, { currency });
  }

  async getCurrencyBalances(accountId: string): Promise<ApiResponse<CurrencyBalance[]>> {
    return this.get<CurrencyBalance[]>(`/accounts/multi-currency/${accountId}/balances`);
  }

  async updateCurrencyBalance(
    accountId: string, 
    currency: Currency, 
    request: MoneyTransactionRequest
  ): Promise<ApiResponse<CurrencyBalance>> {
    return this.put<CurrencyBalance>(`/accounts/multi-currency/${accountId}/balances/${currency}`, request);
  }

  async getSupportedCurrencies(): Promise<ApiResponse<Currency[]>> {
    return this.get<Currency[]>('/accounts/currencies');
  }

  // Balance operations for multi-currency accounts
  async creditBalance(
    accountId: string, 
    currency: Currency, 
    request: MoneyTransactionRequest
  ): Promise<ApiResponse<CurrencyBalance>> {
    return this.post<CurrencyBalance>(`/accounts/multi-currency/${accountId}/balances/${currency}/credit`, request);
  }

  async debitBalance(
    accountId: string, 
    currency: Currency, 
    request: MoneyTransactionRequest
  ): Promise<ApiResponse<CurrencyBalance>> {
    return this.post<CurrencyBalance>(`/accounts/multi-currency/${accountId}/balances/${currency}/debit`, request);
  }

  async reserveBalance(
    accountId: string, 
    currency: Currency, 
    request: MoneyTransactionRequest
  ): Promise<ApiResponse<CurrencyBalance>> {
    return this.post<CurrencyBalance>(`/accounts/multi-currency/${accountId}/balances/${currency}/reserve`, request);
  }

  async releaseReservedBalance(
    accountId: string, 
    currency: Currency, 
    request: MoneyTransactionRequest
  ): Promise<ApiResponse<CurrencyBalance>> {
    return this.post<CurrencyBalance>(`/accounts/multi-currency/${accountId}/balances/${currency}/release`, request);
  }
}

export const accountsClient = new AccountsClient();
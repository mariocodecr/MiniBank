import { ApiClient, ApiResponse } from './base';
import { 
  ExchangeRate, 
  FXRateLock, 
  ProviderStatusResponse, 
  CreateFXRateLockRequest,
  Currency,
  FXMetrics
} from './types';

export class FXClient extends ApiClient {
  // Exchange rate operations
  async getExchangeRate(baseCurrency: Currency, quoteCurrency: Currency): Promise<ApiResponse<ExchangeRate>> {
    return this.get<ExchangeRate>(`/api/fx/rates/${baseCurrency}/${quoteCurrency}`);
  }

  async getAllActiveRates(): Promise<ApiResponse<ExchangeRate[]>> {
    return this.get<ExchangeRate[]>('/api/fx/rates');
  }

  async refreshRate(baseCurrency: Currency, quoteCurrency: Currency): Promise<ApiResponse<ExchangeRate>> {
    return this.post<ExchangeRate>(`/api/fx/rates/${baseCurrency}/${quoteCurrency}/refresh`);
  }

  async refreshAllRates(): Promise<ApiResponse<void>> {
    return this.post<void>('/api/fx/rates/refresh');
  }

  // FX Rate Lock operations
  async createRateLock(request: CreateFXRateLockRequest): Promise<ApiResponse<FXRateLock>> {
    return this.post<FXRateLock>('/api/fx/rates/lock', request);
  }

  async getRateLock(lockId: string): Promise<ApiResponse<FXRateLock>> {
    return this.get<FXRateLock>(`/api/fx/rates/locks/${lockId}`);
  }

  async getRateLocks(filters?: {
    baseCurrency?: Currency;
    quoteCurrency?: Currency;
    active?: boolean;
  }): Promise<ApiResponse<FXRateLock[]>> {
    const params = new URLSearchParams();
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }

    const queryString = params.toString();
    const endpoint = queryString ? `/api/fx/rates/locks?${queryString}` : '/api/fx/rates/locks';
    
    return this.get<FXRateLock[]>(endpoint);
  }

  async extendRateLock(lockId: string, additionalMinutes: number): Promise<ApiResponse<FXRateLock>> {
    return this.post<FXRateLock>(`/api/fx/rates/locks/${lockId}/extend`, { additionalMinutes });
  }

  async releaseRateLock(lockId: string): Promise<ApiResponse<void>> {
    return this.post<void>(`/api/fx/rates/locks/${lockId}/release`);
  }

  // Provider operations
  async getProviders(): Promise<ApiResponse<ProviderStatusResponse[]>> {
    return this.get<ProviderStatusResponse[]>('/api/fx/providers');
  }

  async getProviderStatus(providerName: string): Promise<ApiResponse<ProviderStatusResponse>> {
    return this.get<ProviderStatusResponse>(`/api/fx/providers/${providerName}/status`);
  }

  // Spread configuration
  async getSpreads(): Promise<ApiResponse<{
    [currencyPair: string]: {
      retail: number;
      smallBusiness: number;
      corporate: number;
      institutional: number;
    };
  }>> {
    return this.get('/api/fx/spreads');
  }

  async updateSpreads(spreads: {
    [currencyPair: string]: {
      retail: number;
      smallBusiness: number;
      corporate: number;
      institutional: number;
    };
  }): Promise<ApiResponse<void>> {
    return this.put('/api/fx/spreads', spreads);
  }

  // FX conversion operations
  async getQuote(
    baseCurrency: Currency, 
    quoteCurrency: Currency, 
    amountMinor: number
  ): Promise<ApiResponse<{
    fromCurrency: Currency;
    toCurrency: Currency;
    fromAmountMinor: number;
    toAmountMinor: number;
    rate: number;
    spread: number;
    provider: string;
    validUntil: string;
    quoteId: string;
  }>> {
    return this.post('/api/fx/quote', {
      baseCurrency,
      quoteCurrency,
      amountMinor,
    });
  }

  async convert(
    quoteId: string,
    fromAccountId: string,
    toAccountId: string
  ): Promise<ApiResponse<{
    conversionId: string;
    status: 'PENDING' | 'COMPLETED' | 'FAILED';
    fromAccountId: string;
    toAccountId: string;
    executedRate: number;
    timestamp: string;
  }>> {
    return this.post('/api/fx/convert', {
      quoteId,
      fromAccountId,
      toAccountId,
    });
  }

  // Metrics
  async getFXMetrics(timeframe?: '1h' | '24h' | '7d' | '30d'): Promise<ApiResponse<FXMetrics>> {
    const endpoint = timeframe ? `/api/fx/metrics?timeframe=${timeframe}` : '/api/fx/metrics';
    return this.get<FXMetrics>(endpoint);
  }
}

export const fxClient = new FXClient();
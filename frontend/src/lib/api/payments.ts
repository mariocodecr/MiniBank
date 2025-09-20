import { ApiClient, ApiResponse } from './base';
import { Payment, CreatePaymentRequest, PaymentMetrics } from './types';

export class PaymentsClient extends ApiClient {
  async createPayment(request: CreatePaymentRequest): Promise<ApiResponse<Payment>> {
    return this.post<Payment>('/payments', request);
  }

  async getPayment(paymentId: string): Promise<ApiResponse<Payment>> {
    return this.get<Payment>(`/payments/${paymentId}`);
  }

  async getPayments(filters?: {
    userId?: string;
    status?: string;
    fromDate?: string;
    toDate?: string;
    currency?: string;
    limit?: number;
    offset?: number;
  }): Promise<ApiResponse<Payment[]>> {
    const params = new URLSearchParams();
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined) {
          params.append(key, value.toString());
        }
      });
    }

    const queryString = params.toString();
    const endpoint = queryString ? `/payments?${queryString}` : '/payments';
    
    return this.get<Payment[]>(endpoint);
  }

  async getPaymentMetrics(timeframe?: '1h' | '24h' | '7d' | '30d'): Promise<ApiResponse<PaymentMetrics>> {
    const endpoint = timeframe ? `/payments/metrics?timeframe=${timeframe}` : '/payments/metrics';
    return this.get<PaymentMetrics>(endpoint);
  }

  async cancelPayment(paymentId: string): Promise<ApiResponse<Payment>> {
    return this.post<Payment>(`/payments/${paymentId}/cancel`);
  }

  async retryPayment(paymentId: string): Promise<ApiResponse<Payment>> {
    return this.post<Payment>(`/payments/${paymentId}/retry`);
  }

  // Cross-currency payment operations
  async createCrossCurrencyPayment(request: {
    requestId: string;
    fromAccount: string;
    toAccount: string;
    fromAmountMinor: number;
    fromCurrency: string;
    toCurrency: string;
    fxRateLockId?: string;
  }): Promise<ApiResponse<Payment>> {
    return this.post<Payment>('/payments/cross-currency', request);
  }

  async getFXDetails(paymentId: string): Promise<ApiResponse<{
    fxRateLockId?: string;
    lockedRate?: number;
    fromAmountMinor: number;
    toAmountMinor: number;
    fromCurrency: string;
    toCurrency: string;
    spread: number;
    provider: string;
  }>> {
    return this.get(`/payments/${paymentId}/fx-details`);
  }
}

export const paymentsClient = new PaymentsClient();
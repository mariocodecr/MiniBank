import { useQuery } from '@tanstack/react-query';
import { paymentsClient } from '@/lib/api';
import type { PaymentMetrics } from '@/lib/api/types';

export function usePaymentMetrics(timeframe: '1h' | '24h' | '7d' | '30d' = '24h') {
  return useQuery({
    queryKey: ['payment-metrics', timeframe],
    queryFn: async () => {
      const response = await paymentsClient.getPaymentMetrics(timeframe);
      return response.data;
    },
    // Mock data for development
    initialData: {
      totalPayments: 1547,
      successfulPayments: 1502,
      failedPayments: 45,
      successRate: 97.1,
      averageAmount: 1250.75,
      totalVolume: 1935435.25,
      p95Latency: 2.3,
      compensationRate: 1.2,
    } as PaymentMetrics,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}
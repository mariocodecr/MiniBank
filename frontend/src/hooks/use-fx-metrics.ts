import { useQuery } from '@tanstack/react-query';
import { fxClient } from '@/lib/api';
import type { FXMetrics } from '@/lib/api/types';

export function useFXMetrics(timeframe: '1h' | '24h' | '7d' | '30d' = '24h') {
  return useQuery({
    queryKey: ['fx-metrics', timeframe],
    queryFn: async () => {
      const response = await fxClient.getFXMetrics(timeframe);
      return response.data;
    },
    // Mock data for development
    initialData: {
      totalConversions: 342,
      totalVolumeUSD: 2847392.50,
      averageSpread: 0.0025,
      providersOnline: 3,
      rateUpdatesLast24h: 1440,
    } as FXMetrics,
    refetchInterval: 60000, // Refetch every minute
  });
}
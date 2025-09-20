'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ArrowLeftRight, Clock, TrendingUp, TrendingDown, RefreshCw } from 'lucide-react';
import { fxClient, formatCurrency } from '@/lib/api';
import type { ExchangeRate, ProviderStatusResponse, Currency } from '@/lib/api/types';

function ExchangeRateCard({ rate }: { rate: ExchangeRate }) {
  const isExpired = new Date(rate.validUntil) < new Date();
  
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">
          {rate.baseCurrency}/{rate.quoteCurrency}
        </CardTitle>
        <Badge variant={isExpired ? 'destructive' : 'success'}>
          {isExpired ? 'Expired' : 'Active'}
        </Badge>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{rate.midRate.toFixed(6)}</div>
        <div className="flex justify-between text-sm mt-2">
          <div>
            <p className="text-muted-foreground">Buy</p>
            <p className="font-medium text-green-600">{rate.buyRate.toFixed(6)}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Sell</p>
            <p className="font-medium text-red-600">{rate.sellRate.toFixed(6)}</p>
          </div>
        </div>
        <div className="flex justify-between text-xs mt-2 pt-2 border-t">
          <span className="text-muted-foreground">Spread: {(rate.spread * 100).toFixed(3)}%</span>
          <span className="text-muted-foreground">Via {rate.provider}</span>
        </div>
      </CardContent>
    </Card>
  );
}

function ProviderStatusCard({ provider }: { provider: ProviderStatusResponse }) {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'HEALTHY': return 'success';
      case 'DEGRADED': return 'warning';
      case 'UNAVAILABLE': return 'destructive';
      default: return 'secondary';
    }
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{provider.providerName}</CardTitle>
        <Badge variant={getStatusColor(provider.status)}>
          {provider.status}
        </Badge>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Priority:</span>
            <span className="font-medium">#{provider.priority}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Enabled:</span>
            <span className={provider.enabled ? 'text-green-600' : 'text-red-600'}>
              {provider.enabled ? 'Yes' : 'No'}
            </span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Last Update:</span>
            <span className="text-xs">{new Date(provider.lastUpdate).toLocaleTimeString()}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function FXPage() {
  const [fromCurrency, setFromCurrency] = useState<Currency>('USD');
  const [toCurrency, setToCurrency] = useState<Currency>('EUR');
  const [amount, setAmount] = useState<number>(1000);

  const { data: rates, isLoading: ratesLoading, refetch: refetchRates } = useQuery({
    queryKey: ['fx-rates'],
    queryFn: async () => {
      const response = await fxClient.getAllActiveRates();
      return response.data;
    },
    // Mock data for development
    initialData: [
      {
        id: 'rate_1',
        baseCurrency: 'USD',
        quoteCurrency: 'EUR',
        midRate: 0.8542,
        buyRate: 0.8530,
        sellRate: 0.8554,
        spread: 0.0024,
        provider: 'XE.com',
        timestamp: new Date().toISOString(),
        validUntil: new Date(Date.now() + 3600000).toISOString(),
        expired: false,
      },
      {
        id: 'rate_2',
        baseCurrency: 'GBP',
        quoteCurrency: 'USD',
        midRate: 1.2758,
        buyRate: 1.2745,
        sellRate: 1.2771,
        spread: 0.0026,
        provider: 'CurrencyLayer',
        timestamp: new Date().toISOString(),
        validUntil: new Date(Date.now() + 3600000).toISOString(),
        expired: false,
      },
      {
        id: 'rate_3',
        baseCurrency: 'USD',
        quoteCurrency: 'JPY',
        midRate: 150.23,
        buyRate: 150.08,
        sellRate: 150.38,
        spread: 0.0030,
        provider: 'XE.com',
        timestamp: new Date().toISOString(),
        validUntil: new Date(Date.now() + 3600000).toISOString(),
        expired: false,
      },
    ] as ExchangeRate[],
    refetchInterval: 60000, // Refetch every minute
  });

  const { data: providers, isLoading: providersLoading } = useQuery({
    queryKey: ['fx-providers'],
    queryFn: async () => {
      const response = await fxClient.getProviders();
      return response.data;
    },
    // Mock data for development
    initialData: [
      {
        providerName: 'XE.com',
        status: 'HEALTHY',
        priority: 1,
        enabled: true,
        lastUpdate: new Date().toISOString(),
      },
      {
        providerName: 'CurrencyLayer',
        status: 'HEALTHY',
        priority: 2,
        enabled: true,
        lastUpdate: new Date().toISOString(),
      },
      {
        providerName: 'External API',
        status: 'DEGRADED',
        priority: 3,
        enabled: true,
        lastUpdate: new Date(Date.now() - 300000).toISOString(),
      },
    ] as ProviderStatusResponse[],
    refetchInterval: 30000, // Refetch every 30 seconds
  });

  const currentRate = rates?.find(
    rate => rate.baseCurrency === fromCurrency && rate.quoteCurrency === toCurrency
  );

  const convertedAmount = currentRate ? amount * currentRate.midRate : 0;

  const currencies: Currency[] = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD', 'CRC'];

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Foreign Exchange</h1>
            <p className="text-muted-foreground">
              Real-time exchange rates and currency conversion
            </p>
          </div>
          <Button onClick={() => refetchRates()} variant="outline">
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh Rates
          </Button>
        </div>

        {/* Currency Converter */}
        <Card>
          <CardHeader>
            <CardTitle>Currency Converter</CardTitle>
            <CardDescription>
              Get real-time exchange rates and convert currencies
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-5 items-end">
              <div>
                <label className="text-sm font-medium">Amount</label>
                <Input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
                  placeholder="Enter amount"
                />
              </div>
              
              <div>
                <label className="text-sm font-medium">From</label>
                <Select value={fromCurrency} onValueChange={(value) => setFromCurrency(value as Currency)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {currencies.map((currency) => (
                      <SelectItem key={currency} value={currency}>
                        {currency}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="flex justify-center">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => {
                    const temp = fromCurrency;
                    setFromCurrency(toCurrency);
                    setToCurrency(temp);
                  }}
                >
                  <ArrowLeftRight className="h-4 w-4" />
                </Button>
              </div>

              <div>
                <label className="text-sm font-medium">To</label>
                <Select value={toCurrency} onValueChange={(value) => setToCurrency(value as Currency)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {currencies.map((currency) => (
                      <SelectItem key={currency} value={currency}>
                        {currency}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <Button className="h-10">
                <Clock className="h-4 w-4 mr-2" />
                Lock Rate
              </Button>
            </div>

            {currentRate && (
              <div className="mt-6 p-4 bg-blue-50 rounded-lg">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">
                      {formatCurrency(amount, fromCurrency)} =
                    </p>
                    <p className="text-2xl font-bold text-blue-600">
                      {formatCurrency(convertedAmount, toCurrency)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-muted-foreground">Exchange Rate</p>
                    <p className="text-lg font-semibold">
                      1 {fromCurrency} = {currentRate.midRate.toFixed(6)} {toCurrency}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Exchange Rates */}
        <div>
          <h2 className="text-xl font-semibold mb-4">Current Exchange Rates</h2>
          {ratesLoading ? (
            <div className="grid gap-4 md:grid-cols-3">
              {[1, 2, 3].map((i) => (
                <Card key={i}>
                  <CardContent className="p-6">
                    <div className="animate-pulse space-y-2">
                      <div className="h-4 bg-gray-200 rounded w-20"></div>
                      <div className="h-8 bg-gray-200 rounded w-32"></div>
                      <div className="h-3 bg-gray-200 rounded w-24"></div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-3">
              {rates?.map((rate) => (
                <ExchangeRateCard key={rate.id} rate={rate} />
              ))}
            </div>
          )}
        </div>

        {/* Provider Status */}
        <div>
          <h2 className="text-xl font-semibold mb-4">Provider Status</h2>
          {providersLoading ? (
            <div className="grid gap-4 md:grid-cols-3">
              {[1, 2, 3].map((i) => (
                <Card key={i}>
                  <CardContent className="p-6">
                    <div className="animate-pulse space-y-2">
                      <div className="h-4 bg-gray-200 rounded w-24"></div>
                      <div className="h-6 bg-gray-200 rounded w-16"></div>
                      <div className="h-3 bg-gray-200 rounded w-32"></div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-3">
              {providers?.map((provider) => (
                <ProviderStatusCard key={provider.providerName} provider={provider} />
              ))}
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  );
}
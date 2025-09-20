'use client';

import { MainLayout } from '@/components/layout/main-layout';
import { MetricCard } from '@/components/dashboard/metric-card';
import { PaymentVolumeChart, SuccessRateChart } from '@/components/dashboard/payment-chart';
import { usePaymentMetrics } from '@/hooks/use-payment-metrics';
import { useFXMetrics } from '@/hooks/use-fx-metrics';
import { 
  CreditCard, 
  TrendingUp, 
  Clock, 
  AlertTriangle,
  ArrowLeftRight,
  Globe,
  Activity,
  Users
} from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

export default function DashboardPage() {
  const { data: paymentMetrics, isLoading: paymentsLoading } = usePaymentMetrics();
  const { data: fxMetrics, isLoading: fxLoading } = useFXMetrics();

  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            Welcome to your banking operations overview
          </p>
        </div>

        {/* Payment Metrics */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title="Success Rate"
            value={`${paymentMetrics?.successRate.toFixed(1)}%`}
            description="Payment success rate"
            trend={{ value: 2.1, label: 'from last period' }}
            status={paymentMetrics?.successRate > 95 ? 'success' : 'warning'}
            icon={TrendingUp}
          />
          
          <MetricCard
            title="P95 Latency"
            value={`${paymentMetrics?.p95Latency}s`}
            description="95th percentile response time"
            trend={{ value: -0.5, label: 'improvement' }}
            status={paymentMetrics?.p95Latency < 3 ? 'success' : 'warning'}
            icon={Clock}
          />
          
          <MetricCard
            title="Compensation Rate"
            value={`${paymentMetrics?.compensationRate}%`}
            description="Failed payments requiring compensation"
            trend={{ value: -0.3, label: 'reduction' }}
            status={paymentMetrics?.compensationRate < 2 ? 'success' : 'warning'}
            icon={AlertTriangle}
          />
          
          <MetricCard
            title="Total Volume"
            value={formatCurrency(paymentMetrics?.totalVolume || 0, 'USD')}
            description="Payment volume today"
            trend={{ value: 12.5, label: 'from yesterday' }}
            status="success"
            icon={CreditCard}
          />
        </div>

        {/* FX Metrics */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title="FX Conversions"
            value={fxMetrics?.totalConversions.toLocaleString()}
            description="Total FX conversions today"
            trend={{ value: 8.2, label: 'from yesterday' }}
            status="success"
            icon={ArrowLeftRight}
          />
          
          <MetricCard
            title="FX Volume"
            value={formatCurrency(fxMetrics?.totalVolumeUSD || 0, 'USD')}
            description="Total FX volume in USD"
            trend={{ value: 15.7, label: 'from yesterday' }}
            status="success"
            icon={Globe}
          />
          
          <MetricCard
            title="Average Spread"
            value={`${(fxMetrics?.averageSpread * 100 || 0).toFixed(3)}%`}
            description="Average FX spread applied"
            trend={{ value: -0.1, label: 'improvement' }}
            status="success"
            icon={Activity}
          />
          
          <MetricCard
            title="Providers Online"
            value={`${fxMetrics?.providersOnline}/3`}
            description="FX rate providers available"
            status={fxMetrics?.providersOnline === 3 ? 'success' : 'warning'}
            icon={Users}
          />
        </div>

        {/* Charts */}
        <div className="grid gap-4 md:grid-cols-2">
          <PaymentVolumeChart />
          <SuccessRateChart />
        </div>

        {/* Recent Activity */}
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-4">
            <h3 className="text-lg font-medium">Recent Payments</h3>
            <div className="space-y-2">
              {/* Mock recent payments */}
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">Payment #12345</p>
                  <p className="text-xs text-muted-foreground">2 minutes ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">$1,250.00</p>
                  <p className="text-xs text-success">Completed</p>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">Payment #12344</p>
                  <p className="text-xs text-muted-foreground">5 minutes ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">$750.50</p>
                  <p className="text-xs text-success">Completed</p>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">Payment #12343</p>
                  <p className="text-xs text-muted-foreground">12 minutes ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">$2,100.00</p>
                  <p className="text-xs text-warning">Processing</p>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="text-lg font-medium">FX Rate Updates</h3>
            <div className="space-y-2">
              {/* Mock FX rate updates */}
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">USD/EUR</p>
                  <p className="text-xs text-muted-foreground">1 minute ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">0.8542</p>
                  <p className="text-xs text-success">+0.0023</p>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">GBP/USD</p>
                  <p className="text-xs text-muted-foreground">3 minutes ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">1.2758</p>
                  <p className="text-xs text-destructive">-0.0012</p>
                </div>
              </div>
              
              <div className="flex items-center justify-between p-3 bg-white rounded-lg border">
                <div>
                  <p className="text-sm font-medium">USD/JPY</p>
                  <p className="text-xs text-muted-foreground">7 minutes ago</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-medium">150.23</p>
                  <p className="text-xs text-success">+0.15</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}
'use client';

import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  Plus,
  Search,
  Filter,
  ArrowUpRight,
  ArrowDownLeft,
  Clock,
  CheckCircle,
  XCircle,
  MoreHorizontal,
  Calendar
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { formatCurrency } from '@/lib/api';

// Mock payment data
const mockPayments = [
  {
    id: 'pay_001',
    type: 'OUTBOUND',
    status: 'COMPLETED',
    amount: 1250.00,
    currency: 'USD',
    fromAccount: 'acc_multi_1',
    toAccount: 'acc_ext_001',
    description: 'Vendor payment - Office supplies',
    reference: 'INV-2024-001',
    createdAt: '2024-01-20T14:30:00Z',
    completedAt: '2024-01-20T14:32:15Z',
    beneficiaryName: 'Office Supplies Ltd',
  },
  {
    id: 'pay_002',
    type: 'INBOUND',
    status: 'PENDING',
    amount: 3500.00,
    currency: 'EUR',
    fromAccount: 'acc_ext_002',
    toAccount: 'acc_multi_1',
    description: 'Client payment - Project milestone',
    reference: 'PROJ-MS-001',
    createdAt: '2024-01-20T10:15:00Z',
    beneficiaryName: 'Tech Solutions Inc',
  },
  {
    id: 'pay_003',
    type: 'OUTBOUND',
    status: 'FAILED',
    amount: 750.50,
    currency: 'GBP',
    fromAccount: 'acc_multi_1',
    toAccount: 'acc_ext_003',
    description: 'Salary payment',
    reference: 'SAL-JAN-2024',
    createdAt: '2024-01-19T16:45:00Z',
    failedAt: '2024-01-19T16:47:30Z',
    beneficiaryName: 'John Smith',
    failureReason: 'Insufficient funds',
  },
  {
    id: 'pay_004',
    type: 'FX_EXCHANGE',
    status: 'COMPLETED',
    amount: 2000.00,
    currency: 'USD',
    exchangeAmount: 1850.75,
    exchangeCurrency: 'EUR',
    description: 'Currency exchange USD → EUR',
    reference: 'FX-20240120-001',
    createdAt: '2024-01-20T09:20:00Z',
    completedAt: '2024-01-20T09:20:45Z',
    exchangeRate: 0.925375,
  },
];

interface Payment {
  id: string;
  type: 'INBOUND' | 'OUTBOUND' | 'FX_EXCHANGE';
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  amount: number;
  currency: string;
  fromAccount?: string;
  toAccount?: string;
  description: string;
  reference: string;
  createdAt: string;
  completedAt?: string;
  failedAt?: string;
  beneficiaryName?: string;
  failureReason?: string;
  exchangeAmount?: number;
  exchangeCurrency?: string;
  exchangeRate?: number;
}

function PaymentStatusBadge({ status }: { status: Payment['status'] }) {
  const variants = {
    PENDING: {
      variant: 'secondary' as const,
      icon: Clock,
      className: 'bg-yellow-100 text-yellow-800 border-yellow-200 hover:bg-yellow-200',
      label: 'Pending'
    },
    COMPLETED: {
      variant: 'secondary' as const,
      icon: CheckCircle,
      className: 'bg-green-100 text-green-800 border-green-200 hover:bg-green-200',
      label: 'Completed'
    },
    FAILED: {
      variant: 'secondary' as const,
      icon: XCircle,
      className: 'bg-red-100 text-red-800 border-red-200 hover:bg-red-200',
      label: 'Failed'
    },
    CANCELLED: {
      variant: 'secondary' as const,
      icon: XCircle,
      className: 'bg-gray-100 text-gray-800 border-gray-200 hover:bg-gray-200',
      label: 'Cancelled'
    },
  };

  const config = variants[status];
  const Icon = config.icon;

  return (
    <Badge variant={config.variant} className={`flex items-center gap-1 ${config.className}`}>
      <Icon className="h-3 w-3" />
      {config.label}
    </Badge>
  );
}

function PaymentTypeIcon({ type }: { type: Payment['type'] }) {
  if (type === 'INBOUND') {
    return <ArrowDownLeft className="h-4 w-4 text-green-600" />;
  }
  if (type === 'OUTBOUND') {
    return <ArrowUpRight className="h-4 w-4 text-red-600" />;
  }
  return <div className="h-4 w-4 bg-blue-600 rounded-full flex items-center justify-center">
    <span className="text-xs text-white font-bold">FX</span>
  </div>;
}

function PaymentCard({ payment }: { payment: Payment }) {
  const router = useRouter();

  const handleViewDetails = () => {
    router.push(`/payments/${payment.id}`);
  };

  return (
    <Card className="hover:shadow-md transition-shadow cursor-pointer" onClick={handleViewDetails}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <PaymentTypeIcon type={payment.type} />
            <div className="flex-1">
              <div className="flex items-center justify-between gap-4">
                <h3 className="font-semibold truncate max-w-[250px]">
                  {payment.beneficiaryName || payment.description}
                </h3>
                <PaymentStatusBadge status={payment.status} />
              </div>
              <p className="text-sm text-muted-foreground mt-1">
                {payment.type === 'FX_EXCHANGE' ? (
                  `${formatCurrency(payment.amount, payment.currency)} → ${formatCurrency(payment.exchangeAmount!, payment.exchangeCurrency!)}`
                ) : (
                  payment.description
                )}
              </p>
              <div className="flex items-center space-x-4 mt-2 text-xs text-muted-foreground">
                <span>Ref: {payment.reference}</span>
                <span>•</span>
                <span>{new Date(payment.createdAt).toLocaleDateString()}</span>
                {payment.failureReason && (
                  <>
                    <span>•</span>
                    <span className="text-red-600">{payment.failureReason}</span>
                  </>
                )}
              </div>
            </div>
          </div>
          <div className="text-right">
            <div className="text-lg font-semibold">
              {payment.type === 'FX_EXCHANGE' ? (
                <div className="space-y-1">
                  <div className="text-red-600">-{formatCurrency(payment.amount, payment.currency)}</div>
                  <div className="text-green-600">+{formatCurrency(payment.exchangeAmount!, payment.exchangeCurrency!)}</div>
                </div>
              ) : (
                <span className={payment.type === 'INBOUND' ? 'text-green-600' : 'text-red-600'}>
                  {payment.type === 'INBOUND' ? '+' : '-'}{formatCurrency(payment.amount, payment.currency)}
                </span>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function PaymentsPage() {
  const router = useRouter();

  const { data: payments, isLoading } = useQuery({
    queryKey: ['payments'],
    queryFn: async () => {
      // Mock API call
      await new Promise(resolve => setTimeout(resolve, 500));
      return mockPayments;
    },
  });

  const handleCreatePayment = () => {
    router.push('/payments/new');
  };

  const stats = payments ? {
    total: payments.length,
    pending: payments.filter(p => p.status === 'PENDING').length,
    completed: payments.filter(p => p.status === 'COMPLETED').length,
    failed: payments.filter(p => p.status === 'FAILED').length,
    totalVolume: payments
      .filter(p => p.status === 'COMPLETED')
      .reduce((sum, p) => sum + p.amount, 0),
  } : null;

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Payments</h1>
            <p className="text-muted-foreground">
              Manage and track all your payment transactions
            </p>
          </div>
          <div className="flex space-x-2">
            <Button variant="outline">
              <Filter className="h-4 w-4 mr-2" />
              Filter
            </Button>
            <Button onClick={handleCreatePayment}>
              <Plus className="h-4 w-4 mr-2" />
              New Payment
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Payments</CardTitle>
                <MoreHorizontal className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.total}</div>
                <p className="text-xs text-muted-foreground">All time</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Pending</CardTitle>
                <Clock className="h-4 w-4 text-yellow-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.pending}</div>
                <p className="text-xs text-muted-foreground">Awaiting processing</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Completed</CardTitle>
                <CheckCircle className="h-4 w-4 text-green-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.completed}</div>
                <p className="text-xs text-muted-foreground">Successfully processed</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Volume</CardTitle>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatCurrency(stats.totalVolume, 'USD')}</div>
                <p className="text-xs text-muted-foreground">Completed payments</p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Filters */}
        <Card>
          <CardContent className="p-4">
            <div className="flex space-x-4">
              <div className="flex-1">
                <Input
                  placeholder="Search payments by reference, description, or beneficiary..."
                  className="max-w-md"
                />
              </div>
              <Select defaultValue="all">
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Status</SelectItem>
                  <SelectItem value="pending">Pending</SelectItem>
                  <SelectItem value="completed">Completed</SelectItem>
                  <SelectItem value="failed">Failed</SelectItem>
                </SelectContent>
              </Select>
              <Select defaultValue="all">
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="Type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  <SelectItem value="inbound">Inbound</SelectItem>
                  <SelectItem value="outbound">Outbound</SelectItem>
                  <SelectItem value="fx">FX Exchange</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Payments List */}
        {isLoading ? (
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : (
          <div className="space-y-4">
            {payments?.map((payment) => (
              <PaymentCard key={payment.id} payment={payment} />
            ))}
          </div>
        )}

        {payments && payments.length === 0 && (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <div className="text-center">
                <h3 className="text-lg font-semibold mb-2">No payments found</h3>
                <p className="text-muted-foreground mb-4">
                  Get started by creating your first payment
                </p>
                <Button onClick={handleCreatePayment}>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Payment
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </MainLayout>
  );
}
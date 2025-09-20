'use client';

import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  ArrowLeft,
  ArrowUpRight,
  ArrowDownLeft,
  Calendar,
  Filter,
  Download,
  TrendingUp,
  TrendingDown
} from 'lucide-react';
import { formatCurrency } from '@/lib/api';

// Mock transaction data
const mockTransactions = [
  {
    id: 'txn_001',
    date: '2024-01-20T14:30:00Z',
    type: 'OUTBOUND_PAYMENT',
    description: 'Payment to Office Supplies Ltd',
    amount: -1250.00,
    currency: 'USD',
    balance: 3750.00,
    reference: 'PAY-001',
    status: 'COMPLETED'
  },
  {
    id: 'txn_002',
    date: '2024-01-20T10:15:00Z',
    type: 'INBOUND_PAYMENT',
    description: 'Payment from Tech Solutions Inc',
    amount: 3500.00,
    currency: 'EUR',
    balance: 3500.50,
    reference: 'PAY-002',
    status: 'COMPLETED'
  },
  {
    id: 'txn_003',
    date: '2024-01-19T16:45:00Z',
    type: 'OUTBOUND_PAYMENT',
    description: 'Salary payment to John Smith',
    amount: -750.50,
    currency: 'GBP',
    balance: 2100.75,
    reference: 'PAY-003',
    status: 'FAILED'
  },
  {
    id: 'txn_004',
    date: '2024-01-19T09:20:00Z',
    type: 'FX_EXCHANGE',
    description: 'Currency exchange USD → EUR',
    amount: -2000.00,
    currency: 'USD',
    balance: 5000.00,
    reference: 'FX-001',
    status: 'COMPLETED'
  },
  {
    id: 'txn_005',
    date: '2024-01-19T09:20:00Z',
    type: 'FX_EXCHANGE',
    description: 'Currency exchange USD → EUR',
    amount: 1850.75,
    currency: 'EUR',
    balance: 1850.75,
    reference: 'FX-001',
    status: 'COMPLETED'
  },
  {
    id: 'txn_006',
    date: '2024-01-18T11:30:00Z',
    type: 'DEPOSIT',
    description: 'Initial account funding',
    amount: 7000.00,
    currency: 'USD',
    balance: 7000.00,
    reference: 'DEP-001',
    status: 'COMPLETED'
  },
];

interface Transaction {
  id: string;
  date: string;
  type: string;
  description: string;
  amount: number;
  currency: string;
  balance: number;
  reference: string;
  status: string;
}

function TransactionIcon({ type }: { type: string }) {
  if (type === 'INBOUND_PAYMENT' || type === 'DEPOSIT') {
    return <ArrowDownLeft className="h-4 w-4 text-green-600" />;
  }
  if (type === 'OUTBOUND_PAYMENT') {
    return <ArrowUpRight className="h-4 w-4 text-red-600" />;
  }
  return <div className="h-4 w-4 bg-blue-600 rounded-full flex items-center justify-center">
    <span className="text-xs text-white font-bold">FX</span>
  </div>;
}

function TransactionStatusBadge({ status }: { status: string }) {
  const config = {
    COMPLETED: { label: 'Completed', className: 'bg-green-100 text-green-800' },
    FAILED: { label: 'Failed', className: 'bg-red-100 text-red-800' },
    PENDING: { label: 'Pending', className: 'bg-yellow-100 text-yellow-800' },
  }[status] || { label: status, className: 'bg-gray-100 text-gray-800' };

  return (
    <Badge variant="secondary" className={config.className}>
      {config.label}
    </Badge>
  );
}

function TransactionRow({ transaction }: { transaction: Transaction }) {
  return (
    <div className="flex items-center justify-between p-4 border-b hover:bg-gray-50">
      <div className="flex items-center space-x-4">
        <TransactionIcon type={transaction.type} />
        <div>
          <h4 className="font-medium">{transaction.description}</h4>
          <div className="flex items-center space-x-2 text-sm text-muted-foreground">
            <span>{new Date(transaction.date).toLocaleDateString()}</span>
            <span>•</span>
            <span>Ref: {transaction.reference}</span>
            <span>•</span>
            <TransactionStatusBadge status={transaction.status} />
          </div>
        </div>
      </div>
      <div className="text-right">
        <div className={`font-semibold ${transaction.amount >= 0 ? 'text-green-600' : 'text-red-600'}`}>
          {transaction.amount >= 0 ? '+' : ''}{formatCurrency(transaction.amount, transaction.currency)}
        </div>
        <div className="text-sm text-muted-foreground">
          Balance: {formatCurrency(transaction.balance, transaction.currency)}
        </div>
      </div>
    </div>
  );
}

export default function AccountHistoryPage() {
  const params = useParams();
  const router = useRouter();
  const accountId = params.accountId as string;

  const { data: transactions, isLoading } = useQuery({
    queryKey: ['account-history', accountId],
    queryFn: async () => {
      // Mock API call
      await new Promise(resolve => setTimeout(resolve, 500));
      return mockTransactions;
    },
  });

  const stats = transactions ? {
    totalTransactions: transactions.length,
    totalInbound: transactions.filter(t => t.amount > 0).reduce((sum, t) => sum + t.amount, 0),
    totalOutbound: Math.abs(transactions.filter(t => t.amount < 0).reduce((sum, t) => sum + t.amount, 0)),
    netChange: transactions.reduce((sum, t) => sum + t.amount, 0),
  } : null;

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button variant="ghost" size="sm" onClick={() => router.back()}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Accounts
            </Button>
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Account History</h1>
              <p className="text-muted-foreground">
                Transaction history for account {accountId}
              </p>
            </div>
          </div>
          <div className="flex space-x-2">
            <Button variant="outline">
              <Filter className="h-4 w-4 mr-2" />
              Filter
            </Button>
            <Button variant="outline">
              <Download className="h-4 w-4 mr-2" />
              Export
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Transactions</CardTitle>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.totalTransactions}</div>
                <p className="text-xs text-muted-foreground">All time</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Inbound</CardTitle>
                <TrendingUp className="h-4 w-4 text-green-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">
                  +{formatCurrency(stats.totalInbound, 'USD')}
                </div>
                <p className="text-xs text-muted-foreground">Money received</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Outbound</CardTitle>
                <TrendingDown className="h-4 w-4 text-red-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">
                  -{formatCurrency(stats.totalOutbound, 'USD')}
                </div>
                <p className="text-xs text-muted-foreground">Money sent</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Net Change</CardTitle>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className={`text-2xl font-bold ${stats.netChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {stats.netChange >= 0 ? '+' : ''}{formatCurrency(stats.netChange, 'USD')}
                </div>
                <p className="text-xs text-muted-foreground">Total change</p>
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
                  placeholder="Search transactions by description or reference..."
                  className="max-w-md"
                />
              </div>
              <Select defaultValue="all">
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="Type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  <SelectItem value="inbound">Inbound</SelectItem>
                  <SelectItem value="outbound">Outbound</SelectItem>
                  <SelectItem value="fx">FX Exchange</SelectItem>
                  <SelectItem value="deposit">Deposit</SelectItem>
                </SelectContent>
              </Select>
              <Select defaultValue="all">
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="Currency" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Currencies</SelectItem>
                  <SelectItem value="usd">USD</SelectItem>
                  <SelectItem value="eur">EUR</SelectItem>
                  <SelectItem value="gbp">GBP</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Transactions List */}
        <Card>
          <CardHeader>
            <CardTitle>Transaction History</CardTitle>
            <CardDescription>
              Complete list of account transactions
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            {isLoading ? (
              <div className="flex items-center justify-center min-h-[200px]">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              </div>
            ) : (
              <div>
                {transactions?.map((transaction) => (
                  <TransactionRow key={transaction.id} transaction={transaction} />
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  );
}
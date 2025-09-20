'use client';

import { useQuery } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { CheckCircle, Clock, AlertCircle, XCircle, ArrowRight } from 'lucide-react';
import { paymentsClient, ledgerClient, formatMinorCurrency, formatRelativeTime } from '@/lib/api';
import type { Payment, PaymentStatus, LedgerEntry } from '@/lib/api/types';

function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  const getStatusConfig = (status: PaymentStatus) => {
    switch (status) {
      case 'REQUESTED':
        return { variant: 'default' as const, icon: Clock, color: 'text-blue-600' };
      case 'DEBITED':
        return { variant: 'warning' as const, icon: Clock, color: 'text-orange-600' };
      case 'CREDITED':
        return { variant: 'success' as const, icon: CheckCircle, color: 'text-green-600' };
      case 'COMPLETED':
        return { variant: 'success' as const, icon: CheckCircle, color: 'text-green-600' };
      case 'FAILED':
        return { variant: 'destructive' as const, icon: XCircle, color: 'text-red-600' };
      case 'COMPENSATED':
        return { variant: 'secondary' as const, icon: AlertCircle, color: 'text-gray-600' };
      default:
        return { variant: 'outline' as const, icon: Clock, color: 'text-gray-600' };
    }
  };

  const config = getStatusConfig(status);
  const Icon = config.icon;

  return (
    <Badge variant={config.variant} className="flex items-center space-x-1">
      <Icon className="h-3 w-3" />
      <span>{status}</span>
    </Badge>
  );
}

function PaymentTimeline({ payment }: { payment: Payment }) {
  const timelineSteps = [
    { status: 'REQUESTED', label: 'Payment Requested', completed: true },
    { status: 'DEBITED', label: 'Funds Debited', completed: ['DEBITED', 'CREDITED', 'COMPLETED'].includes(payment.status) },
    { status: 'CREDITED', label: 'Funds Credited', completed: ['CREDITED', 'COMPLETED'].includes(payment.status) },
    { status: 'COMPLETED', label: 'Payment Completed', completed: payment.status === 'COMPLETED' },
  ];

  return (
    <div className="space-y-4">
      {timelineSteps.map((step, index) => (
        <div key={step.status} className="flex items-center space-x-4">
          <div
            className={`w-8 h-8 rounded-full flex items-center justify-center ${
              step.completed
                ? 'bg-success text-success-foreground'
                : 'bg-gray-200 text-gray-500'
            }`}
          >
            {step.completed ? (
              <CheckCircle className="h-4 w-4" />
            ) : (
              <span className="text-xs font-medium">{index + 1}</span>
            )}
          </div>
          <div className="flex-1">
            <p className={`font-medium ${step.completed ? 'text-gray-900' : 'text-gray-500'}`}>
              {step.label}
            </p>
            {step.completed && (
              <p className="text-sm text-muted-foreground">
                {formatRelativeTime(payment.updatedAt)}
              </p>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

export default function PaymentDetailPage() {
  const params = useParams();
  const paymentId = params.id as string;

  const { data: payment, isLoading: paymentLoading } = useQuery({
    queryKey: ['payment', paymentId],
    queryFn: async () => {
      const response = await paymentsClient.getPayment(paymentId);
      return response.data;
    },
    // Mock data for development
    initialData: {
      id: paymentId,
      requestId: 'req_1234567890',
      fromAccountId: 'acc_1',
      toAccountId: 'acc_2',
      amountMinor: 125000, // $1,250.00
      currency: 'USD',
      status: 'COMPLETED',
      createdAt: new Date(Date.now() - 300000).toISOString(), // 5 minutes ago
      updatedAt: new Date(Date.now() - 60000).toISOString(), // 1 minute ago
    } as Payment,
  });

  const { data: ledgerEntries, isLoading: ledgerLoading } = useQuery({
    queryKey: ['ledger-entries', paymentId],
    queryFn: async () => {
      const response = await ledgerClient.getLedgerEntriesForPayment(paymentId);
      return response.data;
    },
    // Mock data for development
    initialData: [
      {
        id: 'entry_1',
        accountId: 'acc_1',
        paymentId: paymentId,
        debitAmount: 125000,
        currency: 'USD',
        description: 'Payment debit',
        timestamp: new Date(Date.now() - 120000).toISOString(),
        balanceAfter: 375000,
      },
      {
        id: 'entry_2',
        accountId: 'acc_2',
        paymentId: paymentId,
        creditAmount: 125000,
        currency: 'USD',
        description: 'Payment credit',
        timestamp: new Date(Date.now() - 60000).toISOString(),
        balanceAfter: 475000,
      },
    ] as LedgerEntry[],
  });

  if (paymentLoading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      </MainLayout>
    );
  }

  if (!payment) {
    return (
      <MainLayout>
        <div className="text-center py-12">
          <h2 className="text-2xl font-bold">Payment not found</h2>
          <p className="text-muted-foreground mt-2">
            The payment you&apos;re looking for doesn&apos;t exist or you don&apos;t have permission to view it.
          </p>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Payment Details</h1>
            <p className="text-muted-foreground">Payment ID: {payment.id}</p>
          </div>
          <PaymentStatusBadge status={payment.status} />
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          {/* Payment Information */}
          <div className="md:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Payment Information</CardTitle>
                <CardDescription>
                  Details about this payment transaction
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Amount</label>
                    <p className="text-2xl font-bold">
                      {formatMinorCurrency(payment.amountMinor, payment.currency)}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Currency</label>
                    <p className="text-lg font-medium">{payment.currency}</p>
                  </div>
                </div>

                <div className="flex items-center justify-center py-4">
                  <div className="flex items-center space-x-4">
                    <div className="text-center">
                      <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-2">
                        <span className="text-sm font-medium text-blue-600">
                          {payment.fromAccountId.slice(-3).toUpperCase()}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground">From Account</p>
                      <p className="text-sm font-medium">{payment.fromAccountId}</p>
                    </div>
                    
                    <ArrowRight className="h-6 w-6 text-muted-foreground" />
                    
                    <div className="text-center">
                      <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-2">
                        <span className="text-sm font-medium text-green-600">
                          {payment.toAccountId.slice(-3).toUpperCase()}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground">To Account</p>
                      <p className="text-sm font-medium">{payment.toAccountId}</p>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 pt-4 border-t">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Request ID</label>
                    <p className="text-sm font-mono">{payment.requestId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Created</label>
                    <p className="text-sm">{formatRelativeTime(payment.createdAt)}</p>
                  </div>
                </div>

                {payment.failureReason && (
                  <div className="p-4 bg-red-50 border border-red-200 rounded-md">
                    <h4 className="text-sm font-medium text-red-800">Failure Reason</h4>
                    <p className="text-sm text-red-700 mt-1">{payment.failureReason}</p>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Ledger Entries */}
            <Card>
              <CardHeader>
                <CardTitle>Ledger Entries</CardTitle>
                <CardDescription>
                  Double-entry accounting records for this payment
                </CardDescription>
              </CardHeader>
              <CardContent>
                {ledgerLoading ? (
                  <div className="text-center py-4">
                    <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary mx-auto"></div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {ledgerEntries?.map((entry) => (
                      <div
                        key={entry.id}
                        className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
                      >
                        <div>
                          <p className="font-medium">{entry.description}</p>
                          <p className="text-sm text-muted-foreground">
                            Account: {entry.accountId}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {formatRelativeTime(entry.timestamp)}
                          </p>
                        </div>
                        <div className="text-right">
                          {entry.debitAmount && (
                            <p className="text-red-600 font-medium">
                              -{formatMinorCurrency(entry.debitAmount, entry.currency)}
                            </p>
                          )}
                          {entry.creditAmount && (
                            <p className="text-green-600 font-medium">
                              +{formatMinorCurrency(entry.creditAmount, entry.currency)}
                            </p>
                          )}
                          <p className="text-xs text-muted-foreground">
                            Balance: {formatMinorCurrency(entry.balanceAfter, entry.currency)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Payment Timeline */}
          <div>
            <Card>
              <CardHeader>
                <CardTitle>Payment Timeline</CardTitle>
                <CardDescription>
                  Track the progress of your payment
                </CardDescription>
              </CardHeader>
              <CardContent>
                <PaymentTimeline payment={payment} />
              </CardContent>
            </Card>

            {/* Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Button variant="outline" size="sm" className="w-full">
                  Download Receipt
                </Button>
                <Button variant="outline" size="sm" className="w-full">
                  View Full Details
                </Button>
                {payment.status === 'FAILED' && (
                  <Button variant="default" size="sm" className="w-full">
                    Retry Payment
                  </Button>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </MainLayout>
  );
}
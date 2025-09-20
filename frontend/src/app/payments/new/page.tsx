'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { toast } from 'sonner';

import { MainLayout } from '@/components/layout/main-layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Badge } from '@/components/ui/badge';

import { apiClient, generateRequestId, toMinorUnits } from '@/lib/api';
import type { Currency } from '@/lib/api/types';

const paymentFormSchema = z.object({
  fromAccount: z.string().min(1, 'Please select a source account'),
  toAccount: z.string().min(1, 'Please select a destination account'),
  amount: z.number().min(0.01, 'Amount must be at least 0.01').max(1000000, 'Amount cannot exceed 1,000,000'),
  currency: z.enum(['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD', 'CRC']),
  description: z.string().max(255, 'Description cannot exceed 255 characters').optional(),
});

type PaymentFormValues = z.infer<typeof paymentFormSchema>;

export default function NewPaymentPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<PaymentFormValues>({
    resolver: zodResolver(paymentFormSchema),
    defaultValues: {
      currency: 'USD',
      amount: 0,
    },
  });

  // Mock account data - in real app, fetch from API
  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: async () => {
      // Mock accounts data
      return [
        { id: 'acc_1', userId: 'user_1', currency: 'USD' as Currency, balance: 5000.00, status: 'ACTIVE' },
        { id: 'acc_2', userId: 'user_1', currency: 'EUR' as Currency, balance: 3500.50, status: 'ACTIVE' },
        { id: 'acc_3', userId: 'user_2', currency: 'USD' as Currency, balance: 2000.00, status: 'ACTIVE' },
      ];
    },
  });

  const paymentMutation = useMutation({
    mutationFn: async (data: PaymentFormValues) => {
      const requestId = generateRequestId();

      // Mock payment creation - simulate API call
      await new Promise(resolve => setTimeout(resolve, 1500)); // Simulate network delay

      // Mock successful payment response
      const mockPayment = {
        id: `pay_${Date.now()}`,
        requestId,
        fromAccount: data.fromAccount,
        toAccount: data.toAccount,
        amount: data.amount,
        currency: data.currency,
        description: data.description || 'Payment transfer',
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        reference: `PAY-${Date.now()}`,
      };

      return { payment: mockPayment, requestId };
    },
    onSuccess: ({ payment, requestId }) => {
      toast.success('Payment initiated successfully', {
        description: `Payment ID: ${payment.id}`,
      });
      
      // Redirect to payment status page
      router.push(`/payments/${payment.id}`);
    },
    onError: (error: any) => {
      toast.error('Failed to create payment', {
        description: error.message || 'An unexpected error occurred',
      });
    },
  });

  const onSubmit = async (data: PaymentFormValues) => {
    setIsSubmitting(true);
    try {
      await paymentMutation.mutateAsync(data);
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedFromAccount = accounts?.find(acc => acc.id === form.watch('fromAccount'));
  const availableToAccounts = accounts?.filter(acc => acc.id !== form.watch('fromAccount'));

  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Create Payment</h1>
          <p className="text-muted-foreground">
            Transfer funds between accounts securely
          </p>
        </div>

        <div className="max-w-2xl">
          <Card>
            <CardHeader>
              <CardTitle>Payment Details</CardTitle>
              <CardDescription>
                Enter the payment information. All fields are required.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                  {/* From Account */}
                  <FormField
                    control={form.control}
                    name="fromAccount"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>From Account</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select source account" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {accounts?.map((account) => (
                              <SelectItem key={account.id} value={account.id}>
                                <div className="flex items-center justify-between w-full">
                                  <span>{account.id}</span>
                                  <div className="flex items-center space-x-2 ml-4">
                                    <Badge variant="outline">{account.currency}</Badge>
                                    <span className="text-sm text-muted-foreground">
                                      ${account.balance.toFixed(2)}
                                    </span>
                                  </div>
                                </div>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          Choose the account to send money from
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* To Account */}
                  <FormField
                    control={form.control}
                    name="toAccount"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>To Account</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select destination account" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {availableToAccounts?.map((account) => (
                              <SelectItem key={account.id} value={account.id}>
                                <div className="flex items-center justify-between w-full">
                                  <span>{account.id}</span>
                                  <div className="flex items-center space-x-2 ml-4">
                                    <Badge variant="outline">{account.currency}</Badge>
                                    <span className="text-sm text-muted-foreground">
                                      ${account.balance.toFixed(2)}
                                    </span>
                                  </div>
                                </div>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          Choose the account to send money to
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Amount */}
                  <FormField
                    control={form.control}
                    name="amount"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Amount</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.01"
                            placeholder="0.00"
                            {...field}
                            onChange={(e) => field.onChange(parseFloat(e.target.value) || 0)}
                          />
                        </FormControl>
                        <FormDescription>
                          Enter the amount to transfer
                          {selectedFromAccount && (
                            <span className="block text-xs text-muted-foreground mt-1">
                              Available balance: ${selectedFromAccount.balance.toFixed(2)} {selectedFromAccount.currency}
                            </span>
                          )}
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Currency */}
                  <FormField
                    control={form.control}
                    name="currency"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Currency</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select currency" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="USD">USD - US Dollar</SelectItem>
                            <SelectItem value="EUR">EUR - Euro</SelectItem>
                            <SelectItem value="GBP">GBP - British Pound</SelectItem>
                            <SelectItem value="JPY">JPY - Japanese Yen</SelectItem>
                            <SelectItem value="CHF">CHF - Swiss Franc</SelectItem>
                            <SelectItem value="CAD">CAD - Canadian Dollar</SelectItem>
                            <SelectItem value="AUD">AUD - Australian Dollar</SelectItem>
                            <SelectItem value="CRC">CRC - Costa Rican Colón</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          Payment currency
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Description */}
                  <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Description (Optional)</FormLabel>
                        <FormControl>
                          <Input
                            placeholder="Payment description"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>
                          Optional description for this payment
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Submit Button */}
                  <div className="flex space-x-4">
                    <Button
                      type="submit"
                      disabled={isSubmitting}
                      className="flex-1"
                    >
                      {isSubmitting ? 'Creating Payment...' : 'Create Payment'}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => router.back()}
                    >
                      Cancel
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </div>
      </div>
    </MainLayout>
  );
}
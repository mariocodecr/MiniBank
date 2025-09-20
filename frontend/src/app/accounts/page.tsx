'use client';

import { useQuery } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Building2, Plus, CreditCard, TrendingUp, TrendingDown } from 'lucide-react';
import { accountsClient, formatCurrency } from '@/lib/api';
import type { MultiCurrencyAccount, CurrencyBalance } from '@/lib/api/types';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';

// Available currencies with their details
const AVAILABLE_CURRENCIES = [
  { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$' },
  { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' },
  { code: 'CHF', name: 'Swiss Franc', symbol: 'CHF' },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥' },
  { code: 'CRC', name: 'Costa Rican Colón', symbol: '₡' },
];

function AddCurrencyDialog({
  account,
  onCurrencyAdded
}: {
  account: MultiCurrencyAccount;
  onCurrencyAdded: (currency: string) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedCurrency, setSelectedCurrency] = useState<string>('');
  const [isAdding, setIsAdding] = useState(false);

  const currentCurrencies = account.balances.map(b => b.currency);
  const availableCurrencies = AVAILABLE_CURRENCIES.filter(
    currency => !currentCurrencies.includes(currency.code)
  );

  const handleAddCurrency = async () => {
    if (!selectedCurrency) {
      toast.error('Please select a currency');
      return;
    }

    setIsAdding(true);
    try {
      // Simulate API call to add currency
      await new Promise(resolve => setTimeout(resolve, 1500));

      onCurrencyAdded(selectedCurrency);
      toast.success(`${selectedCurrency} has been added to your account`);
      setIsOpen(false);
      setSelectedCurrency('');
    } catch (error) {
      toast.error('Failed to add currency. Please try again.');
    } finally {
      setIsAdding(false);
    }
  };

  if (availableCurrencies.length === 0) {
    return (
      <Button variant="outline" size="sm" disabled>
        <Plus className="h-3 w-3 mr-1" />
        Add Currency
      </Button>
    );
  }

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Plus className="h-3 w-3 mr-1" />
          Add Currency
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Add Currency</DialogTitle>
          <DialogDescription>
            Select a currency to add to your multi-currency account. You can start transacting immediately after adding.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="space-y-3">
            <Label>Available Currencies</Label>
            <div className="grid gap-2">
              {availableCurrencies.map((currency) => (
                <div
                  key={currency.code}
                  className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                    selectedCurrency === currency.code
                      ? 'border-primary bg-primary/10'
                      : 'border-border hover:border-primary/50'
                  }`}
                  onClick={() => setSelectedCurrency(currency.code)}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{currency.code}</div>
                      <div className="text-sm text-muted-foreground">{currency.name}</div>
                    </div>
                    <div className="text-lg font-bold text-muted-foreground">
                      {currency.symbol}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setIsOpen(false)} disabled={isAdding}>
            Cancel
          </Button>
          <Button onClick={handleAddCurrency} disabled={!selectedCurrency || isAdding}>
            {isAdding ? 'Adding...' : 'Add Currency'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function CurrencyBalanceCard({ balance }: { balance: CurrencyBalance }) {
  const router = useRouter();
  const totalAmount = balance.totalAmount;
  const availableAmount = balance.availableAmount;
  const reservedAmount = balance.reservedAmount;

  const handleTransfer = () => {
    router.push(`/payments/new?currency=${balance.currency}&amount=${availableAmount}`);
  };

  const handleDeposit = () => {
    alert(`Deposit functionality for ${balance.currency} coming soon!\nAvailable balance: ${formatCurrency(availableAmount, balance.currency)}`);
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">
          {balance.currency}
        </CardTitle>
        <Badge variant="outline" className="currency-badge">
          {balance.currency}
        </Badge>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">
          {formatCurrency(totalAmount, balance.currency)}
        </div>
        <div className="space-y-1 mt-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Available:</span>
            <span className="font-medium">
              {formatCurrency(availableAmount, balance.currency)}
            </span>
          </div>
          {reservedAmount > 0 && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Reserved:</span>
              <span className="font-medium text-orange-600">
                {formatCurrency(reservedAmount, balance.currency)}
              </span>
            </div>
          )}
        </div>
        <div className="flex space-x-2 mt-4">
          <Button size="sm" variant="outline" className="flex-1" onClick={handleTransfer}>
            <CreditCard className="h-3 w-3 mr-1" />
            Transfer
          </Button>
          <Button size="sm" variant="outline" className="flex-1" onClick={handleDeposit}>
            <TrendingUp className="h-3 w-3 mr-1" />
            Deposit
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function AccountCard({ account, onAccountUpdate }: {
  account: MultiCurrencyAccount;
  onAccountUpdate: (updatedAccount: MultiCurrencyAccount) => void;
}) {
  const router = useRouter();
  const totalValue = account.balances.reduce((sum, balance) => {
    // Convert all to USD for total calculation (simplified)
    return sum + balance.totalAmount;
  }, 0);

  const handleCurrencyAdded = (currencyCode: string) => {
    // Create new balance for the added currency
    const newBalance: CurrencyBalance = {
      currency: currencyCode as any,
      availableAmount: 0,
      reservedAmount: 0,
      totalAmount: 0,
      availableAmountMinor: 0,
      reservedAmountMinor: 0,
      totalAmountMinor: 0,
    };

    // Update the account with the new currency balance
    const updatedAccount: MultiCurrencyAccount = {
      ...account,
      balances: [...account.balances, newBalance],
      updatedAt: new Date().toISOString(),
    };

    onAccountUpdate(updatedAccount);
  };

  const handleViewHistory = () => {
    router.push(`/accounts/${account.id}/history`);
  };

  const handleAccountSettings = () => {
    router.push(`/accounts/${account.id}/settings`);
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Building2 className="h-5 w-5 text-muted-foreground" />
            <div>
              <CardTitle className="text-lg">Multi-Currency Account</CardTitle>
              <CardDescription>Account ID: {account.id}</CardDescription>
            </div>
          </div>
          <Badge 
            variant={account.status === 'ACTIVE' ? 'success' : 'secondary'}
          >
            {account.status}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="mb-4">
          <p className="text-sm text-muted-foreground">Total Portfolio Value</p>
          <p className="text-2xl font-bold">{formatCurrency(totalValue, 'USD')}</p>
        </div>
        
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {account.balances.map((balance) => (
            <CurrencyBalanceCard key={balance.currency} balance={balance} />
          ))}
        </div>

        <div className="flex space-x-2 mt-6 pt-4 border-t">
          <AddCurrencyDialog account={account} onCurrencyAdded={handleCurrencyAdded} />
          <Button variant="outline" size="sm" onClick={handleViewHistory}>
            View History
          </Button>
          <Button variant="outline" size="sm" onClick={handleAccountSettings}>
            Account Settings
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export default function AccountsPage() {
  const router = useRouter();
  const [isCreating, setIsCreating] = useState(false);
  const [localAccounts, setLocalAccounts] = useState<MultiCurrencyAccount[]>([]);

  const { data: accounts, isLoading } = useQuery({
    queryKey: ['multi-currency-accounts'],
    queryFn: async () => {
      // Mock data for development
      return [
        {
          id: 'acc_multi_1',
          userId: 'user_1',
          status: 'ACTIVE',
          createdAt: '2024-01-15T10:00:00Z',
          updatedAt: '2024-01-20T15:30:00Z',
          balances: [
            {
              currency: 'USD',
              availableAmount: 5000.00,
              reservedAmount: 250.00,
              totalAmount: 5250.00,
              availableAmountMinor: 500000,
              reservedAmountMinor: 25000,
              totalAmountMinor: 525000,
            },
            {
              currency: 'EUR',
              availableAmount: 3500.50,
              reservedAmount: 0.00,
              totalAmount: 3500.50,
              availableAmountMinor: 350050,
              reservedAmountMinor: 0,
              totalAmountMinor: 350050,
            },
            {
              currency: 'GBP',
              availableAmount: 2000.75,
              reservedAmount: 100.00,
              totalAmount: 2100.75,
              availableAmountMinor: 200075,
              reservedAmountMinor: 10000,
              totalAmountMinor: 210075,
            },
          ],
        },
      ] as MultiCurrencyAccount[];
    },
  });

  // Sync local state with query data
  useEffect(() => {
    if (accounts) {
      setLocalAccounts(accounts);
    }
  }, [accounts]);

  const handleAccountUpdate = (updatedAccount: MultiCurrencyAccount) => {
    setLocalAccounts(prev =>
      prev.map(account =>
        account.id === updatedAccount.id ? updatedAccount : account
      )
    );
  };

  // Use local accounts if available, otherwise use query data
  const displayAccounts = localAccounts.length > 0 ? localAccounts : accounts;

  const handleCreateAccount = async () => {
    setIsCreating(true);
    try {
      // Simulate account creation
      await new Promise(resolve => setTimeout(resolve, 2000));

      // Show success message or redirect
      alert('Multi-currency account created successfully!');

      // In a real app, you would refetch the accounts or add to the list
      window.location.reload();
    } catch (error) {
      console.error('Failed to create account:', error);
      alert('Failed to create account. Please try again.');
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Accounts</h1>
            <p className="text-muted-foreground">
              Manage your multi-currency accounts and balances
            </p>
          </div>
          <Button onClick={handleCreateAccount} disabled={isCreating}>
            <Plus className="h-4 w-4 mr-2" />
            {isCreating ? 'Creating...' : 'Create Account'}
          </Button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : (
          <div className="space-y-6">
            {displayAccounts?.map((account) => (
              <AccountCard key={account.id} account={account} onAccountUpdate={handleAccountUpdate} />
            ))}
          </div>
        )}

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>
              Common account operations
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-3">
              <Button
                variant="outline"
                className="h-20 flex-col"
                onClick={() => router.push('/payments/new')}
              >
                <CreditCard className="h-6 w-6 mb-2" />
                <span>New Payment</span>
              </Button>
              <Button
                variant="outline"
                className="h-20 flex-col"
                onClick={() => router.push('/fx')}
              >
                <TrendingUp className="h-6 w-6 mb-2" />
                <span>FX Exchange</span>
              </Button>
              <Button
                variant="outline"
                className="h-20 flex-col"
                onClick={() => alert('Statements feature coming soon!')}
              >
                <TrendingDown className="h-6 w-6 mb-2" />
                <span>View Statements</span>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </MainLayout>
  );
}
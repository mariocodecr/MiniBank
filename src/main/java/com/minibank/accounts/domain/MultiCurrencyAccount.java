package com.minibank.accounts.domain;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiCurrencyAccount {
    private final UUID id;
    private final String accountNumber;
    private final String accountHolderName;
    private final String email;
    private final AccountStatus status;
    private final Map<Currency, CurrencyBalance> currencyBalances;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    public MultiCurrencyAccount(UUID id, String accountNumber, String accountHolderName, String email,
                               AccountStatus status, Map<Currency, CurrencyBalance> currencyBalances,
                               LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        this.id = Objects.requireNonNull(id, "Account ID cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.accountHolderName = Objects.requireNonNull(accountHolderName, "Account holder name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.status = Objects.requireNonNull(status, "Account status cannot be null");
        this.currencyBalances = new ConcurrentHashMap<>(currencyBalances != null ? currencyBalances : new HashMap<>());
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        this.version = version;
    }

    public static MultiCurrencyAccount create(String accountNumber, String accountHolderName, String email) {
        LocalDateTime now = LocalDateTime.now();
        return new MultiCurrencyAccount(
            UUID.randomUUID(),
            accountNumber,
            accountHolderName,
            email,
            AccountStatus.ACTIVE,
            new HashMap<>(),
            now,
            now,
            0
        );
    }

    // Currency balance operations
    public CurrencyBalance getBalance(Currency currency) {
        return currencyBalances.getOrDefault(currency, CurrencyBalance.zero(currency));
    }

    public Set<Currency> getSupportedCurrencies() {
        return new HashSet<>(currencyBalances.keySet());
    }

    public Map<Currency, CurrencyBalance> getAllBalances() {
        return new HashMap<>(currencyBalances);
    }

    public boolean hasCurrency(Currency currency) {
        return currencyBalances.containsKey(currency);
    }

    public MultiCurrencyAccount enableCurrency(Currency currency) {
        if (currencyBalances.containsKey(currency)) {
            return this; // Already enabled
        }
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, CurrencyBalance.zero(currency));
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount credit(Currency currency, long amountMinor) {
        validateActiveStatus();
        
        CurrencyBalance currentBalance = getBalance(currency);
        CurrencyBalance newBalance = currentBalance.credit(amountMinor);
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, newBalance);
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount debit(Currency currency, long amountMinor) {
        validateActiveStatus();
        
        CurrencyBalance currentBalance = getBalance(currency);
        if (!currentBalance.hasAvailableBalance(amountMinor)) {
            throw new InsufficientFundsException(
                String.format("Insufficient %s balance: available=%d, requested=%d", 
                    currency.getCode(), currentBalance.getAvailableAmountMinor(), amountMinor)
            );
        }
        
        CurrencyBalance newBalance = currentBalance.debit(amountMinor);
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, newBalance);
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount reserve(Currency currency, long amountMinor) {
        validateActiveStatus();
        
        CurrencyBalance currentBalance = getBalance(currency);
        CurrencyBalance newBalance = currentBalance.reserve(amountMinor);
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, newBalance);
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount releaseReservation(Currency currency, long amountMinor) {
        validateActiveStatus();
        
        CurrencyBalance currentBalance = getBalance(currency);
        CurrencyBalance newBalance = currentBalance.releaseReservation(amountMinor);
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, newBalance);
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount useReservation(Currency currency, long amountMinor) {
        validateActiveStatus();
        
        CurrencyBalance currentBalance = getBalance(currency);
        CurrencyBalance newBalance = currentBalance.useReservation(amountMinor);
        
        Map<Currency, CurrencyBalance> newBalances = new HashMap<>(currencyBalances);
        newBalances.put(currency, newBalance);
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, status,
            newBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount suspend() {
        if (status == AccountStatus.SUSPENDED) {
            return this;
        }
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, AccountStatus.SUSPENDED,
            currencyBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount activate() {
        if (status == AccountStatus.ACTIVE) {
            return this;
        }
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, AccountStatus.ACTIVE,
            currencyBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    public MultiCurrencyAccount close() {
        // Verify all balances are zero before closing
        for (CurrencyBalance balance : currencyBalances.values()) {
            if (balance.getTotalAmountMinor() > 0) {
                throw new IllegalStateException(
                    String.format("Cannot close account with non-zero %s balance: %s", 
                        balance.getCurrency().getCode(), balance.getTotalAmount())
                );
            }
        }
        
        return new MultiCurrencyAccount(id, accountNumber, accountHolderName, email, AccountStatus.CLOSED,
            currencyBalances, createdAt, LocalDateTime.now(), version + 1);
    }

    private void validateActiveStatus() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: " + status);
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getEmail() { return email; }
    public AccountStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiCurrencyAccount account = (MultiCurrencyAccount) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("MultiCurrencyAccount{id=%s, accountNumber='%s', status=%s, currencies=%d, version=%d}",
            id, accountNumber, status, currencyBalances.size(), version);
    }
}
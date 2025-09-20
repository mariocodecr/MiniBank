package com.minibank.accounts.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Account {
    private UUID id;
    private UUID userId;
    private Currency currency;
    private long balanceMinor;
    private AccountStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Account create(UUID userId, Currency currency) {
        return new Account(
            UUID.randomUUID(),
            userId,
            currency,
            0L,
            AccountStatus.ACTIVE,
            0L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    public Money getBalance() {
        return Money.of(balanceMinor, currency);
    }

    public void debit(Money amount) {
        validateCurrency(amount);
        validateActiveStatus();
        
        if (balanceMinor < amount.getMinorUnits()) {
            throw new IllegalStateException("Insufficient funds for debit operation");
        }
        
        this.balanceMinor -= amount.getMinorUnits();
        this.updatedAt = LocalDateTime.now();
    }

    public void credit(Money amount) {
        validateCurrency(amount);
        validateActiveStatus();
        
        this.balanceMinor += amount.getMinorUnits();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canDebit(Money amount) {
        validateCurrency(amount);
        return status == AccountStatus.ACTIVE && balanceMinor >= amount.getMinorUnits();
    }

    public void suspend() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed account");
        }
        this.status = AccountStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate a closed account");
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if (balanceMinor != 0) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }
        this.status = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateCurrency(Money amount) {
        if (this.currency != amount.getCurrency()) {
            throw new IllegalArgumentException(
                "Currency mismatch: account currency is " + this.currency + 
                " but amount currency is " + amount.getCurrency());
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Protected constructors
    protected Account() {}

    public Account(UUID id, UUID userId, Currency currency, long balanceMinor, 
                   AccountStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balanceMinor = balanceMinor;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private void validateActiveStatus() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account must be active for this operation");
        }
    }
}
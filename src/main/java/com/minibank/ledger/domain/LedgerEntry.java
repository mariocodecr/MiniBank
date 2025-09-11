package com.minibank.ledger.domain;

import com.minibank.accounts.domain.Currency;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {
    private UUID id;
    private UUID paymentId;
    private UUID accountId;
    private EntryType entryType;
    private long amountMinor;
    private Currency currency;
    private LocalDateTime occurredAt;

    protected LedgerEntry() {}

    public LedgerEntry(UUID id, UUID paymentId, UUID accountId, EntryType entryType,
                      long amountMinor, Currency currency, LocalDateTime occurredAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }

    public static LedgerEntry createDebit(UUID paymentId, UUID accountId, long amountMinor, Currency currency) {
        validateEntry(paymentId, accountId, amountMinor, currency);
        return new LedgerEntry(
            UUID.randomUUID(),
            paymentId,
            accountId,
            EntryType.DEBIT,
            amountMinor,
            currency,
            LocalDateTime.now()
        );
    }

    public static LedgerEntry createCredit(UUID paymentId, UUID accountId, long amountMinor, Currency currency) {
        validateEntry(paymentId, accountId, amountMinor, currency);
        return new LedgerEntry(
            UUID.randomUUID(),
            paymentId,
            accountId,
            EntryType.CREDIT,
            amountMinor,
            currency,
            LocalDateTime.now()
        );
    }

    public long getSignedAmount() {
        return entryType == EntryType.DEBIT ? -amountMinor : amountMinor;
    }

    private static void validateEntry(UUID paymentId, UUID accountId, long amountMinor, Currency currency) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getAccountId() { return accountId; }
    public EntryType getEntryType() { return entryType; }
    public long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
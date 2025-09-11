package com.minibank.ledger.adapter.web.dto;

import com.minibank.accounts.domain.Currency;
import com.minibank.ledger.domain.EntryType;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntryResponse {
    private UUID id;
    private UUID paymentId;
    private UUID accountId;
    private EntryType entryType;
    private Long amountMinor;
    private Currency currency;
    private LocalDateTime occurredAt;

    public LedgerEntryResponse() {}

    public LedgerEntryResponse(UUID id, UUID paymentId, UUID accountId, EntryType entryType,
                              Long amountMinor, Currency currency, LocalDateTime occurredAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getAccountId() { return accountId; }
    public EntryType getEntryType() { return entryType; }
    public Long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
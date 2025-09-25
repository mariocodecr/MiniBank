package com.minibank.ledger.adapter.persistence;

import com.minibank.ledger.domain.EntryType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@EntityListeners(AuditingEntityListener.class)
public class LedgerEntryEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public LedgerEntryEntity() {}

    public LedgerEntryEntity(UUID id, UUID paymentId, UUID accountId, EntryType entryType,
                            Long amountMinor, LocalDateTime occurredAt, LocalDateTime createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amountMinor = amountMinor;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }

    public Long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
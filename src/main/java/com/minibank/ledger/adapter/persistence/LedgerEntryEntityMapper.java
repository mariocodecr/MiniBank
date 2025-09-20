package com.minibank.ledger.adapter.persistence;

import com.minibank.ledger.domain.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryEntityMapper {
    
    public LedgerEntryEntity toEntity(LedgerEntry ledgerEntry) {
        return new LedgerEntryEntity(
            ledgerEntry.getId(),
            ledgerEntry.getPaymentId(),
            ledgerEntry.getAccountId(),
            ledgerEntry.getEntryType(),
            ledgerEntry.getAmountMinor(),
            ledgerEntry.getCurrency(),
            ledgerEntry.getOccurredAt(),
            null // createdAt will be set by JPA auditing
        );
    }
    
    public LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(
            entity.getId(),
            entity.getPaymentId(),
            entity.getAccountId(),
            entity.getEntryType(),
            entity.getAmountMinor(),
            entity.getCurrency(),
            entity.getOccurredAt()
        );
    }
}
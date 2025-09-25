package com.minibank.ledger.adapter.persistence;

import com.minibank.ledger.domain.LedgerEntry;
import com.minibank.accounts.domain.Currency;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.minibank.accounts.adapter.persistence.AccountJpaRepository;

@Component
public class LedgerEntryEntityMapper {

    private final AccountJpaRepository accountRepository;

    @Autowired
    public LedgerEntryEntityMapper(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public LedgerEntryEntity toEntity(LedgerEntry ledgerEntry) {
        return new LedgerEntryEntity(
            ledgerEntry.getId(),
            ledgerEntry.getPaymentId(),
            ledgerEntry.getAccountId(),
            ledgerEntry.getEntryType(),
            ledgerEntry.getAmountMinor(),
            ledgerEntry.getOccurredAt(),
            null // createdAt will be set by JPA auditing
        );
    }

    public LedgerEntry toDomain(LedgerEntryEntity entity, Currency currency) {
        return new LedgerEntry(
            entity.getId(),
            entity.getPaymentId(),
            entity.getAccountId(),
            entity.getEntryType(),
            entity.getAmountMinor(),
            currency,
            entity.getOccurredAt()
        );
    }

    public LedgerEntry toDomain(LedgerEntryEntity entity) {
        // Get currency from the account
        Currency currency = accountRepository.findById(entity.getAccountId())
            .map(account -> account.getCurrency())
            .orElse(Currency.USD); // fallback currency

        return toDomain(entity, currency);
    }
}
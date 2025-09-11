package com.minibank.ledger.domain;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository {
    LedgerEntry save(LedgerEntry ledgerEntry);
    List<LedgerEntry> findByPaymentId(UUID paymentId);
    List<LedgerEntry> saveAll(List<LedgerEntry> entries);
}
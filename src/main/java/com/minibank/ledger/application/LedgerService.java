package com.minibank.ledger.application;

import com.minibank.accounts.domain.Currency;
import com.minibank.ledger.domain.LedgerEntry;
import com.minibank.ledger.domain.LedgerEntryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LedgerService {
    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final Counter entriesRecorded;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, MeterRegistry meterRegistry) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.entriesRecorded = Counter.builder("ledger.entries.recorded.total")
            .description("Total number of ledger entries recorded")
            .register(meterRegistry);
    }

    public List<LedgerEntry> recordPaymentEntries(UUID paymentId, UUID fromAccountId, 
                                                 UUID toAccountId, long amountMinor, Currency currency) {
        logger.info("Recording ledger entries for payment: {}, from: {}, to: {}, amount: {}", 
                   paymentId, fromAccountId, toAccountId, amountMinor);

        // Create double-entry ledger entries
        LedgerEntry debitEntry = LedgerEntry.createDebit(paymentId, fromAccountId, amountMinor, currency);
        LedgerEntry creditEntry = LedgerEntry.createCredit(paymentId, toAccountId, amountMinor, currency);

        // Validate double-entry invariant
        validateDoubleEntry(Arrays.asList(debitEntry, creditEntry));

        // Save both entries atomically
        List<LedgerEntry> entries = ledgerEntryRepository.saveAll(Arrays.asList(debitEntry, creditEntry));
        
        entriesRecorded.increment(2);
        
        logger.info("Successfully recorded {} ledger entries for payment: {}", entries.size(), paymentId);
        return entries;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesForPayment(UUID paymentId) {
        logger.debug("Retrieving ledger entries for payment: {}", paymentId);
        return ledgerEntryRepository.findByPaymentId(paymentId);
    }

    private void validateDoubleEntry(List<LedgerEntry> entries) {
        long total = entries.stream()
            .mapToLong(LedgerEntry::getSignedAmount)
            .sum();
        
        if (total != 0) {
            throw new IllegalStateException("Double-entry ledger violation: entries do not sum to zero");
        }
    }
}
package com.minibank.payments.adapter.client;

import com.minibank.accounts.domain.Currency;
import com.minibank.ledger.application.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LedgerServiceClientImpl implements LedgerServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceClientImpl.class);

    // For M1, we'll implement this as in-process calls to the ledger service
    // In M2, this would be HTTP calls or message-based communication
    
    private final LedgerService ledgerService;
    
    public LedgerServiceClientImpl(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public LedgerResult recordPaymentEntries(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                           long amountMinor, Currency currency) {
        try {
            logger.debug("Recording ledger entries for payment: {}, from: {}, to: {}, amount: {}", 
                        paymentId, fromAccountId, toAccountId, amountMinor);
            
            ledgerService.recordPaymentEntries(paymentId, fromAccountId, toAccountId, amountMinor, currency);
            return LedgerResult.success();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Ledger entry recording failed for payment {}: {}", paymentId, e.getMessage());
            return LedgerResult.failure(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Ledger entry recording failed for payment {}: {}", paymentId, e.getMessage());
            return LedgerResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error recording ledger entries for payment {}", paymentId, e);
            return LedgerResult.failure("System error: " + e.getMessage());
        }
    }
}
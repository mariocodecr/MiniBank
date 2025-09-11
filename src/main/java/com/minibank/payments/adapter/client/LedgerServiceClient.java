package com.minibank.payments.adapter.client;

import com.minibank.accounts.domain.Currency;

import java.util.UUID;

public interface LedgerServiceClient {
    LedgerResult recordPaymentEntries(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                     long amountMinor, Currency currency);
    
    public static class LedgerResult {
        private final boolean success;
        private final String errorMessage;
        
        private LedgerResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static LedgerResult success() {
            return new LedgerResult(true, null);
        }
        
        public static LedgerResult failure(String errorMessage) {
            return new LedgerResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}
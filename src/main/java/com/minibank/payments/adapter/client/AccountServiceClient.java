package com.minibank.payments.adapter.client;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.Money;

import java.util.UUID;

public interface AccountServiceClient {
    ReserveResult reserveFunds(UUID accountId, Money amount);
    PostResult postCredit(UUID accountId, Money amount);
    PostResult postDebit(UUID accountId, Money amount);
    
    public static class ReserveResult {
        private final boolean success;
        private final String errorMessage;
        
        private ReserveResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static ReserveResult success() {
            return new ReserveResult(true, null);
        }
        
        public static ReserveResult failure(String errorMessage) {
            return new ReserveResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class PostResult {
        private final boolean success;
        private final String errorMessage;
        
        private PostResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static PostResult success() {
            return new PostResult(true, null);
        }
        
        public static PostResult failure(String errorMessage) {
            return new PostResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}
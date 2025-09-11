package com.minibank.accounts.adapter.web.dto;

import com.minibank.accounts.domain.Currency;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateAccountRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    public CreateAccountRequest() {}
    
    public CreateAccountRequest(UUID userId, Currency currency) {
        this.userId = userId;
        this.currency = currency;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public Currency getCurrency() {
        return currency;
    }
}
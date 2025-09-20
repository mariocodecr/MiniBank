package com.minibank.accounts.adapter.web.dto;

import java.util.UUID;

import com.minibank.accounts.domain.Currency;

import jakarta.validation.constraints.NotNull;

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
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
package com.minibank.accounts.adapter.web.dto;

import com.minibank.accounts.domain.AccountStatus;
import com.minibank.accounts.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponse {
    private UUID id;
    private UUID userId;
    private Currency currency;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public AccountResponse() {}
    
    public AccountResponse(UUID id, UUID userId, Currency currency, BigDecimal balance, 
                          AccountStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
package com.minibank.accounts.adapter.web.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MultiCurrencyAccountResponse {
    
    @NotNull
    private UUID id;
    
    @NotBlank
    private String accountNumber;
    
    @NotBlank
    private String accountHolderName;
    
    @NotBlank
    private String email;
    
    @NotBlank
    private String status;
    
    @NotNull
    private Map<String, CurrencyBalanceResponse> balances;
    
    @NotNull
    private LocalDateTime createdAt;
    
    @NotNull
    private LocalDateTime updatedAt;
    
    private int version;

    public MultiCurrencyAccountResponse() {}

    public MultiCurrencyAccountResponse(UUID id, String accountNumber, String accountHolderName, 
                                      String email, String status, Map<String, CurrencyBalanceResponse> balances,
                                      LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.email = email;
        this.status = status;
        this.balances = balances;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, CurrencyBalanceResponse> getBalances() { return balances; }
    public void setBalances(Map<String, CurrencyBalanceResponse> balances) { this.balances = balances; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}










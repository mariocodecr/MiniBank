package com.minibank.payments.adapter.web.dto;

import com.minibank.accounts.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreatePaymentRequest {
    @NotBlank(message = "Request ID is required")
    private String requestId;
    
    @NotNull(message = "From account ID is required")
    private UUID fromAccount;
    
    @NotNull(message = "To account ID is required")
    private UUID toAccount;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1", message = "Amount must be greater than 0")
    private Long amountMinor;
    
    @NotNull(message = "Currency is required")
    private Currency currency;

    public CreatePaymentRequest() {}

    public CreatePaymentRequest(String requestId, UUID fromAccount, UUID toAccount, 
                               Long amountMinor, Currency currency) {
        this.requestId = requestId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amountMinor = amountMinor;
        this.currency = currency;
    }

    // Getters
    public String getRequestId() { return requestId; }
    public UUID getFromAccount() { return fromAccount; }
    public UUID getToAccount() { return toAccount; }
    public Long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    
    // Setters for Jackson
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setFromAccount(UUID fromAccount) { this.fromAccount = fromAccount; }
    public void setToAccount(UUID toAccount) { this.toAccount = toAccount; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }
    public void setCurrency(Currency currency) { this.currency = currency; }
}
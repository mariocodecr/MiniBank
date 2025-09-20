package com.minibank.fx.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ConversionRequest {
    
    @NotNull(message = "Account ID is required")
    private UUID accountId;
    
    @NotBlank(message = "From currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String fromCurrency;
    
    @NotBlank(message = "To currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String toCurrency;
    
    @Positive(message = "Amount must be positive")
    private long fromAmountMinor;
    
    @NotBlank(message = "Correlation ID is required")
    private String correlationId;

    public ConversionRequest() {}

    public ConversionRequest(UUID accountId, String fromCurrency, String toCurrency,
                            long fromAmountMinor, String correlationId) {
        this.accountId = accountId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmountMinor = fromAmountMinor;
        this.correlationId = correlationId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public long getFromAmountMinor() {
        return fromAmountMinor;
    }

    public void setFromAmountMinor(long fromAmountMinor) {
        this.fromAmountMinor = fromAmountMinor;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
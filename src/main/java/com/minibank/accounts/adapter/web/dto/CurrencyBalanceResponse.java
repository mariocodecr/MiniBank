package com.minibank.accounts.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CurrencyBalanceResponse {
    
    @NotBlank
    private String currency;
    
    @NotBlank
    private String currencyName;
    
    @NotBlank
    private String totalAmount;
    
    @NotBlank
    private String availableAmount;
    
    @NotBlank
    private String reservedAmount;

    public CurrencyBalanceResponse() {}

    public CurrencyBalanceResponse(String currency, String currencyName, String totalAmount, 
                                 String availableAmount, String reservedAmount) {
        this.currency = currency;
        this.currencyName = currencyName;
        this.totalAmount = totalAmount;
        this.availableAmount = availableAmount;
        this.reservedAmount = reservedAmount;
    }

    // Getters and Setters
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(String availableAmount) { this.availableAmount = availableAmount; }

    public String getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(String reservedAmount) { this.reservedAmount = reservedAmount; }
}










package com.minibank.accounts.api.dto;

import java.math.BigDecimal;

public class AccountBalanceDto {
    private String currencyCode;
    private BigDecimal availableAmount;
    private BigDecimal reservedAmount;
    private BigDecimal totalAmount;

    public AccountBalanceDto() {}

    public AccountBalanceDto(String currencyCode, BigDecimal availableAmount,
                           BigDecimal reservedAmount, BigDecimal totalAmount) {
        this.currencyCode = currencyCode;
        this.availableAmount = availableAmount;
        this.reservedAmount = reservedAmount;
        this.totalAmount = totalAmount;
    }

    // Getters and setters
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(BigDecimal availableAmount) { this.availableAmount = availableAmount; }

    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal reservedAmount) { this.reservedAmount = reservedAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
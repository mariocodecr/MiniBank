package com.minibank.accounts.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public class BalanceOperationRequest {
    
    @NotBlank
    private String amount;

    public BalanceOperationRequest() {}

    public BalanceOperationRequest(String amount) {
        this.amount = amount;
    }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
}

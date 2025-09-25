package com.minibank.accounts.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateMultiCurrencyAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    public CreateMultiCurrencyAccountRequest() {}
    
    public CreateMultiCurrencyAccountRequest(String accountHolderName, String email) {
        this.accountHolderName = accountHolderName;
        this.email = email;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}













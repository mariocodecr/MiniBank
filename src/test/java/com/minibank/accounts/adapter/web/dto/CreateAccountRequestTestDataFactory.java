package com.minibank.accounts.adapter.web.dto;

import com.minibank.accounts.domain.Currency;

import java.util.UUID;

public class CreateAccountRequestTestDataFactory {
    
    public static CreateAccountRequest createRequest() {
        return new CreateAccountRequest(UUID.randomUUID(), Currency.USD);
    }
    
    public static CreateAccountRequest createRequest(UUID userId, Currency currency) {
        return new CreateAccountRequest(userId, currency);
    }
    
    public static CreateAccountRequest createUSDRequest(UUID userId) {
        return new CreateAccountRequest(userId, Currency.USD);
    }
    
    public static CreateAccountRequest createCRCRequest(UUID userId) {
        return new CreateAccountRequest(userId, Currency.CRC);
    }
}
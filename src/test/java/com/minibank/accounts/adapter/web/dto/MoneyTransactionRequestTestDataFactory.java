package com.minibank.accounts.adapter.web.dto;

import com.minibank.accounts.domain.Currency;

import java.math.BigDecimal;

public class MoneyTransactionRequestTestDataFactory {
    
    public static MoneyTransactionRequest createRequest(BigDecimal amount, Currency currency) {
        return new MoneyTransactionRequest(amount, currency);
    }
    
    public static MoneyTransactionRequest createUSDRequest(BigDecimal amount) {
        return new MoneyTransactionRequest(amount, Currency.USD);
    }
    
    public static MoneyTransactionRequest createCRCRequest(BigDecimal amount) {
        return new MoneyTransactionRequest(amount, Currency.CRC);
    }
    
    public static MoneyTransactionRequest createUSDRequest(String amount) {
        return new MoneyTransactionRequest(new BigDecimal(amount), Currency.USD);
    }
    
    public static MoneyTransactionRequest createCRCRequest(String amount) {
        return new MoneyTransactionRequest(new BigDecimal(amount), Currency.CRC);
    }
}
package com.minibank.accounts.domain;

import java.math.BigDecimal;

public class MoneyTestDataFactory {
    
    public static Money dollars(long amount) {
        return Money.of(amount, Currency.USD);
    }
    
    public static Money dollars(BigDecimal amount) {
        return Money.of(amount, Currency.USD);
    }
    
    public static Money colones(long amount) {
        return Money.of(amount, Currency.CRC);
    }
    
    public static Money colones(BigDecimal amount) {
        return Money.of(amount, Currency.CRC);
    }
    
    public static Money zeroDollars() {
        return Money.zero(Currency.USD);
    }
    
    public static Money zeroColones() {
        return Money.zero(Currency.CRC);
    }
}
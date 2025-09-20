package com.minibank.accounts.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.minibank.accounts.domain.MoneyTestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyFromMinorUnits() {
        Money money = Money.of(10050, Currency.USD);
        
        assertEquals(10050, money.getMinorUnits());
        assertEquals(Currency.USD, money.getCurrency());
        assertEquals(new BigDecimal("100.50"), money.getAmount());
    }

    @Test
    void shouldCreateMoneyFromBigDecimal() {
        Money money = Money.of(new BigDecimal("100.50"), Currency.USD);
        
        assertEquals(10050, money.getMinorUnits());
        assertEquals(new BigDecimal("100.50"), money.getAmount());
    }

    @Test
    void shouldCreateZeroMoney() {
        Money money = Money.zero(Currency.USD);
        
        assertTrue(money.isZero());
        assertEquals(0, money.getMinorUnits());
    }

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, 
            () -> Money.of(-100, Currency.USD));
        
        assertThrows(IllegalArgumentException.class,
            () -> Money.of(new BigDecimal("-10.00"), Currency.USD));
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money money1 = dollars(10050); // $100.50
        Money money2 = dollars(2550);  // $25.50
        
        Money result = money1.add(money2);
        
        assertEquals(12600, result.getMinorUnits());
        assertEquals(new BigDecimal("126.00"), result.getAmount());
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money dollars = dollars(1000);
        Money colones = colones(1000);
        
        assertThrows(IllegalArgumentException.class, 
            () -> dollars.add(colones));
    }

    @Test
    void shouldSubtractMoneyWithSameCurrency() {
        Money money1 = dollars(10050); // $100.50
        Money money2 = dollars(2550);  // $25.50
        
        Money result = money1.subtract(money2);
        
        assertEquals(7500, result.getMinorUnits());
        assertEquals(new BigDecimal("75.00"), result.getAmount());
    }

    @Test
    void shouldThrowExceptionWhenSubtractingResultsInNegative() {
        Money money1 = dollars(2550);  // $25.50
        Money money2 = dollars(10050); // $100.50
        
        assertThrows(IllegalArgumentException.class,
            () -> money1.subtract(money2));
    }

    @Test
    void shouldCompareAmounts() {
        Money smaller = dollars(2550);  // $25.50
        Money larger = dollars(10050);  // $100.50
        Money equal = dollars(2550);    // $25.50
        
        assertTrue(larger.isGreaterThan(smaller));
        assertFalse(smaller.isGreaterThan(larger));
        assertFalse(smaller.isGreaterThan(equal));
        
        assertTrue(larger.isGreaterThanOrEqual(smaller));
        assertTrue(smaller.isGreaterThanOrEqual(equal));
        assertFalse(smaller.isGreaterThanOrEqual(larger));
    }

    @Test
    void shouldBeEqualWhenSameAmountAndCurrency() {
        Money money1 = dollars(10050);
        Money money2 = dollars(10050);
        Money different = colones(10050);
        
        assertEquals(money1, money2);
        assertNotEquals(money1, different);
        assertEquals(money1.hashCode(), money2.hashCode());
    }
}
package com.minibank.accounts.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
    private final long minorUnits;
    private final Currency currency;

    private Money(long minorUnits, Currency currency) {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        this.minorUnits = minorUnits;
        this.currency = currency;
    }

    public static Money of(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        return new Money(toMinorUnits(amount, currency), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public BigDecimal getAmount() {
        return BigDecimal.valueOf(minorUnits)
            .divide(scalingFactor(currency), currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.minorUnits + other.minorUnits, this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        long result = this.minorUnits - other.minorUnits;
        if (result < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative");
        }
        return new Money(result, this.currency);
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.minorUnits > other.minorUnits;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.minorUnits >= other.minorUnits;
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    public long getMinorUnits() {
        return minorUnits;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return minorUnits == money.minorUnits && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minorUnits, currency);
    }

    @Override
    public String toString() {
        return "Money{" +
                "minorUnits=" + minorUnits +
                ", currency=" + currency +
                '}';
    }

    private void validateSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                "Cannot perform operation on different currencies: " + 
                this.currency + " and " + other.currency);
        }
    }

    private static long toMinorUnits(BigDecimal amount, Currency currency) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }

        BigDecimal scaled = amount.multiply(scalingFactor(currency))
            .setScale(0, RoundingMode.HALF_UP);
        try {
            return scaled.longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Money amount is too large", ex);
        }
    }

    private static BigDecimal scalingFactor(Currency currency) {
        return BigDecimal.TEN.pow(currency.getDecimalPlaces());
    }
}

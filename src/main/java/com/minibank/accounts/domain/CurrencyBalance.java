package com.minibank.accounts.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class CurrencyBalance {
    private final Currency currency;
    private final long availableAmountMinor;
    private final long reservedAmountMinor;
    private final long totalAmountMinor;
    private final int version;

    public CurrencyBalance(Currency currency, long availableAmountMinor, long reservedAmountMinor, int version) {
        if (availableAmountMinor < 0) {
            throw new IllegalArgumentException("Available amount cannot be negative");
        }
        if (reservedAmountMinor < 0) {
            throw new IllegalArgumentException("Reserved amount cannot be negative");
        }
        
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.availableAmountMinor = availableAmountMinor;
        this.reservedAmountMinor = reservedAmountMinor;
        this.totalAmountMinor = availableAmountMinor + reservedAmountMinor;
        this.version = version;
    }

    public static CurrencyBalance zero(Currency currency) {
        return new CurrencyBalance(currency, 0, 0, 0);
    }

    public static CurrencyBalance create(Currency currency, long amountMinor) {
        return new CurrencyBalance(currency, amountMinor, 0, 0);
    }

    public CurrencyBalance credit(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return new CurrencyBalance(currency, availableAmountMinor + amountMinor, reservedAmountMinor, version);
    }

    public CurrencyBalance debit(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (availableAmountMinor < amountMinor) {
            throw new InsufficientFundsException(
                String.format("Insufficient available balance: %d, requested: %d", availableAmountMinor, amountMinor)
            );
        }
        return new CurrencyBalance(currency, availableAmountMinor - amountMinor, reservedAmountMinor, version);
    }

    public CurrencyBalance reserve(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Reserve amount must be positive");
        }
        if (availableAmountMinor < amountMinor) {
            throw new InsufficientFundsException(
                String.format("Insufficient available balance for reservation: %d, requested: %d", availableAmountMinor, amountMinor)
            );
        }
        return new CurrencyBalance(currency, availableAmountMinor - amountMinor, reservedAmountMinor + amountMinor, version);
    }

    public CurrencyBalance releaseReservation(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }
        if (reservedAmountMinor < amountMinor) {
            throw new IllegalArgumentException(
                String.format("Insufficient reserved balance: %d, requested: %d", reservedAmountMinor, amountMinor)
            );
        }
        return new CurrencyBalance(currency, availableAmountMinor + amountMinor, reservedAmountMinor - amountMinor, version);
    }

    public CurrencyBalance useReservation(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Use amount must be positive");
        }
        if (reservedAmountMinor < amountMinor) {
            throw new IllegalArgumentException(
                String.format("Insufficient reserved balance: %d, requested: %d", reservedAmountMinor, amountMinor)
            );
        }
        return new CurrencyBalance(currency, availableAmountMinor, reservedAmountMinor - amountMinor, version);
    }

    public boolean hasAvailableBalance(long amountMinor) {
        return availableAmountMinor >= amountMinor;
    }

    public boolean hasReservedBalance(long amountMinor) {
        return reservedAmountMinor >= amountMinor;
    }

    public BigDecimal getAvailableAmount() {
        return BigDecimal.valueOf(availableAmountMinor)
            .divide(BigDecimal.valueOf(Math.pow(10, currency.getDecimalPlaces())), currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    public BigDecimal getReservedAmount() {
        return BigDecimal.valueOf(reservedAmountMinor)
            .divide(BigDecimal.valueOf(Math.pow(10, currency.getDecimalPlaces())), currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmount() {
        return BigDecimal.valueOf(totalAmountMinor)
            .divide(BigDecimal.valueOf(Math.pow(10, currency.getDecimalPlaces())), currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    // Getters
    public Currency getCurrency() { return currency; }
    public long getAvailableAmountMinor() { return availableAmountMinor; }
    public long getReservedAmountMinor() { return reservedAmountMinor; }
    public long getTotalAmountMinor() { return totalAmountMinor; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyBalance that = (CurrencyBalance) o;
        return availableAmountMinor == that.availableAmountMinor &&
               reservedAmountMinor == that.reservedAmountMinor &&
               version == that.version &&
               Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, availableAmountMinor, reservedAmountMinor, version);
    }

    @Override
    public String toString() {
        return String.format("CurrencyBalance{currency=%s, available=%s, reserved=%s, total=%s, version=%d}", 
            currency.getCode(), getAvailableAmount(), getReservedAmount(), getTotalAmount(), version);
    }
}
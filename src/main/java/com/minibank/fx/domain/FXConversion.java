package com.minibank.fx.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FXConversion {
    private final UUID id;
    private final UUID accountId;
    private final String fromCurrency;
    private final String toCurrency;
    private final long fromAmountMinor;
    private final long toAmountMinor;
    private final BigDecimal exchangeRate;
    private final BigDecimal spread;
    private final String provider;
    private final Instant timestamp;
    private final String correlationId;

    private FXConversion(UUID id, UUID accountId, String fromCurrency, String toCurrency,
                        long fromAmountMinor, long toAmountMinor, BigDecimal exchangeRate,
                        BigDecimal spread, String provider, Instant timestamp, String correlationId) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.fromCurrency = Objects.requireNonNull(fromCurrency, "From currency cannot be null");
        this.toCurrency = Objects.requireNonNull(toCurrency, "To currency cannot be null");
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.exchangeRate = Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null");
        this.spread = Objects.requireNonNull(spread, "Spread cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.correlationId = Objects.requireNonNull(correlationId, "Correlation ID cannot be null");

        if (fromAmountMinor <= 0) {
            throw new IllegalArgumentException("From amount must be positive");
        }
        if (toAmountMinor <= 0) {
            throw new IllegalArgumentException("To amount must be positive");
        }
        if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        if (spread.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spread cannot be negative");
        }
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("From and to currencies cannot be the same");
        }
    }

    public static FXConversion create(UUID accountId, String fromCurrency, String toCurrency,
                                    long fromAmountMinor, ExchangeRate rate, String correlationId) {
        BigDecimal fromAmount = BigDecimal.valueOf(fromAmountMinor);
        BigDecimal toAmount = fromAmount.multiply(rate.getSellRate());
        long toAmountMinor = toAmount.longValue();

        return new FXConversion(UUID.randomUUID(), accountId, fromCurrency, toCurrency,
                               fromAmountMinor, toAmountMinor, rate.getRate(), rate.getSpread(),
                               rate.getProvider(), Instant.now(), correlationId);
    }

    public static FXConversion fromEntity(UUID id, UUID accountId, String fromCurrency, String toCurrency,
                                        long fromAmountMinor, long toAmountMinor, BigDecimal exchangeRate,
                                        BigDecimal spread, String provider, Instant timestamp, String correlationId) {
        return new FXConversion(id, accountId, fromCurrency, toCurrency, fromAmountMinor, toAmountMinor,
                               exchangeRate, spread, provider, timestamp, correlationId);
    }

    public BigDecimal getEffectiveRate() {
        return exchangeRate.subtract(spread);
    }

    public String getCurrencyPair() {
        return fromCurrency + "/" + toCurrency;
    }

    public BigDecimal getFromAmountMajor(int decimalPlaces) {
        return BigDecimal.valueOf(fromAmountMinor)
            .divide(BigDecimal.valueOf(Math.pow(10, decimalPlaces)), decimalPlaces, RoundingMode.HALF_UP);
    }

    public BigDecimal getToAmountMajor(int decimalPlaces) {
        return BigDecimal.valueOf(toAmountMinor)
            .divide(BigDecimal.valueOf(Math.pow(10, decimalPlaces)), decimalPlaces, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public long getFromAmountMinor() {
        return fromAmountMinor;
    }

    public long getToAmountMinor() {
        return toAmountMinor;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FXConversion that = (FXConversion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("FXConversion{%s->%s, %d->%d, rate=%s, account=%s}",
                           fromCurrency, toCurrency, fromAmountMinor, toAmountMinor, exchangeRate, accountId);
    }
}
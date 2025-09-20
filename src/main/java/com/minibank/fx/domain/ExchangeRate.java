package com.minibank.fx.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ExchangeRate {
    private final UUID id;
    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal rate;
    private final BigDecimal spread;
    private final String provider;
    private final Instant timestamp;
    private final Instant validUntil;

    private ExchangeRate(UUID id, String baseCurrency, String quoteCurrency, 
                        BigDecimal rate, BigDecimal spread, String provider,
                        Instant timestamp, Instant validUntil) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.quoteCurrency = Objects.requireNonNull(quoteCurrency, "Quote currency cannot be null");
        this.rate = Objects.requireNonNull(rate, "Rate cannot be null");
        this.spread = Objects.requireNonNull(spread, "Spread cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.validUntil = Objects.requireNonNull(validUntil, "Valid until cannot be null");
        
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (spread.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spread cannot be negative");
        }
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalArgumentException("Base and quote currencies cannot be the same");
        }
    }

    public static ExchangeRate create(String baseCurrency, String quoteCurrency, 
                                    BigDecimal rate, BigDecimal spread, String provider,
                                    Instant timestamp, Instant validUntil) {
        return new ExchangeRate(UUID.randomUUID(), baseCurrency, quoteCurrency, 
                               rate, spread, provider, timestamp, validUntil);
    }

    public static ExchangeRate fromEntity(UUID id, String baseCurrency, String quoteCurrency, 
                                        BigDecimal rate, BigDecimal spread, String provider,
                                        Instant timestamp, Instant validUntil) {
        return new ExchangeRate(id, baseCurrency, quoteCurrency, rate, spread, provider, 
                               timestamp, validUntil);
    }

    public BigDecimal getBuyRate() {
        return rate.add(spread).setScale(8, RoundingMode.HALF_UP);
    }

    public BigDecimal getSellRate() {
        return rate.subtract(spread).setScale(8, RoundingMode.HALF_UP);
    }

    public BigDecimal getMidRate() {
        return rate.setScale(8, RoundingMode.HALF_UP);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(validUntil);
    }

    public String getCurrencyPair() {
        return baseCurrency + "/" + quoteCurrency;
    }

    public ExchangeRate withSpread(BigDecimal newSpread) {
        if (newSpread.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spread cannot be negative");
        }
        return new ExchangeRate(id, baseCurrency, quoteCurrency, rate, newSpread, 
                               provider, timestamp, validUntil);
    }

    public ExchangeRate inverse() {
        BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP);
        return new ExchangeRate(UUID.randomUUID(), quoteCurrency, baseCurrency, 
                               inverseRate, spread, provider, timestamp, validUntil);
    }

    public UUID getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
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

    public Instant getValidUntil() {
        return validUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeRate that = (ExchangeRate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ExchangeRate{%s=%s, provider=%s, rate=%s±%s, valid=%s}", 
                           getCurrencyPair(), rate, provider, rate, spread, validUntil);
    }
}
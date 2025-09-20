package com.minibank.fx.adapter.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ConversionResponse {
    private final UUID id;
    private final UUID accountId;
    private final String fromCurrency;
    private final String toCurrency;
    private final long fromAmountMinor;
    private final long toAmountMinor;
    private final BigDecimal exchangeRate;
    private final BigDecimal effectiveRate;
    private final BigDecimal spread;
    private final String provider;
    private final Instant timestamp;
    private final String correlationId;

    public ConversionResponse(UUID id, UUID accountId, String fromCurrency, String toCurrency,
                             long fromAmountMinor, long toAmountMinor, BigDecimal exchangeRate,
                             BigDecimal effectiveRate, BigDecimal spread, String provider,
                             Instant timestamp, String correlationId) {
        this.id = id;
        this.accountId = accountId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.exchangeRate = exchangeRate;
        this.effectiveRate = effectiveRate;
        this.spread = spread;
        this.provider = provider;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
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

    public BigDecimal getEffectiveRate() {
        return effectiveRate;
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

    public String getCurrencyPair() {
        return fromCurrency + "/" + toCurrency;
    }
}
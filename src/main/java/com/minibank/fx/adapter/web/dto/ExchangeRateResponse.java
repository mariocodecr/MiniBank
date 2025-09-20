package com.minibank.fx.adapter.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExchangeRateResponse {
    private final UUID id;
    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal midRate;
    private final BigDecimal buyRate;
    private final BigDecimal sellRate;
    private final BigDecimal spread;
    private final String provider;
    private final Instant timestamp;
    private final Instant validUntil;
    private final boolean expired;

    public ExchangeRateResponse(UUID id, String baseCurrency, String quoteCurrency,
                               BigDecimal midRate, BigDecimal buyRate, BigDecimal sellRate,
                               BigDecimal spread, String provider, Instant timestamp,
                               Instant validUntil, boolean expired) {
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.midRate = midRate;
        this.buyRate = buyRate;
        this.sellRate = sellRate;
        this.spread = spread;
        this.provider = provider;
        this.timestamp = timestamp;
        this.validUntil = validUntil;
        this.expired = expired;
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

    public BigDecimal getMidRate() {
        return midRate;
    }

    public BigDecimal getBuyRate() {
        return buyRate;
    }

    public BigDecimal getSellRate() {
        return sellRate;
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

    public boolean isExpired() {
        return expired;
    }

    public String getCurrencyPair() {
        return baseCurrency + "/" + quoteCurrency;
    }
}